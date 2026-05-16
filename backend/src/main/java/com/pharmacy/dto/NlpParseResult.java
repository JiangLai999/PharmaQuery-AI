package com.pharmacy.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * NLP解析结果
 */
@Data
public class NlpParseResult {
    /** 识别出的实体列表 */
    private List<NlpEntity> entities;
    /** 查询意图: drug_search / symptom_search / category_search */
    private String intent;
    /** 结构化查询参数 */
    private Map<String, String> queryParams;

    @Data
    public static class NlpEntity {
        private String text;
        private String type;  // DRUG / SYMPTOM / DOSAGE_FORM / POPULATION
        private double confidence;
    }
}
