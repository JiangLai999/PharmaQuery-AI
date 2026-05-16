package com.pharmacy.service;

import com.pharmacy.dto.DrugDTO;
import java.util.List;

/**
 * 智能推荐服务接口 (协同过滤)
 */
public interface RecommendService {

    /**
     * 为指定用户生成推荐列表
     * @param userId 用户ID
     * @param topK   返回前K个推荐
     * @return 推荐药品列表(含推荐理由)
     */
    List<DrugDTO> recommendDrugs(Long userId, int topK);

    /**
     * 计算两个用户之间的余弦相似度
     */
    double calculateUserSimilarity(Long userId1, Long userId2);
}
