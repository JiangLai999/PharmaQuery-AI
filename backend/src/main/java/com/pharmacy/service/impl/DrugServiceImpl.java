package com.pharmacy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.dto.DrugDTO;
import com.pharmacy.dto.NlpParseResult;
import com.pharmacy.dto.SearchRequest;
import com.pharmacy.entity.DrugInfo;
import com.pharmacy.entity.QueryLog;
import com.pharmacy.mapper.DrugInfoMapper;
import com.pharmacy.mapper.UserDrugInteractionMapper;
import com.pharmacy.repository.QueryLogRepository;
import com.pharmacy.service.DrugService;
import com.pharmacy.service.NlpService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 药品查询服务实现
 * Redis/MongoDB 为可选依赖，不可用时自动降级
 */
@Slf4j
@Service
public class DrugServiceImpl implements DrugService {

    private final DrugInfoMapper drugInfoMapper;
    private final UserDrugInteractionMapper interactionMapper;
    private final NlpService nlpService;

    // 可选依赖: Redis (注入失败时为null)
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    // 可选依赖: MongoDB (注入失败时为null)
    @Autowired(required = false)
    private QueryLogRepository queryLogRepository;

    private static final String CACHE_PREFIX = "drug:search:";
    private static final long CACHE_TTL = 300;

    public DrugServiceImpl(DrugInfoMapper drugInfoMapper,
                           UserDrugInteractionMapper interactionMapper,
                           NlpService nlpService) {
        this.drugInfoMapper = drugInfoMapper;
        this.interactionMapper = interactionMapper;
        this.nlpService = nlpService;
    }

    // ============================================================
    // 多条件组合查询 (论文 3.1.1)
    // ============================================================
    @Override
    public Page<DrugDTO> search(SearchRequest request) {
        Page<DrugInfo> page = new Page<>(request.getPageNum(), request.getPageSize());

        QueryWrapper<DrugInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        if (StringUtils.isNotBlank(request.getGenericName())) {
            wrapper.like("generic_name", request.getGenericName());
        }
        if (StringUtils.isNotBlank(request.getDosageForm())) {
            wrapper.eq("dosage_form", request.getDosageForm());
        }
        if (StringUtils.isNotBlank(request.getApprovalNumber())) {
            wrapper.eq("approval_number", request.getApprovalNumber());
        }
        if (StringUtils.isNotBlank(request.getManufacturer())) {
            wrapper.like("manufacturer", request.getManufacturer());
        }
        if (StringUtils.isNotBlank(request.getCategory())) {
            wrapper.eq("category", request.getCategory());
        }
        if (StringUtils.isNotBlank(request.getInsuranceType())) {
            wrapper.eq("insurance_type", request.getInsuranceType());
        }
        if (StringUtils.isNotBlank(request.getStockStatus())) {
            switch (request.getStockStatus()) {
                case "inStock":
                    wrapper.gt("stock_quantity", 0);
                    break;
                case "lowStock":
                    wrapper.apply("stock_quantity <= stock_threshold AND stock_quantity > 0");
                    break;
                case "outOfStock":
                    wrapper.eq("stock_quantity", 0);
                    break;
            }
        }

        wrapper.orderByDesc("updated_at");
        Page<DrugInfo> result = drugInfoMapper.selectPage(page, wrapper);

        Page<DrugDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        dtoPage.setRecords(result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
        return dtoPage;
    }

    // ============================================================
    // NLP语义查询 (论文 3.1.2)
    // ============================================================
    @Override
    public List<DrugDTO> nlpSearch(String query) {
        log.info("NLP语义查询: {}", query);

        NlpParseResult parseResult = nlpService.parseQuery(query);

        QueryWrapper<DrugInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);

        Map<String, String> params = parseResult.getQueryParams();
        boolean hasCondition = false;

        if (params != null) {
            if (params.containsKey("drugName")) {
                String name = params.get("drugName");
                wrapper.and(w -> w.like("generic_name", name)
                       .or().like("trade_name", name));
                hasCondition = true;
            }
            if (params.containsKey("symptom")) {
                wrapper.like("indication", params.get("symptom"));
                hasCondition = true;
            }
            if (params.containsKey("dosageForm")) {
                wrapper.eq("dosage_form", params.get("dosageForm"));
                hasCondition = true;
            }
            if (params.containsKey("category")) {
                wrapper.like("category", params.get("category"));
                hasCondition = true;
            }
            // 否定语义: 排除含某禁忌的药品
            if (params.containsKey("contraindication")) {
                wrapper.notLike("contraindication", params.get("contraindication"));
                hasCondition = true;
            }
            // 人群过滤: 排除禁忌该人群的药品
            if (params.containsKey("population")) {
                String pop = params.get("population");
                wrapper.notLike("contraindication", pop);
                hasCondition = true;
            }
        }

        if (!hasCondition) {
            return fuzzySearch(query);
        }

        wrapper.last("LIMIT 50");
        List<DrugInfo> results = drugInfoMapper.selectList(wrapper);

        if (results.isEmpty()) {
            log.info("NLP解析无匹配结果，回退到模糊搜索: {}", query);
            return fuzzySearch(query);
        }

        return results.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    // ============================================================
    // 模糊搜索
    // ============================================================
    @Override
    public List<DrugDTO> fuzzySearch(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return Collections.emptyList();
        }

        // Redis缓存 (可选, 不可用时跳过)
        String cacheKey = CACHE_PREFIX + keyword.hashCode();
        if (redisTemplate != null) {
            try {
                @SuppressWarnings("unchecked")
                List<DrugDTO> cached = (List<DrugDTO>) redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("缓存命中: {}", keyword);
                    return cached;
                }
            } catch (Exception e) {
                log.warn("Redis读取失败, 跳过缓存: {}", e.getMessage());
            }
        }

