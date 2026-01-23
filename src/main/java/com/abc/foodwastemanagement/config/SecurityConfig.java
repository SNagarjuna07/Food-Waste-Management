package com.abc.foodwastemanagement.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.abc.foodwastemanagement.security.JwtAuthenticationFilter;
import com.abc.foodwastemanagement.security.RestAccessDeniedHandler;

/**
 * Responsibilities of THIS class:
 * --------------------------------
 * 1. JWT authentication
 * 2. Authorization rules
 * 3. Proper 401 / 403 handling
 *
 * Rate limiting is handled:
 * - BEFORE Spring Security (PreAuthRateLimitingFilter)
 * - OR after authentication but NEVER via Spring exceptions
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    /**
     * JWT authentication filter.
     *
     * Reads Authorization header,
     * validates token,
     * sets Authentication in SecurityContext.
     */
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Handles 401 Unauthorized.
     *
     * Triggered when:
     * - JWT is missing
     * - JWT is invalid / expired
     */
    @Autowired
    private AuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Handles 403 Forbidden.
     *
     * Triggered when:
     * - User is authenticated
     * - But lacks required permission/role
     *
     * IMPORTANT:
     * - Does NOT handle rate limiting
     */
    @Autowired
    private RestAccessDeniedHandler accessDeniedHandler;

    /*
     * SECURITY FILTER CHAIN
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            /*
             * Disable CSRF:
             * - REST API
             * - Stateless JWT auth
             */
            .csrf(csrf -> csrf.disable())

            /*
             * Stateless session:
             * - No HttpSession
             * - Every request must carry JWT
             */
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            /*
             * Exception handling:
             *
             * AuthenticationException → 401
             * AccessDeniedException  → 403
             */
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            /*
             * Authorization rules:
             *
             * /auth/**        → public
             * /manage-app/** → public (or admin if you enable later)
             * others         → authenticated
             */
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                .requestMatchers("/manage-app/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )

            /*
             * JWT filter MUST run before UsernamePasswordAuthenticationFilter
             * so SecurityContext is populated early.
             */
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    /**
     * Password encoder.
     *
     * BCrypt:
     * - salted
     * - adaptive
     * - industry standard
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
