package org.example.BenhAnDienTu.identity.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.BenhAnDienTu.identity.api.IdentityApi;
import org.example.BenhAnDienTu.identity.application.RolePermissionResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class IdentitySecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity httpSecurity,
      IdentityApi identityApi,
      ObjectMapper objectMapper,
      RolePermissionResolver rolePermissionResolver)
      throws Exception {
    ApiAuthenticationFilter apiAuthenticationFilter =
        new ApiAuthenticationFilter(identityApi, objectMapper);
    ApiAuthorizationFilter apiAuthorizationFilter =
        new ApiAuthorizationFilter(objectMapper, rolePermissionResolver);

    httpSecurity
        .cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterBefore(apiAuthenticationFilter, AnonymousAuthenticationFilter.class)
        .addFilterAfter(apiAuthorizationFilter, ApiAuthenticationFilter.class)
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers(
                        "/error",
                        "/api/ping",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/v1/auth/sign-in",
                        "/api/v1/auth/refresh",
                        "/api/v1/auth/sign-out",
                        "/api/v1/auth/introspect",
                        "/api/v1/auth/sign-up",
                        "/api/v1/auth/sign-up/verify-otp",
                        "/api/v1/auth/forgot-password",
                        "/api/v1/auth/forgot-password/verify-otp",
                        "/api/v1/auth/forgot-password/reset",
                        "/actuator/health",
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll());

    return httpSecurity.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }
}
