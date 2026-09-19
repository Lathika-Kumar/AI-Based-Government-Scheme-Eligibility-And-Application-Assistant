package com.schemebridge.scheme.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
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
        
        String jwt = parseJwt(request);

        if (jwt != null) {
            if (jwtTokenProvider.validateToken(jwt)) {
                String userId = jwtTokenProvider.getUserIdFromToken(jwt);
                List<String> roles = jwtTokenProvider.getRolesFromToken(jwt);

                if (userId != null && roles != null) {
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

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                // If a token is present but invalid/expired, we clear the context and do not authenticate.
                // This ensures that any subsequent security checks for protected endpoints will fail.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }
}
