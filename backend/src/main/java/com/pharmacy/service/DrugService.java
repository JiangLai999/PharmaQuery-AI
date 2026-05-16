package com.pharmacy.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.dto.DrugDTO;
import com.pharmacy.dto.SearchRequest;
import com.pharmacy.entity.DrugInfo;
import java.util.List;
import java.util.Map;

/**
 * 药品查询服务接口
 */
public interface DrugService {

    /**
     * 多条件组合查询
     */
    Page<DrugDTO> search(SearchRequest request);

    /**
     * NLP语义查询
     */
    List<DrugDTO> nlpSearch(String query);

    /**
     * 根据关键词模糊搜索
     */
    List<DrugDTO> fuzzySearch(String keyword);

    /**
     * 根据ID获取药品详情
     */
    DrugDTO findById(Long drugId);

    /**
     * 获取近效期药品列表
     */
    List<DrugDTO> getNearExpiryDrugs();

    /**
     * 获取低库存药品列表
     */
    List<DrugDTO> getLowStockDrugs();

    /**
     * 按分类统计
     */
    List<Map<String, Object>> countByCategory();

    /**
     * 记录用户查询行为
     */
    void recordInteraction(Long userId, Long drugId);
}
