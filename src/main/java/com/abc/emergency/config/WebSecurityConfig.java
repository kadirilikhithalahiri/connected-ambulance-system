package com.abc.emergency.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(requestHandler)
                .ignoringRequestMatchers(
                        "/api/auth/login",
                        "/api/auth/csrf"
                )
            )
            .authorizeHttpRequests(auth -> auth
                // Public web assets and authentication endpoints
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico"
                ).permitAll()
                .requestMatchers(
                        "/api/auth/login",
                        "/api/auth/csrf"
                ).permitAll()

                // Role-restricted endpoint prefixes based on the six system roles
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/dispatcher/**").hasAnyRole("ADMIN", "DISPATCHER")
                .requestMatchers("/api/paramedic/**").hasAnyRole("ADMIN", "PARAMEDIC")
                .requestMatchers("/api/doctor/**").hasAnyRole("ADMIN", "EMERGENCY_DOCTOR")
                .requestMatchers("/api/driver/**").hasAnyRole("ADMIN", "AMBULANCE_DRIVER")
                .requestMatchers("/api/hospital/**").hasAnyRole("ADMIN", "HOSPITAL_ADMIN")

                // Deny-by-default for all other API endpoints
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write(
                            "{\"status\":401,\"message\":\"Authentication required. Please sign in.\",\"data\":null}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.getWriter().write(
                            "{\"status\":403,\"message\":\"Access denied. Insufficient role permissions.\",\"data\":null}"
                    );
                })
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(sessionFixation -> sessionFixation.changeSessionId())
                .maximumSessions(2)
            );

        return http.build();
    }
}
