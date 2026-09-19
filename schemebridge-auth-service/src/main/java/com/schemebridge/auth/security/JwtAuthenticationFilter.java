package com.schemebridge.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        if (jwtTokenProvider.validateToken(jwt)) {
            String userId = jwtTokenProvider.getUserIdFromToken(jwt);
            List<String> roles = jwtTokenProvider.getRolesFromToken(jwt);

            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .flatMap(role -> {
                            String r = role.trim().toUpperCase();
                            String raw = r.startsWith("ROLE_") ? r.substring(5) : r;
                            List<String> list = new java.util.ArrayList<>();
                            list.add("ROLE_" + raw);
                            if ("ADMINISTRATOR".equals(raw) || "ADMIN".equals(raw) || "SUPER_ADMIN".equals(raw)) {
                                list.add("ROLE_ADMIN");
                                list.add("ROLE_ADMINISTRATOR");
                            } else if ("SCHEME_MANAGER".equals(raw) || "MANAGER".equals(raw)) {
                                list.add("ROLE_SCHEME_MANAGER");
                            } else if ("VERIFICATION_OFFICER".equals(raw) || "OFFICER".equals(raw)) {
                                list.add("ROLE_VERIFICATION_OFFICER");
                            }
                            return list.stream().distinct().map(SimpleGrantedAuthority::new);
                        })
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userId, // subject/userId as primary identity
                        null,
                        authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
