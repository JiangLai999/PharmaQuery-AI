package com.pharmacy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pharmacy.entity.DrugInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

/**
 * 药品信息Mapper
 */
@Mapper
public interface DrugInfoMapper extends BaseMapper<DrugInfo> {

    /**
     * 根据适应症关键词模糊搜索
     */
    @Select("SELECT * FROM drug_info WHERE indication LIKE CONCAT('%', #{keyword}, '%') AND status = 1")
    List<DrugInfo> searchByIndication(@Param("keyword") String keyword);

    /**
     * 查询近效期药品 (距过期不足90天)
     */
    @Select("SELECT * FROM drug_info WHERE expiry_date IS NOT NULL AND DATEDIFF(expiry_date, CURDATE()) <= 90 AND DATEDIFF(expiry_date, CURDATE()) > 0 AND status = 1 ORDER BY expiry_date ASC")
    List<DrugInfo> findNearExpiryDrugs();

    /**
     * 查询低库存药品
     */
    @Select("SELECT * FROM drug_info WHERE stock_quantity <= stock_threshold AND status = 1 ORDER BY stock_quantity ASC")
    List<DrugInfo> findLowStockDrugs();

    /**
     * 按分类统计药品数量
     */
    @Select("SELECT category, COUNT(*) as cnt FROM drug_info WHERE status = 1 GROUP BY category ORDER BY cnt DESC")
    List<java.util.Map<String, Object>> countByCategory();
}
