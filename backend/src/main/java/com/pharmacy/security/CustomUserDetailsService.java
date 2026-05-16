package com.pharmacy.security;

import com.pharmacy.entity.Permission;
import com.pharmacy.entity.Role;
import com.pharmacy.entity.UserInfo;
import com.pharmacy.mapper.PermissionMapper;
import com.pharmacy.mapper.RoleMapper;
import com.pharmacy.mapper.UserInfoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security 用户详情服务
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserInfoMapper userInfoMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserInfo user = userInfoMapper.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new DisabledException("用户已被禁用: " + username);
        }

        Role role = roleMapper.selectById(user.getRoleId());
        List<Permission> permissions = permissionMapper.findByRoleId(user.getRoleId());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (role != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleCode()));
        }
        for (Permission perm : permissions) {
            authorities.add(new SimpleGrantedAuthority(perm.getPermCode()));
        }

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(), user.getPassword(), authorities);
    }
}
