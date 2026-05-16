package com.pharmacy.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色实体
 */
@Data
@TableName("role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long roleId;

    private String roleName;
    private String roleCode;
    private String description;
    private LocalDateTime createdAt;
}
