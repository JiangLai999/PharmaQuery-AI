package com.pharmacy.dto;

import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {
    private String username;
    private String password;
    /** 微信登录code (小程序场景) */
    private String wxCode;
}
