package com.yyt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors() // 启用CORS
            .and()
            .authorizeRequests()
                // 公开接口
                .antMatchers("/api/user/login", "/api/user/register", "/api/user/logout", "/api/time-slot/update-expired", "/api/time-slot/available").permitAll()
                // 文件上传接口 - 所有认证用户可访问
                .antMatchers("/api/upload/**").hasAnyAuthority("user", "clerk", "admin")
                // WebSocket端点
                .antMatchers("/ws/**", "/chat/ws").permitAll()
                // admin接口 - 使用hasAuthority避免ROLE_前缀问题
                .antMatchers("/api/user/save", "/api/user/status", "/api/user/addClerk", "/api/user/delete",
                            "/api/time-slot/list", "/api/time-slot/generate", "/api/time-slot/update-status", 
                            "/api/time-slot/delete").hasAuthority("admin")
                // 用户列表接口 - 所有认证用户可访问（用于投诉页面选择营业员）
                .antMatchers("/api/user/list").hasAnyAuthority("user", "clerk", "admin")
                // clerk接口
                .antMatchers("/api/appointment/process", "/api/window/**", "/api/appointment/today").hasAnyAuthority("clerk", "admin")
                // 客户接口
                .antMatchers("/api/appointment/create", "/api/appointment/cancel", "/api/appointment/user").hasAnyAuthority("user", "clerk", "admin")
                // 用户更新个人信息接口
                .antMatchers("/api/user/update").hasAnyAuthority("user", "clerk", "admin")
                // 通知接口 - 所有认证用户可访问
                .antMatchers("/api/notification/**").hasAnyAuthority("user", "clerk", "admin")
                // 聊天接口 - 所有认证用户可访问
                .antMatchers("/api/chat/**").hasAnyAuthority("user", "clerk", "admin")
                // 投诉接口 - 所有认证用户可访问
                .antMatchers("/api/complaint/**").hasAnyAuthority("user", "clerk", "admin")
                // 资源接口 - 所有认证用户可访问（用于预约页面获取业务类型）
                .antMatchers("/api/resource/**").hasAnyAuthority("user", "clerk", "admin")
                // 咨询接口 - 所有认证用户可访问
                .antMatchers("/api/consultation/**").hasAnyAuthority("user", "clerk", "admin")
                // 请假接口 - 所有认证用户可访问
                .antMatchers("/api/leave/**").hasAnyAuthority("user", "clerk", "admin")
                // 审批申请接口 - 提交和查询自己的申请：所有认证用户；审批操作和查询列表：仅admin
                .antMatchers("/api/approval/submit", "/api/approval/my").hasAnyAuthority("user", "clerk", "admin")
                .antMatchers("/api/approval/list", "/api/approval/pending", "/api/approval/statistics", "/api/approval/approve/**", "/api/approval/detail/**").hasAuthority("admin")
                // 其他所有接口需要认证
                .anyRequest().authenticated()
            .and()
            .csrf().disable()
            .formLogin().disable()
            .httpBasic().disable()
            // 启用Session管理
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
        return http.build();
    }
    
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.Arrays.asList("http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
