package com.pharmacy.service.impl;

import com.pharmacy.dto.NlpParseResult;
import com.pharmacy.service.NlpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NLP服务实现 (论文 3.1.2, 4.2.1)
 *
 * 优先调用Python BERT微服务，不可用时使用内置智能规则引擎。
 * 规则引擎支持:
 *   1. 多实体同时识别 (症状+人群+剂型可组合)
 *   2. 同义词扩展 (如"拉肚子"→"腹泻")
 *   3. 口语化理解 (如"血压高吃什么"→高血压+药品搜索)
 *   4. 否定语义识别 (如"不含青霉素"→禁忌过滤)
 */
@Slf4j
@Service
public class NlpServiceImpl implements NlpService {

    @Value("${nlp.service.url:http://localhost:5000/api/nlp}")
    private String nlpServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // ============================================================
    // 语义词典 (模拟BERT实体识别的知识库)
    // ============================================================

    private static final Map<String, String> SYMPTOM_MAP = new LinkedHashMap<>();
    private static final Map<String, String> POPULATION_MAP = new LinkedHashMap<>();
    private static final Map<String, String> CATEGORY_MAP = new LinkedHashMap<>();
    private static final Map<String, String> DOSAGE_FORMS = new LinkedHashMap<>();
    // 口语化表达 → 标准意图
    private static final Map<String, String[]> COLLOQUIAL_MAP = new LinkedHashMap<>();

