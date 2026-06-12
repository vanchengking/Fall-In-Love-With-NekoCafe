package com.nekocafe.config;

import com.nekocafe.security.JwtAuthFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 安全配置：JWT 无状态鉴权 + RBAC（customer/staff/manager/operator/cat_keeper/admin）。
 * 读取接口公开以保证演示可用，写入接口按角色限制。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/healthz", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // 管理接口必须排在 GET /api/** permitAll 之前，否则顾客可匿名读取管理数据
                        // FR-TABLE-003：店员桌位实时状态看板需要读桌位管理查询，GET 对 staff 放行；
                        // 写操作（POST/PUT/DELETE）落入下一条规则，仍限 manager/operator/admin
                        .requestMatchers(HttpMethod.GET, "/api/admin/tables/**")
                            .hasAnyRole("STAFF", "MANAGER", "OPERATOR", "ADMIN")
                        .requestMatchers("/api/admin/dashboard/**", "/api/admin/stores/**", "/api/admin/tables/**")
                            .hasAnyRole("MANAGER", "OPERATOR", "ADMIN")
                        .requestMatchers("/api/admin/**").hasAnyRole("MANAGER", "OPERATOR", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/reservations/*/status")
                            .hasAnyRole("STAFF", "MANAGER", "OPERATOR", "ADMIN")
                        // 取消接口对所有登录用户开放，由服务层校验本人/后台角色
                        .requestMatchers(HttpMethod.PATCH, "/api/reservations/*/cancel")
                            .authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/cat-health-records")
                            .hasAnyRole("CAT_KEEPER", "MANAGER", "ADMIN")
                        // 个人资料/会员/积分明细均为本人数据，必须登录（不能落入 GET /api/** permitAll）
                        .requestMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/upload").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/cats/*/photo", "/api/menu-items/*/photo").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reviews").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/reservations", "/api/orders").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> writeError(res, 401, "authentication required"))
                        .accessDeniedHandler((req, res, e) -> writeError(res, 403, "insufficient permission")))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse res, int status, String message) throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"error\":{\"message\":\"" + message + "\",\"status\":" + status + "}}");
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
