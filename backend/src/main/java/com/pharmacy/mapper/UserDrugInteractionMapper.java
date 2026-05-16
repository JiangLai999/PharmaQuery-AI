package com.pharmacy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pharmacy.entity.UserDrugInteraction;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserDrugInteractionMapper extends BaseMapper<UserDrugInteraction> {

    @Select("SELECT * FROM user_drug_interaction WHERE user_id = #{userId} ORDER BY frequency DESC")
    List<UserDrugInteraction> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_drug_interaction WHERE drug_id = #{drugId}")
    List<UserDrugInteraction> findByDrugId(@Param("drugId") Long drugId);

    /**
     * 记录或更新用户-药品交互 (查询次数+1)
     */
    @Insert("INSERT INTO user_drug_interaction (user_id, drug_id, frequency, last_query) " +
            "VALUES (#{userId}, #{drugId}, 1, NOW()) " +
            "ON DUPLICATE KEY UPDATE frequency = frequency + 1, last_query = NOW()")
    void upsertInteraction(@Param("userId") Long userId, @Param("drugId") Long drugId);

    /**
     * 获取所有有交互记录的用户ID
     */
    @Select("SELECT DISTINCT user_id FROM user_drug_interaction")
    List<Long> findAllActiveUserIds();
}
