package com.pharmacy;

import com.pharmacy.dto.*;
import com.pharmacy.entity.*;
import com.pharmacy.service.impl.NlpServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 药库查询系统 - 单元测试
 * 不依赖外部数据库，验证核心业务逻辑
 */
class PharmacyUnitTest {

    // ============================================================
    // 1. 实体类与DTO测试
    // ============================================================

    @Test
    @DisplayName("DrugInfo实体字段完整性")
    void testDrugInfoEntity() {
        DrugInfo drug = new DrugInfo();
        drug.setDrugId(1L);
        drug.setGenericName("阿莫西林胶囊");
        drug.setTradeName("阿莫仙");
        drug.setSpecification("0.5g*24粒");
        drug.setDosageForm("胶囊剂");
        drug.setManufacturer("珠海联邦制药");
        drug.setApprovalNumber("国药准字H10983045");
        drug.setCategory("抗感染药");
        drug.setInsuranceType("甲");
        drug.setIndication("敏感菌所致的感染");
        drug.setContraindication("青霉素过敏者禁用");
        drug.setStockQuantity(500);
        drug.setStockThreshold(50);
        drug.setUnitPrice(new BigDecimal("12.50"));
        drug.setExpiryDate(LocalDate.of(2026, 6, 15));
        drug.setShelfLifeDays(730);
        drug.setStorageCondition("密封，阴凉处保存");
        drug.setStatus(1);

        assertEquals(1L, drug.getDrugId());
        assertEquals("阿莫西林胶囊", drug.getGenericName());
        assertEquals("阿莫仙", drug.getTradeName());
        assertEquals("0.5g*24粒", drug.getSpecification());
        assertEquals("胶囊剂", drug.getDosageForm());
        assertEquals("国药准字H10983045", drug.getApprovalNumber());
        assertEquals("抗感染药", drug.getCategory());
        assertEquals("甲", drug.getInsuranceType());
        assertEquals(500, drug.getStockQuantity());
        assertEquals(50, drug.getStockThreshold());
        assertEquals(new BigDecimal("12.50"), drug.getUnitPrice());
        assertEquals(730, drug.getShelfLifeDays());
        assertEquals(1, drug.getStatus());
    }

    @Test
    @DisplayName("UserInfo实体字段完整性")
    void testUserInfoEntity() {
        UserInfo user = new UserInfo();
        user.setUserId(1L);
        user.setUsername("pharmacist");
        user.setPassword("encrypted");
        user.setRealName("张药师");
        user.setDepartment("药剂科");
        user.setRoleId(3L);
        user.setStatus(1);

        assertEquals("pharmacist", user.getUsername());
        assertEquals("张药师", user.getRealName());
        assertEquals("药剂科", user.getDepartment());
        assertEquals(3L, user.getRoleId());
    }

    @Test
    @DisplayName("ApiResult统一响应封装")
    void testApiResult() {
        ApiResult<String> success = ApiResult.success("test");
        assertEquals(200, success.getCode());
        assertEquals("success", success.getMessage());
        assertEquals("test", success.getData());

        ApiResult<String> error = ApiResult.error(404, "not found");
        assertEquals(404, error.getCode());
        assertEquals("not found", error.getMessage());
        assertNull(error.getData());

        ApiResult<String> error2 = ApiResult.error("server error");
        assertEquals(500, error2.getCode());
    }

    @Test
    @DisplayName("DrugDTO风险等级字段")
    void testDrugDTO() {
        DrugDTO dto = new DrugDTO();
        dto.setDrugId(1L);
        dto.setGenericName("测试药品");
        dto.setRiskLevel("danger");
        dto.setRecommendation("85%的相似用户也查询了此药品");

        assertEquals("danger", dto.getRiskLevel());
        assertNotNull(dto.getRecommendation());
        assertTrue(dto.getRecommendation().contains("85%"));
    }

    @Test
    @DisplayName("SearchRequest默认分页参数")
    void testSearchRequest() {
        SearchRequest req = new SearchRequest();
        assertEquals(1, req.getPageNum());
        assertEquals(20, req.getPageSize());
        assertNull(req.getQuery());
        assertNull(req.getDosageForm());
    }

    @Test
    @DisplayName("NlpParseResult结构")
    void testNlpParseResult() {
        NlpParseResult result = new NlpParseResult();
        result.setIntent("symptom_search");

        NlpParseResult.NlpEntity entity = new NlpParseResult.NlpEntity();
        entity.setText("感冒");
        entity.setType("SYMPTOM");
        entity.setConfidence(0.85);

        assertEquals("感冒", entity.getText());
        assertEquals("SYMPTOM", entity.getType());
        assertEquals(0.85, entity.getConfidence(), 0.001);
    }

    // ============================================================
    // 2. NLP规则引擎测试 (论文 3.1.2)
    // ============================================================

