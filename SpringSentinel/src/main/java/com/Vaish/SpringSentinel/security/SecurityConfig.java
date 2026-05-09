package com.Vaish.SpringSentinel.security;

import com.Vaish.SpringSentinel.filter.RateLimitFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method
        .configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web
        .builders.HttpSecurity;
import org.springframework.security.config.annotation.web
        .configuration.EnableWebSecurity;
import org.springframework.security.config.http
        .SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt
        .BCryptPasswordEncoder;
import org.springframework.security.crypto.password
        .PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication
        .UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;
    private final RateLimitFilter rateLimitFilter;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Public ───────────────────────────────────
                        .requestMatchers("/api/auth/**")
                        .permitAll()
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/api-docs/**",
                                "/api-docs",
                                "/webjars/**"
                        ).permitAll()

                        // ── ADMIN only ───────────────────────────────
                        .requestMatchers("/api/bots/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/users/*/promote")
                        .hasRole("ADMIN")

                        // ── Everything else needs JWT ─────────────────
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        rateLimitFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(
                        jwtFilter,
                        RateLimitFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}