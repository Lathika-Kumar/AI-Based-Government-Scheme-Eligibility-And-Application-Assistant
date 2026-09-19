package com.schemebridge.scheme.config;

import com.schemebridge.scheme.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Swagger and Actuator endpoints
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                
                // Public GET endpoints
                .requestMatchers(HttpMethod.GET, "/api/schemes/**", "/api/categories/**", "/api/schemes/categories/**").permitAll()
                
                // Eligibility evaluation endpoints (must precede generic state-modifying rules)
                .requestMatchers(HttpMethod.POST, "/api/schemes/eligibility/evaluate-all").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/schemes/*/eligibility/evaluate").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/schemes/code/*/eligibility/evaluate").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/schemes/recommendations").authenticated()
                
                // Application endpoints
                .requestMatchers("/api/applications/**").authenticated()
                
                // Citizen grievance and notification endpoints
                .requestMatchers("/api/grievances/**").authenticated()
                .requestMatchers("/api/notifications/**").authenticated()
                
                // Citizen persistent profile & feedback endpoints
                .requestMatchers("/api/profile/**").authenticated()
                .requestMatchers("/api/feedback/**").authenticated()


                // Conversational AI endpoints
                .requestMatchers("/api/ai/citizen/**", "/api/ai/chat", "/api/v1/ai/**").authenticated()
                .requestMatchers("/api/ai/admin/**").hasAnyRole("SCHEME_MANAGER", "ADMIN", "VERIFICATION_OFFICER", "ADMINISTRATOR")

                // All Admin Operations endpoints
                .requestMatchers("/api/admin/**").hasAnyRole("SCHEME_MANAGER", "ADMIN", "VERIFICATION_OFFICER", "ADMINISTRATOR")

                // State-modifying endpoints (POST, PUT, PATCH, DELETE) under /api/schemes require SCHEME_MANAGER or ADMIN
                .requestMatchers(HttpMethod.POST, "/api/schemes/**").hasAnyRole("SCHEME_MANAGER", "ADMIN", "ADMINISTRATOR")
                .requestMatchers(HttpMethod.PUT, "/api/schemes/**").hasAnyRole("SCHEME_MANAGER", "ADMIN", "ADMINISTRATOR")
                .requestMatchers(HttpMethod.PATCH, "/api/schemes/**").hasAnyRole("SCHEME_MANAGER", "ADMIN", "ADMINISTRATOR")
                .requestMatchers(HttpMethod.DELETE, "/api/schemes/**").hasAnyRole("SCHEME_MANAGER", "ADMIN", "ADMINISTRATOR")
                
                // Any other request must be authenticated
                .anyRequest().authenticated()

            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of("http://localhost:*", "http://127.0.0.1:*"));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "Cache-Control"));
        configuration.setAllowCredentials(true);
        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}