    private NlpServiceImpl createNlpService() {
        NlpServiceImpl svc = new NlpServiceImpl();
        try {
            java.lang.reflect.Field f = NlpServiceImpl.class.getDeclaredField("nlpServiceUrl");
            f.setAccessible(true);
            f.set(svc, "http://localhost:5000/api/nlp");
        } catch (Exception ignored) {}
        return svc;
    }

    @Test
    @DisplayName("NLP解析 - 症状查询: 治感冒的药")
    void testNlpParseSymptom() {
        NlpServiceImpl nlpService = createNlpService();
        NlpParseResult result = nlpService.parseQuery("治感冒的药");

        assertNotNull(result);
        assertEquals("symptom_search", result.getIntent());
        assertNotNull(result.getQueryParams());
        assertEquals("感冒", result.getQueryParams().get("symptom"));
    }

    @Test
    @DisplayName("NLP解析 - 分类查询: 降压药")
    void testNlpParseCategory() {
        NlpServiceImpl nlpService = createNlpService();
        NlpParseResult result = nlpService.parseQuery("降压药");

        assertNotNull(result);
        assertNotNull(result.getQueryParams());
        // 降压药应该映射到心血管系统药
        assertTrue(result.getQueryParams().containsKey("category") ||
                   result.getQueryParams().containsKey("symptom"));
    }

    @Test
    @DisplayName("NLP解析 - 药品名称直接查询: 阿莫西林")
    void testNlpParseDrugName() {
        NlpServiceImpl nlpService = createNlpService();
        NlpParseResult result = nlpService.parseQuery("阿莫西林");

        assertNotNull(result);
        assertNotNull(result.getQueryParams());
        // 应该作为药品名称查询
        assertTrue(result.getQueryParams().containsKey("drugName"));
        assertTrue(result.getQueryParams().get("drugName").contains("阿莫西林"));
    }

    @Test
    @DisplayName("NLP解析 - 复合查询: 儿童用退烧药")
    void testNlpParseComplex() {
        NlpServiceImpl nlpService = createNlpService();
        NlpParseResult result = nlpService.parseQuery("儿童用退烧药");

        assertNotNull(result);
        assertNotNull(result.getQueryParams());
        // 应该识别出症状(发热)或分类(解热镇痛药)
        assertTrue(result.getQueryParams().containsKey("symptom") ||
                   result.getQueryParams().containsKey("category"));
    }

    @Test
    @DisplayName("NLP解析 - 剂型查询: 胶囊")
    void testNlpParseDosageForm() {
        NlpServiceImpl nlpService = createNlpService();
        NlpParseResult result = nlpService.parseQuery("感冒胶囊");

        assertNotNull(result);
        assertNotNull(result.getQueryParams());
        assertTrue(result.getQueryParams().containsKey("dosageForm") ||
                   result.getQueryParams().containsKey("symptom"));
    }

    @Test
    @DisplayName("NLP解析 - 空查询处理")
    void testNlpParseEmpty() {
        NlpServiceImpl nlpService = createNlpService();
        NlpParseResult result = nlpService.parseQuery("xyz未知输入");
        assertEquals("drug_search", result.getIntent());
        assertTrue(result.getQueryParams().containsKey("drugName"));
    }

    // ============================================================
    // 3. 风险等级计算测试 (论文 3.2.2)
    // ============================================================

    @Test
    @DisplayName("风险等级计算 - 正常药品")
    void testRiskLevelNormal() {
        // 有效期还有1年, 库存充足
        String risk = calculateRiskLevel(
                LocalDate.now().plusDays(365), 730, 500, 50);
        assertEquals("normal", risk);
    }

    @Test
    @DisplayName("风险等级计算 - 近效期警告")
    void testRiskLevelWarning() {
        // 有效期还有60天, 库存正常
        String risk = calculateRiskLevel(
                LocalDate.now().plusDays(60), 730, 200, 50);
        assertEquals("warning", risk);
    }

    @Test
    @DisplayName("风险等级计算 - 高风险(近效期+低库存)")
    void testRiskLevelDanger() {
        // 有效期还有10天, 库存低于阈值
        String risk = calculateRiskLevel(
                LocalDate.now().plusDays(10), 730, 5, 50);
        assertEquals("danger", risk);
    }

    @Test
    @DisplayName("风险等级计算 - 已过期(库存正常时为warning)")
    void testRiskLevelExpired() {
        // 已过期, 但库存正常 -> expiryRisk=1.0, stockRisk=0 -> R=0.6 -> warning
        String risk = calculateRiskLevel(
                LocalDate.now().minusDays(10), 730, 100, 50);
        assertEquals("warning", risk);

        // 已过期 + 低库存 -> danger
        String risk2 = calculateRiskLevel(
                LocalDate.now().minusDays(10), 730, 10, 50);
        assertEquals("danger", risk2);
    }

