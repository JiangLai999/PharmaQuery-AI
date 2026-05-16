package com.pharmacy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pharmacy.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {

    @Select("SELECT * FROM user_info WHERE username = #{username} AND status = 1")
    UserInfo findByUsername(@Param("username") String username);

    @Select("SELECT * FROM user_info WHERE openid = #{openid} AND status = 1")
    UserInfo findByOpenid(@Param("openid") String openid);
}
