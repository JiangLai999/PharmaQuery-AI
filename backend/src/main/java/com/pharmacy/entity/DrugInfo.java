package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 药品基础信息实体
 */
@Data
@TableName("drug_info")
public class DrugInfo {

    @TableId(type = IdType.AUTO)
    private Long drugId;

    /** 通用名 */
    private String genericName;

    /** 商品名 */
    private String tradeName;

    /** 规格 */
    private String specification;

    /** 剂型 */
    private String dosageForm;

    /** 生产厂家 */
    private String manufacturer;

    /** 批准文号 */
    private String approvalNumber;

    /** 条形码 */
    private String barcode;

    /** 药理分类 */
    private String category;

    /** 医保类别 */
    private String insuranceType;

    /** 适应症 */
    private String indication;

    /** 禁忌症 */
    private String contraindication;

    /** 药物相互作用 */
    private String interaction;

    /** 当前库存量 */
    private Integer stockQuantity;

    /** 补货阈值 */
    private Integer stockThreshold;

    /** 单价 */
    private BigDecimal unitPrice;

    /** 最近批次有效期 */
    private LocalDate expiryDate;

    /** 总有效期(天) */
    private Integer shelfLifeDays;

    /** 储存条件 */
    private String storageCondition;

    /** 状态 1正常 0停用 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
