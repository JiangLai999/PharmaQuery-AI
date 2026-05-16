package com.pharmacy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pharmacy.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

    @Select("SELECT p.* FROM permission p " +
            "INNER JOIN role_permission rp ON p.perm_id = rp.perm_id " +
            "WHERE rp.role_id = #{roleId}")
    List<Permission> findByRoleId(@Param("roleId") Long roleId);
}
