package com.pharmacy.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 药品信息返回DTO
 */
@Data
public class DrugDTO {
    private Long drugId;
    private String genericName;
    private String tradeName;
    private String specification;
    private String dosageForm;
    private String manufacturer;
    private String approvalNumber;
    private String barcode;
    private String category;
    private String insuranceType;
    private String indication;
    private String contraindication;
    private String interaction;
    private Integer stockQuantity;
    private Integer stockThreshold;
    private BigDecimal unitPrice;
    private LocalDate expiryDate;
    private Integer shelfLifeDays;
    private String storageCondition;
    private Integer status;

    /** 风险等级: normal / warning / danger */
    private String riskLevel;
    /** 推荐理由(推荐场景使用) */
    private String recommendation;
}