        QueryWrapper<DrugInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1)
               .and(w -> w.like("generic_name", keyword)
                          .or().like("trade_name", keyword)
                          .or().like("indication", keyword)
                          .or().like("approval_number", keyword)
                          .or().like("manufacturer", keyword));
        wrapper.last("LIMIT 50");

        List<DrugDTO> results = drugInfoMapper.selectList(wrapper).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 写入缓存 (可选)
        if (redisTemplate != null && !results.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, results, CACHE_TTL, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Redis写入失败, 跳过缓存: {}", e.getMessage());
            }
        }

        return results;
    }

    @Override
    public DrugDTO findById(Long drugId) {
        DrugInfo drug = drugInfoMapper.selectById(drugId);
        if (drug == null) return null;
        return convertToDTO(drug);
    }

    @Override
    public List<DrugDTO> getNearExpiryDrugs() {
        return drugInfoMapper.findNearExpiryDrugs().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<DrugDTO> getLowStockDrugs() {
        return drugInfoMapper.findLowStockDrugs().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> countByCategory() {
        return drugInfoMapper.countByCategory();
    }

    // ============================================================
    // 记录用户-药品交互 (MongoDB可选)
    // ============================================================
    @Override
    @Async
    public void recordInteraction(Long userId, Long drugId) {
        try {
            interactionMapper.upsertInteraction(userId, drugId);
        } catch (Exception e) {
            log.warn("记录MySQL交互失败: {}", e.getMessage());
        }

        // MongoDB日志 (可选)
        if (queryLogRepository != null) {
            try {
                QueryLog qlog = new QueryLog();
                qlog.setUserId(String.valueOf(userId));
                qlog.setSelectedDrugId(drugId);
                qlog.setTimestamp(LocalDateTime.now());
                queryLogRepository.save(qlog);
            } catch (Exception e) {
                log.warn("MongoDB日志写入失败, 跳过: {}", e.getMessage());
            }
        }
    }

    // ============================================================
    // 实体转DTO + 风险等级计算
    // ============================================================
    private DrugDTO convertToDTO(DrugInfo drug) {
        DrugDTO dto = new DrugDTO();
        BeanUtils.copyProperties(drug, dto);
        dto.setRiskLevel(calculateRiskLevel(drug));
        return dto;
    }

    private String calculateRiskLevel(DrugInfo drug) {
        double alpha = 0.6, beta = 0.4;
        double expiryRisk = 0, stockRisk = 0;

        if (drug.getExpiryDate() != null && drug.getShelfLifeDays() != null && drug.getShelfLifeDays() > 0) {
            long daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), drug.getExpiryDate());
            expiryRisk = 1.0 - (double) Math.max(daysRemaining, 0) / drug.getShelfLifeDays();
        }

        if (drug.getStockThreshold() != null && drug.getStockThreshold() > 0) {
            stockRisk = 1.0 - (double) Math.min(drug.getStockQuantity(), drug.getStockThreshold()) / drug.getStockThreshold();
        }

        double riskScore = alpha * expiryRisk + beta * stockRisk;

        if (riskScore >= 0.7) return "danger";
        if (riskScore >= 0.4) return "warning";
        return "normal";
    }
}
