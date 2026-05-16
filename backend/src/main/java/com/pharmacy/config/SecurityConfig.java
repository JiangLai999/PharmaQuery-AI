package com.pharmacy.config;

import com.pharmacy.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Spring Security 配置 (论文 3.4.1 RBAC权限控制)
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
                // 公开接口 (无需登录)
                .antMatchers("/auth/**").permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // 药品查询: 需具备药品查看权限
                .antMatchers(HttpMethod.GET, "/drugs/fuzzy").hasAuthority("DRUG_INFO_READ")
                .antMatchers(HttpMethod.GET, "/drugs/near-expiry").hasAuthority("STOCK_READ")
                .antMatchers(HttpMethod.GET, "/drugs/low-stock").hasAuthority("STOCK_READ")
                .antMatchers(HttpMethod.GET, "/drugs/stats/**").hasAuthority("DRUG_INFO_READ")
                .antMatchers(HttpMethod.GET, "/drugs/*").hasAuthority("DRUG_INFO_READ")
                .antMatchers(HttpMethod.POST, "/drugs/search").hasAuthority("DRUG_INFO_READ")
                .antMatchers(HttpMethod.POST, "/drugs/nlp-search").hasAuthority("DRUG_INFO_READ")
                .antMatchers(HttpMethod.GET, "/drugs/search/nlp").hasAuthority("DRUG_INFO_READ")
                // 推荐接口: 需要登录
                .antMatchers("/recommend/**").authenticated()
                // 记录行为: 需要登录
                .antMatchers(HttpMethod.POST, "/drugs/*/view").authenticated()
                // 药品管理: 按权限控制写操作
                .antMatchers(HttpMethod.POST, "/drugs").hasAnyRole("PHARMACY_ADMIN", "SYS_ADMIN")
                .antMatchers(HttpMethod.PUT, "/drugs/*/stock").hasAuthority("STOCK_WRITE")
                .antMatchers(HttpMethod.PUT, "/drugs/**").hasAuthority("DRUG_INFO_WRITE")
                .antMatchers(HttpMethod.DELETE, "/drugs/**").hasAnyRole("PHARMACY_ADMIN", "SYS_ADMIN")
                // 用户管理: 仅系统管理员
                .antMatchers("/users/**").hasRole("SYS_ADMIN")
                // 日志查看
                .antMatchers("/logs/**").hasAnyRole("PHARMACY_ADMIN", "SYS_ADMIN")
                .anyRequest().authenticated()
            .and()
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