    /**
     * 复制自 DrugServiceImpl 的风险计算逻辑
     * R = α * (1 - daysRemaining/shelfLifeDays) + β * (1 - stock/threshold)
     */
    private String calculateRiskLevel(LocalDate expiryDate, int shelfLifeDays,
                                       int stockQuantity, int stockThreshold) {
        double alpha = 0.6, beta = 0.4;
        double expiryRisk = 0, stockRisk = 0;

        if (expiryDate != null && shelfLifeDays > 0) {
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
            expiryRisk = 1.0 - (double) Math.max(daysRemaining, 0) / shelfLifeDays;
        }

        if (stockThreshold > 0) {
            stockRisk = 1.0 - (double) Math.min(stockQuantity, stockThreshold) / stockThreshold;
        }

        double riskScore = alpha * expiryRisk + beta * stockRisk;

        if (riskScore >= 0.7) return "danger";
        if (riskScore >= 0.4) return "warning";
        return "normal";
    }

    // ============================================================
    // 4. 协同过滤相似度计算测试 (论文 3.3)
    // ============================================================

    @Test
    @DisplayName("余弦相似度 - 完全相同向量")
    void testCosineSimilarityIdentical() {
        double[] vec1 = {3, 5, 2, 1};
        double[] vec2 = {3, 5, 2, 1};
        double sim = cosineSimilarity(vec1, vec2);
        assertEquals(1.0, sim, 0.001);
    }

    @Test
    @DisplayName("余弦相似度 - 正交向量")
    void testCosineSimilarityOrthogonal() {
        double[] vec1 = {1, 0, 0};
        double[] vec2 = {0, 1, 0};
        double sim = cosineSimilarity(vec1, vec2);
        assertEquals(0.0, sim, 0.001);
    }

    @Test
    @DisplayName("余弦相似度 - 部分相似")
    void testCosineSimilarityPartial() {
        double[] vec1 = {5, 3, 0, 1};
        double[] vec2 = {4, 0, 0, 1};
        double sim = cosineSimilarity(vec1, vec2);
        assertTrue(sim > 0.5 && sim < 1.0, "部分相似应在0.5~1.0之间, 实际=" + sim);
    }

    @Test
    @DisplayName("余弦相似度 - 零向量处理")
    void testCosineSimilarityZero() {
        double[] vec1 = {0, 0, 0};
        double[] vec2 = {1, 2, 3};
        double sim = cosineSimilarity(vec1, vec2);
        assertEquals(0.0, sim, 0.001);
    }

    /**
     * 余弦相似度计算 (复制自 RecommendServiceImpl)
     */
    private double cosineSimilarity(double[] vec1, double[] vec2) {
        double dotProduct = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < vec1.length; i++) {
            dotProduct += vec1[i] * vec2[i];
            norm1 += vec1[i] * vec1[i];
            norm2 += vec2[i] * vec2[i];
        }
        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }

    // ============================================================
    // 5. MongoDB文档实体测试
    // ============================================================

    @Test
    @DisplayName("OperationLog审计日志实体")
    void testOperationLog() {
        OperationLog log = new OperationLog();
        log.setUserId("doctor01");
        log.setRole("DOCTOR");
        log.setAction("DRUG_QUERY");
        log.setResource("阿莫西林");
        log.setIp("192.168.1.100");
        log.setTimestamp(LocalDateTime.now());

        assertEquals("doctor01", log.getUserId());
        assertEquals("DOCTOR", log.getRole());
        assertEquals("DRUG_QUERY", log.getAction());
        assertNotNull(log.getTimestamp());
    }

    @Test
    @DisplayName("QueryLog查询日志实体")
    void testQueryLog() {
        QueryLog qlog = new QueryLog("user1", "阿莫西林胶囊", 3, LocalDateTime.now());
        assertEquals("user1", qlog.getUserId());
        assertEquals("阿莫西林胶囊", qlog.getQuery());
        assertEquals(3, qlog.getResultCount());
    }

    // ============================================================
    // 6. RBAC权限模型测试
    // ============================================================

    @Test
    @DisplayName("角色实体")
    void testRole() {
        Role role = new Role();
        role.setRoleId(1L);
        role.setRoleName("药房管理员");
        role.setRoleCode("PHARMACY_ADMIN");
        assertEquals("PHARMACY_ADMIN", role.getRoleCode());
    }

    @Test
    @DisplayName("权限实体")
    void testPermission() {
        Permission perm = new Permission();
        perm.setPermId(1L);
        perm.setPermCode("DRUG_INFO_READ");
        perm.setResource("/api/drugs/**");
        perm.setAction("READ");
        assertEquals("DRUG_INFO_READ", perm.getPermCode());
        assertEquals("READ", perm.getAction());
    }

    @Test
    @DisplayName("用户药品交互记录")
    void testUserDrugInteraction() {
        UserDrugInteraction inter = new UserDrugInteraction();
        inter.setUserId(1L);
        inter.setDrugId(5L);
        inter.setFrequency(3);
        assertEquals(1L, inter.getUserId());
        assertEquals(5L, inter.getDrugId());
        assertEquals(3, inter.getFrequency());
    }
}
