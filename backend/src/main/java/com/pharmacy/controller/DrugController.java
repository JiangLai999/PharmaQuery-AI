package com.pharmacy.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pharmacy.aspect.Audit;
import com.pharmacy.dto.ApiResult;
import com.pharmacy.dto.DrugDTO;
import com.pharmacy.dto.SearchRequest;
import com.pharmacy.entity.DrugInfo;
import com.pharmacy.mapper.DrugInfoMapper;
import com.pharmacy.service.DrugService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 药品控制器 (论文 2.3, 3.1, 3.4.1)
 *
 * 权限控制策略 (RBAC最小权限原则):
 *   查询类接口: 公开访问 (SecurityConfig permitAll)
 *   新增药品:   仅 PHARMACY_ADMIN / SYS_ADMIN
 *   编辑药品:   仅 PHARMACY_ADMIN / PHARMACIST / SYS_ADMIN
 *   删除药品:   仅 PHARMACY_ADMIN / SYS_ADMIN
 *   库存预警:   PHARMACIST / PHARMACY_ADMIN / SYS_ADMIN
 */
@RestController
@RequestMapping("/drugs")
@RequiredArgsConstructor
public class DrugController {

    private final DrugService drugService;
    private final DrugInfoMapper drugInfoMapper;

    // ==================== 查询类 (公开) ====================

    /**
     * 多条件组合查询 (论文 3.1.1)
     */
    @PostMapping("/search")
    @Audit("药品组合查询")
    public ApiResult<Page<DrugDTO>> search(@RequestBody SearchRequest request) {
        return ApiResult.success(drugService.search(request));
    }

    /**
     * NLP语义查询 (论文 3.1.2)
     */
    @PostMapping("/nlp-search")
    @Audit("NLP语义查询")
    public ApiResult<List<DrugDTO>> nlpSearch(@RequestParam String q) {
        return ApiResult.success(drugService.nlpSearch(q));
    }

    /**
     * NLP异步查询 (论文 4.3.2)
     */
    @GetMapping("/search/nlp")
    @Audit("NLP异步查询")
    public CompletableFuture<ApiResult<List<DrugDTO>>> nlpSearchAsync(@RequestParam String q) {
        return CompletableFuture.supplyAsync(() -> ApiResult.success(drugService.nlpSearch(q)));
    }

    /**
     * 模糊搜索
     */
    @GetMapping("/fuzzy")
    @Audit("药品模糊搜索")
    public ApiResult<List<DrugDTO>> fuzzySearch(@RequestParam String keyword) {
        return ApiResult.success(drugService.fuzzySearch(keyword));
    }

    /**
     * 药品详情
     */
    @GetMapping("/{id}")
    @Audit("查看药品详情")
    public ApiResult<DrugDTO> getById(@PathVariable("id") Long drugId) {
        DrugDTO drug = drugService.findById(drugId);
        return drug != null ? ApiResult.success(drug) : ApiResult.error(404, "药品不存在");
    }

    /**
     * 近效期药品 (论文 3.2.2)
     */
    @GetMapping("/near-expiry")
    @Audit("查看近效期药品")
    public ApiResult<List<DrugDTO>> getNearExpiry() {
        return ApiResult.success(drugService.getNearExpiryDrugs());
    }

    /**
     * 低库存药品
     */
    @GetMapping("/low-stock")
    @Audit("查看低库存药品")
    public ApiResult<List<DrugDTO>> getLowStock() {
        return ApiResult.success(drugService.getLowStockDrugs());
    }

    /**
     * 分类统计
     */
    @GetMapping("/stats/category")
    public ApiResult<List<Map<String, Object>>> statsByCategory() {
        return ApiResult.success(drugService.countByCategory());
    }

    // ==================== 写操作 (需权限, 论文 3.4.1) ====================

    /**
     * 新增药品 - 仅药房管理员和系统管理员
     */
    @PostMapping
    @Audit("新增药品")
    @PreAuthorize("hasAnyRole('PHARMACY_ADMIN', 'SYS_ADMIN')")
    public ApiResult<DrugInfo> addDrug(@RequestBody DrugInfo drug) {
        drug.setStatus(1);
        drugInfoMapper.insert(drug);
        return ApiResult.success(drug);
    }

    /**
     * 编辑药品 - 需具备药品编辑权限
     */
    @PutMapping("/{id}")
    @Audit("编辑药品信息")
    @PreAuthorize("hasAuthority('DRUG_INFO_WRITE')")
    public ApiResult<DrugInfo> updateDrug(@PathVariable("id") Long drugId, @RequestBody DrugInfo drug) {
        DrugInfo existing = drugInfoMapper.selectById(drugId);
        if (existing == null) {
            return ApiResult.error(404, "药品不存在");
        }
        drug.setDrugId(drugId);
        drugInfoMapper.updateById(drug);
        return ApiResult.success(drug);
    }

    /**
     * 删除药品(逻辑删除) - 仅药房管理员和系统管理员
     */
    @DeleteMapping("/{id}")
    @Audit("删除药品")
    @PreAuthorize("hasAnyRole('PHARMACY_ADMIN', 'SYS_ADMIN')")
    public ApiResult<Void> deleteDrug(@PathVariable("id") Long drugId) {
        DrugInfo drug = drugInfoMapper.selectById(drugId);
        if (drug == null) {
            return ApiResult.error(404, "药品不存在");
        }
        drug.setStatus(0); // 逻辑删除
        drugInfoMapper.updateById(drug);
        return ApiResult.success();
    }

    /**
     * 更新库存 - 药房管理员、药师、系统管理员
     */
    @PutMapping("/{id}/stock")
    @Audit("更新库存")
    @PreAuthorize("hasAnyAuthority('STOCK_WRITE')")
    public ApiResult<Void> updateStock(@PathVariable("id") Long drugId, @RequestParam Integer quantity) {
        DrugInfo drug = drugInfoMapper.selectById(drugId);
        if (drug == null) {
            return ApiResult.error(404, "药品不存在");
        }
        drug.setStockQuantity(quantity);
        drugInfoMapper.updateById(drug);
        return ApiResult.success();
    }

    // ==================== 行为记录 (需登录) ====================

    /**
     * 记录查看行为 (协同过滤数据采集)
     */
    @PostMapping("/{id}/view")
    public ApiResult<Void> recordView(@PathVariable("id") Long drugId, @RequestParam Long userId) {
        drugService.recordInteraction(userId, drugId);
        return ApiResult.success();
    }
}
