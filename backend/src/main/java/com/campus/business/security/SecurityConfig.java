package com.campus.business.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final JwtAuthenticationFilter filter;

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain chain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .cors(cors -> {})
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/login",
                        "/api/mini/auth/wechat-login",
                        "/api/mini/public/**",
                        "/api/actuator/health",
                        "/error",
                        "/uploads/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/mini/**",
                        "/api/auth/me",
                        "/api/auth/logout",
                        "/api/auth/change-password")
                    .authenticated()
                    .requestMatchers("/api/**")
                    .hasAnyRole("OWNER", "ADMIN")
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, exception) -> writeError(response, 401, "登录已失效"))
                    .accessDeniedHandler(
                        (request, response, exception) -> writeError(response, 403, "无权执行此操作")))
        .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  private void writeError(HttpServletResponse response, int status, String message)
      throws java.io.IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    new ObjectMapper()
        .writeValue(
            response.getWriter(), Map.of("code", status, "message", message, "data", Map.of()));
  }
}