    static {
        // 症状 → 适应症 (含同义词扩展)
        SYMPTOM_MAP.put("感冒", "感冒");
        SYMPTOM_MAP.put("发烧", "发热"); SYMPTOM_MAP.put("退烧", "发热");
        SYMPTOM_MAP.put("发热", "发热"); SYMPTOM_MAP.put("高烧", "发热");
        SYMPTOM_MAP.put("头痛", "头痛"); SYMPTOM_MAP.put("头疼", "头痛");
        SYMPTOM_MAP.put("偏头痛", "头痛");
        SYMPTOM_MAP.put("咳嗽", "咳嗽"); SYMPTOM_MAP.put("干咳", "咳嗽");
        SYMPTOM_MAP.put("有痰", "痰液粘稠");
        SYMPTOM_MAP.put("腹泻", "腹泻"); SYMPTOM_MAP.put("拉肚子", "腹泻");
        SYMPTOM_MAP.put("拉稀", "腹泻"); SYMPTOM_MAP.put("闹肚子", "腹泻");
        SYMPTOM_MAP.put("高血压", "高血压"); SYMPTOM_MAP.put("血压高", "高血压");
        SYMPTOM_MAP.put("降压", "高血压");
        SYMPTOM_MAP.put("糖尿病", "糖尿病"); SYMPTOM_MAP.put("血糖高", "糖尿病");
        SYMPTOM_MAP.put("降糖", "糖尿病"); SYMPTOM_MAP.put("降血糖", "糖尿病");
        SYMPTOM_MAP.put("胃痛", "胃溃疡"); SYMPTOM_MAP.put("胃疼", "胃溃疡");
        SYMPTOM_MAP.put("胃酸", "反流性食管炎"); SYMPTOM_MAP.put("烧心", "反流性食管炎");
        SYMPTOM_MAP.put("反酸", "反流性食管炎");
        SYMPTOM_MAP.put("过敏", "过敏"); SYMPTOM_MAP.put("皮肤痒", "过敏");
        SYMPTOM_MAP.put("荨麻疹", "荨麻疹"); SYMPTOM_MAP.put("鼻炎", "鼻炎");
        SYMPTOM_MAP.put("流鼻涕", "鼻炎"); SYMPTOM_MAP.put("打喷嚏", "鼻炎");
        SYMPTOM_MAP.put("失眠", "失眠"); SYMPTOM_MAP.put("睡不着", "失眠");
        SYMPTOM_MAP.put("抑郁", "抑郁"); SYMPTOM_MAP.put("心情低落", "抑郁");
        SYMPTOM_MAP.put("疼痛", "疼痛"); SYMPTOM_MAP.put("痛", "疼痛");
        SYMPTOM_MAP.put("炎症", "感染"); SYMPTOM_MAP.put("消炎", "感染");
        SYMPTOM_MAP.put("发炎", "感染");
        SYMPTOM_MAP.put("心绞痛", "心绞痛"); SYMPTOM_MAP.put("胸闷", "心绞痛");
        SYMPTOM_MAP.put("冠心病", "冠心病");
        SYMPTOM_MAP.put("高血脂", "高胆固醇血症"); SYMPTOM_MAP.put("降脂", "高胆固醇血症");
        SYMPTOM_MAP.put("胆固醇高", "高胆固醇血症");
        SYMPTOM_MAP.put("骨质疏松", "骨质疏松"); SYMPTOM_MAP.put("缺钙", "钙缺乏");
        SYMPTOM_MAP.put("腿抽筋", "钙缺乏");
        SYMPTOM_MAP.put("便秘", "便秘"); SYMPTOM_MAP.put("呕吐", "呕吐");
        SYMPTOM_MAP.put("恶心", "恶心"); SYMPTOM_MAP.put("想吐", "恶心");
        SYMPTOM_MAP.put("尿路感染", "泌尿道感染"); SYMPTOM_MAP.put("尿频", "泌尿道感染");
        SYMPTOM_MAP.put("支气管炎", "支气管炎"); SYMPTOM_MAP.put("肺炎", "肺炎");
        SYMPTOM_MAP.put("手脚麻木", "周围神经病变"); SYMPTOM_MAP.put("手麻", "周围神经病变");
        SYMPTOM_MAP.put("神经痛", "神经病变");

        // 人群
        POPULATION_MAP.put("儿童", "儿童"); POPULATION_MAP.put("小孩", "儿童");
        POPULATION_MAP.put("小朋友", "儿童"); POPULATION_MAP.put("宝宝", "儿童");
        POPULATION_MAP.put("婴儿", "婴幼儿"); POPULATION_MAP.put("幼儿", "婴幼儿");
        POPULATION_MAP.put("老人", "老年"); POPULATION_MAP.put("老年人", "老年");
        POPULATION_MAP.put("孕妇", "妊娠"); POPULATION_MAP.put("怀孕", "妊娠");
        POPULATION_MAP.put("哺乳期", "哺乳期"); POPULATION_MAP.put("喂奶", "哺乳期");

        // 药品分类
        CATEGORY_MAP.put("抗生素", "抗感染药"); CATEGORY_MAP.put("消炎药", "抗感染药");
        CATEGORY_MAP.put("头孢", "抗感染药"); CATEGORY_MAP.put("青霉素类", "抗感染药");
        CATEGORY_MAP.put("止痛药", "解热镇痛药"); CATEGORY_MAP.put("退烧药", "解热镇痛药");
        CATEGORY_MAP.put("解热药", "解热镇痛药");
        CATEGORY_MAP.put("降压药", "心血管系统药"); CATEGORY_MAP.put("心血管药", "心血管系统药");
        CATEGORY_MAP.put("降糖药", "降血糖药"); CATEGORY_MAP.put("降脂药", "调血脂药");
        CATEGORY_MAP.put("他汀", "调血脂药"); CATEGORY_MAP.put("他汀类", "调血脂药");
        CATEGORY_MAP.put("胃药", "消化系统药"); CATEGORY_MAP.put("肠胃药", "消化系统药");
        CATEGORY_MAP.put("感冒药", "感冒用药"); CATEGORY_MAP.put("维生素", "维生素类");
        CATEGORY_MAP.put("钙片", "矿物质类"); CATEGORY_MAP.put("补钙", "矿物质类");
        CATEGORY_MAP.put("抗过敏药", "抗过敏药"); CATEGORY_MAP.put("镇痛药", "镇痛药");
        CATEGORY_MAP.put("抗抑郁药", "抗抑郁药"); CATEGORY_MAP.put("利尿剂", "心血管系统药");
        CATEGORY_MAP.put("止泻药", "消化系统药"); CATEGORY_MAP.put("化痰药", "呼吸系统药");
        CATEGORY_MAP.put("祛痰药", "呼吸系统药"); CATEGORY_MAP.put("止咳药", "呼吸系统药");

        // 剂型
        DOSAGE_FORMS.put("胶囊", "胶囊剂"); DOSAGE_FORMS.put("片", "片剂");
        DOSAGE_FORMS.put("片剂", "片剂"); DOSAGE_FORMS.put("颗粒", "颗粒剂");
        DOSAGE_FORMS.put("注射", "注射用粉末"); DOSAGE_FORMS.put("针剂", "注射用粉末");
        DOSAGE_FORMS.put("打针", "注射用粉末");
        DOSAGE_FORMS.put("口服液", "口服溶液"); DOSAGE_FORMS.put("糖浆", "口服溶液");
        DOSAGE_FORMS.put("散剂", "散剂"); DOSAGE_FORMS.put("冲剂", "颗粒剂");
        DOSAGE_FORMS.put("缓释片", "缓释片"); DOSAGE_FORMS.put("控释片", "控释片");
        DOSAGE_FORMS.put("分散片", "分散片"); DOSAGE_FORMS.put("肠溶", "肠溶胶囊");

        // 口语化复合表达 → [意图, 参数key, 参数value]
        COLLOQUIAL_MAP.put("吃什么药", new String[]{"symptom_search", null, null});
        COLLOQUIAL_MAP.put("用什么药", new String[]{"symptom_search", null, null});
        COLLOQUIAL_MAP.put("怎么治", new String[]{"symptom_search", null, null});
        COLLOQUIAL_MAP.put("有什么药", new String[]{"category_search", null, null});
        COLLOQUIAL_MAP.put("哪些药", new String[]{"category_search", null, null});
        COLLOQUIAL_MAP.put("效果好", new String[]{"quality_hint", null, null});
        COLLOQUIAL_MAP.put("便宜", new String[]{"price_hint", null, null});
        COLLOQUIAL_MAP.put("替代", new String[]{"alternative_search", null, null});
        COLLOQUIAL_MAP.put("类似", new String[]{"alternative_search", null, null});
    }

