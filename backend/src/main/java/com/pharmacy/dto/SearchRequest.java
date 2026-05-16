package com.pharmacy.dto;

import lombok.Data;

/**
 * 药品搜索请求
 */
@Data
public class SearchRequest {
    /** 自然语言查询 / 关键词 */
    private String query;
    /** 通用名 */
    private String genericName;
    /** 剂型 */
    private String dosageForm;
    /** 批准文号 */
    private String approvalNumber;
    /** 生产厂家 */
    private String manufacturer;
    /** 药理分类 */
    private String category;
    /** 医保类别 */
    private String insuranceType;
    /** 库存状态: all / inStock / lowStock / outOfStock */
    private String stockStatus;
    /** 页码 */
    private Integer pageNum = 1;
    /** 每页条数 */
    private Integer pageSize = 20;
}
