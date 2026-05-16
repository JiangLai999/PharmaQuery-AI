package com.pharmacy.controller;

import com.pharmacy.aspect.Audit;
import com.pharmacy.dto.*;
import com.pharmacy.entity.Permission;
import com.pharmacy.entity.Role;
import com.pharmacy.entity.UserInfo;
import com.pharmacy.mapper.PermissionMapper;
import com.pharmacy.mapper.RoleMapper;
import com.pharmacy.mapper.UserInfoMapper;
import com.pharmacy.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证控制器 - 登录/注册
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserInfoMapper userInfoMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 用户名密码登录
     */
    @PostMapping("/login")
    @Audit("用户登录")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            UserInfo user = userInfoMapper.findByUsername(request.getUsername());
            Role role = roleMapper.selectById(user.getRoleId());
            List<Permission> permissions = permissionMapper.findByRoleId(user.getRoleId());

            String token = tokenProvider.generateToken(user.getUsername(), user.getUserId(), role.getRoleCode());

            // 更新最后登录时间
            user.setLastLogin(LocalDateTime.now());
            userInfoMapper.updateById(user);

            LoginResponse response = new LoginResponse();
            response.setToken(token);
            response.setUserId(user.getUserId());
            response.setUsername(user.getUsername());
            response.setRealName(user.getRealName());
            response.setRole(role.getRoleCode());
            response.setPermissions(permissions.stream()
                    .map(Permission::getPermCode).collect(Collectors.toList()));

            return ApiResult.success(response);
        } catch (Exception e) {
            return ApiResult.error(401, "用户名或密码错误");
        }
    }

    /**
     * 微信小程序登录 (通过wx.login获取的code)
     */
    @PostMapping("/wx-login")
    @Audit("微信登录")
    public ApiResult<LoginResponse> wxLogin(@RequestBody LoginRequest request) {
        // 实际项目中需调用微信接口换取openid
        // 这里简化处理: 通过code模拟获取openid
        String openid = "wx_" + request.getWxCode();

        UserInfo user = userInfoMapper.findByOpenid(openid);
        if (user == null) {
            // 自动注册新用户 (默认医生角色)
            user = new UserInfo();
            user.setOpenid(openid);
            user.setUsername("wx_" + System.currentTimeMillis());
            user.setPassword(passwordEncoder.encode("wx_default"));
            user.setRoleId(2L); // 默认临床医生角色
            user.setStatus(1);
            userInfoMapper.insert(user);
        }

        Role role = roleMapper.selectById(user.getRoleId());
        List<Permission> permissions = permissionMapper.findByRoleId(user.getRoleId());
        String token = tokenProvider.generateToken(user.getUsername(), user.getUserId(), role.getRoleCode());

        user.setLastLogin(LocalDateTime.now());
        userInfoMapper.updateById(user);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUserId(user.getUserId());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRole(role.getRoleCode());
        response.setPermissions(permissions.stream()
                .map(Permission::getPermCode).collect(Collectors.toList()));

        return ApiResult.success(response);
    }

    /**
     * 初始化所有用户密码为123456 (仅开发环境使用)
     * GET /api/auth/init-passwords
     */
    @GetMapping("/init-passwords")
    public ApiResult<String> initPasswords() {
        String encoded = passwordEncoder.encode("123456");
        List<UserInfo> users = userInfoMapper.selectList(null);
        for (UserInfo u : users) {
            u.setPassword(encoded);
            userInfoMapper.updateById(u);
        }
        return ApiResult.success("已重置 " + users.size() + " 个用户密码为123456");
    }
}