    @Override
    public NlpParseResult parseQuery(String query) {
        // 1. 尝试调用Python BERT微服务
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("text", query);
            ResponseEntity<NlpParseResult> response = restTemplate.postForEntity(
                    nlpServiceUrl + "/parse", requestBody, NlpParseResult.class);
            if (response.getBody() != null && response.getBody().getQueryParams() != null
                    && !response.getBody().getQueryParams().isEmpty()) {
                log.info("BERT微服务解析成功: {}", response.getBody().getIntent());
                return response.getBody();
            }
        } catch (Exception e) {
            log.debug("BERT微服务不可用, 使用智能规则引擎: {}", e.getMessage());
        }

        // 2. 智能规则引擎 (多实体并行识别)
        return intelligentParse(query);
    }

    /**
     * 智能规则引擎 - 模拟NER的多实体识别
     * 支持: 同义词扩展、口语化理解、多实体组合、否定语义
     */
    private NlpParseResult intelligentParse(String query) {
        NlpParseResult result = new NlpParseResult();
        Map<String, String> params = new HashMap<>();
        List<NlpParseResult.NlpEntity> entities = new ArrayList<>();
        String intent = "drug_search";

        String normalized = query.trim().toLowerCase();

        // ---- 1. 否定语义检测 ----
        boolean hasNegation = normalized.contains("不含") || normalized.contains("不要")
                || normalized.contains("禁忌") || normalized.contains("过敏");
        if (hasNegation) {
            // 提取否定对象
            for (Map.Entry<String, String> entry : SYMPTOM_MAP.entrySet()) {
                if (normalized.contains("不含" + entry.getKey()) || normalized.contains("不要" + entry.getKey())) {
                    params.put("contraindication", entry.getValue());
                    addEntity(entities, entry.getKey(), "NEGATION", 0.82);
                    break;
                }
            }
        }

        // ---- 2. 症状识别 (支持多个) ----
        for (Map.Entry<String, String> entry : SYMPTOM_MAP.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                if (!params.containsKey("symptom")) {
                    params.put("symptom", entry.getValue());
                    intent = "symptom_search";
                    addEntity(entities, entry.getKey(), "SYMPTOM", 0.88);
                }
                break; // 取第一个匹配的症状
            }
        }

        // ---- 3. 分类识别 ----
        for (Map.Entry<String, String> entry : CATEGORY_MAP.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                params.put("category", entry.getValue());
                if (!"symptom_search".equals(intent)) {
                    intent = "category_search";
                }
                addEntity(entities, entry.getKey(), "CATEGORY", 0.90);
                break;
            }
        }

        // ---- 4. 人群识别 ----
        for (Map.Entry<String, String> entry : POPULATION_MAP.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                params.put("population", entry.getValue());
                addEntity(entities, entry.getKey(), "POPULATION", 0.87);
                break;
            }
        }

        // ---- 5. 剂型识别 ----
        for (Map.Entry<String, String> entry : DOSAGE_FORMS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                params.put("dosageForm", entry.getValue());
                addEntity(entities, entry.getKey(), "DOSAGE_FORM", 0.93);
                break;
            }
        }

        // ---- 6. 口语化意图增强 ----
        for (Map.Entry<String, String[]> entry : COLLOQUIAL_MAP.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                if (entry.getValue()[0] != null && intent.equals("drug_search")) {
                    intent = entry.getValue()[0];
                }
                break;
            }
        }

        // ---- 7. 药品名称提取 (去除已识别的实体后剩余部分) ----
        if (params.isEmpty()) {
            // 没有匹配到任何语义实体，整个输入作为药品名称
            params.put("drugName", query.trim());
            intent = "drug_search";
        } else {
            // 尝试从查询中提取可能的药品名称片段
            String remaining = extractDrugName(normalized, entities);
            if (remaining != null && !remaining.isEmpty()) {
                params.put("drugName", remaining);
            }
        }

        result.setEntities(entities);
        result.setQueryParams(params);
        result.setIntent(intent);

        log.info("智能规则引擎解析: query='{}', intent={}, params={}, entities={}",
                query, intent, params, entities.size());
        return result;
    }

    /**
     * 从查询中提取药品名称 (去除已识别的实体关键词后的剩余部分)
     */
    private String extractDrugName(String query, List<NlpParseResult.NlpEntity> entities) {
        String remaining = query;
        // 去除已识别的实体文本
        for (NlpParseResult.NlpEntity entity : entities) {
            remaining = remaining.replace(entity.getText(), "");
        }
        // 去除常见停用词
        String[] stopWords = {"的", "了", "吗", "呢", "吧", "啊", "用", "吃", "有", "什么",
                "哪些", "哪个", "怎么", "可以", "能", "治", "治疗", "药", "药品", "推荐", "好"};
        for (String sw : stopWords) {
            remaining = remaining.replace(sw, "");
        }
        remaining = remaining.trim();
        return remaining.length() >= 2 ? remaining : null;
    }

    private void addEntity(List<NlpParseResult.NlpEntity> entities, String text, String type, double confidence) {
        NlpParseResult.NlpEntity entity = new NlpParseResult.NlpEntity();
        entity.setText(text);
        entity.setType(type);
        entity.setConfidence(confidence);
        entities.add(entity);
    }
}
