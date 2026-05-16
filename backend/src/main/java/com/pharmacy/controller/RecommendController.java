package com.pharmacy.controller;

import com.pharmacy.aspect.Audit;
import com.pharmacy.dto.ApiResult;
import com.pharmacy.dto.DrugDTO;
import com.pharmacy.service.RecommendService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能推荐控制器 (论文 3.3)
 */
@RestController
@RequestMapping("/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final RecommendService recommendService;

    /**
     * 获取个性化药品推荐
     * GET /api/recommend/{userId}?topK=8
     */
    @GetMapping("/{userId}")
    @Audit("获取药品推荐")
    @PreAuthorize("hasAuthority('RECOMMEND_READ') and !hasRole('SYS_ADMIN')")
    public ApiResult<List<DrugDTO>> recommend(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "8") int topK) {
        List<DrugDTO> recommendations = recommendService.recommendDrugs(userId, topK);
        return ApiResult.success(recommendations);
    }

    /**
     * 计算两个用户的相似度 (调试接口)
     * GET /api/recommend/similarity?u1=1&u2=2
     */
    @GetMapping("/similarity")
    @PreAuthorize("hasRole('SYS_ADMIN')")
    public ApiResult<Double> similarity(@RequestParam Long u1, @RequestParam Long u2) {
        double sim = recommendService.calculateUserSimilarity(u1, u2);
        return ApiResult.success(sim);
    }
}
