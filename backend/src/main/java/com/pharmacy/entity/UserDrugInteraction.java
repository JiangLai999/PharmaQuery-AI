package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户-药品交互记录 (协同过滤基础数据)
 */
@Data
@TableName("user_drug_interaction")
public class UserDrugInteraction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long drugId;

    /** 查询次数(隐式评分) */
    private Integer frequency;

    private LocalDateTime lastQuery;
}
