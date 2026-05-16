package com.pharmacy.service.impl;

import com.pharmacy.dto.DrugDTO;
import com.pharmacy.entity.DrugInfo;
import com.pharmacy.entity.UserDrugInteraction;
import com.pharmacy.mapper.DrugInfoMapper;
import com.pharmacy.mapper.UserDrugInteractionMapper;
import com.pharmacy.service.RecommendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 协同过滤推荐服务实现 (论文 3.3)
 *
 * 基于用户的协同过滤 (User-Based CF):
 * 1. 构建用户-药品交互矩阵
 * 2. 计算用户间余弦相似度
 * 3. 选取Top-K邻居用户
 * 4. 加权推荐邻居用户高频查询但当前用户未查询的药品
 * 5. 附加推荐理由 (可解释性, 论文 3.3.2)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendServiceImpl implements RecommendService {

    private final UserDrugInteractionMapper interactionMapper;
    private final DrugInfoMapper drugInfoMapper;

    /** 邻居用户数量 */
    private static final int NEIGHBOR_K = 30;
    /** 最低相似度阈值 */
    private static final double MIN_SIMILARITY = 0.45;

    @Override
    public List<DrugDTO> recommendDrugs(Long userId, int topK) {
        // 1. 获取当前用户的交互记录
        List<UserDrugInteraction> userInteractions = interactionMapper.findByUserId(userId);
        if (userInteractions.isEmpty()) {
            // 冷启动: 返回热门药品
            return getHotDrugs(topK);
        }

        Set<Long> userDrugIds = userInteractions.stream()
                .map(UserDrugInteraction::getDrugId)
                .collect(Collectors.toSet());

        // 2. 获取所有活跃用户
        List<Long> allUserIds = interactionMapper.findAllActiveUserIds();

        // 3. 计算与所有用户的相似度, 取Top-K邻居
        Map<Long, Double> similarities = new HashMap<>();
        for (Long otherId : allUserIds) {
            if (otherId.equals(userId)) continue;
            double sim = calculateUserSimilarity(userId, otherId);
            if (sim >= MIN_SIMILARITY) {
                similarities.put(otherId, sim);
            }
        }

        List<Map.Entry<Long, Double>> neighbors = similarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(NEIGHBOR_K)
                .collect(Collectors.toList());

        // 4. 加权计算推荐得分
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, List<Long>> drugRecommenders = new HashMap<>(); // 记录推荐来源

        for (Map.Entry<Long, Double> neighbor : neighbors) {
            Long neighborId = neighbor.getKey();
            double weight = neighbor.getValue();

            List<UserDrugInteraction> neighborInteractions = interactionMapper.findByUserId(neighborId);
            for (UserDrugInteraction inter : neighborInteractions) {
                Long drugId = inter.getDrugId();
                if (!userDrugIds.contains(drugId)) {
                    scores.merge(drugId, weight * inter.getFrequency(), Double::sum);
                    drugRecommenders.computeIfAbsent(drugId, k -> new ArrayList<>()).add(neighborId);
                }
            }
        }

        // 5. 排序取Top-K, 附加推荐理由
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> {
                    DrugInfo drug = drugInfoMapper.selectById(entry.getKey());
                    if (drug == null) return null;
                    DrugDTO dto = new DrugDTO();
                    BeanUtils.copyProperties(drug, dto);

                    // 生成推荐理由 (论文 3.3.2 可解释性)
                    int recommenderCount = drugRecommenders.getOrDefault(entry.getKey(), Collections.emptyList()).size();
                    int percentage = (int) ((double) recommenderCount / neighbors.size() * 100);
                    dto.setRecommendation(
                        String.format("有%d%%的相似用户也查询了此药品", Math.min(percentage, 100))
                    );
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 余弦相似度计算 (论文公式)
     * sim(u1, u2) = Σ(r_u1_i * r_u2_i) / (||r_u1|| * ||r_u2||)
     */
    @Override
    public double calculateUserSimilarity(Long userId1, Long userId2) {
        List<UserDrugInteraction> interactions1 = interactionMapper.findByUserId(userId1);
        List<UserDrugInteraction> interactions2 = interactionMapper.findByUserId(userId2);

        Map<Long, Integer> ratings1 = interactions1.stream()
                .collect(Collectors.toMap(UserDrugInteraction::getDrugId, UserDrugInteraction::getFrequency));
        Map<Long, Integer> ratings2 = interactions2.stream()
                .collect(Collectors.toMap(UserDrugInteraction::getDrugId, UserDrugInteraction::getFrequency));

        // 共同评分药品集合
        Set<Long> commonItems = new HashSet<>(ratings1.keySet());
        commonItems.retainAll(ratings2.keySet());

        if (commonItems.isEmpty()) return 0.0;

        double dotProduct = 0, norm1 = 0, norm2 = 0;
        for (Long drugId : commonItems) {
            int r1 = ratings1.get(drugId);
            int r2 = ratings2.get(drugId);
            dotProduct += r1 * r2;
            norm1 += r1 * r1;
            norm2 += r2 * r2;
        }

        double denominator = Math.sqrt(norm1) * Math.sqrt(norm2);
        return denominator == 0 ? 0.0 : dotProduct / denominator;
    }

    /**
     * 冷启动: 返回热门药品
     */
    private List<DrugDTO> getHotDrugs(int topK) {
        List<DrugInfo> drugs = drugInfoMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<DrugInfo>()
                .eq("status", 1)
                .orderByDesc("stock_quantity")
                .last("LIMIT " + topK)
        );
        return drugs.stream().map(d -> {
            DrugDTO dto = new DrugDTO();
            BeanUtils.copyProperties(d, dto);
            dto.setRecommendation("热门药品推荐");
            return dto;
        }).collect(Collectors.toList());
    }
}
