package com.schemebridge.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * JWT Authentication Gateway Filter.
 * <p>
 * Validates JWT Bearer tokens for protected endpoints and injects authenticated
 * user identity headers (X-User-Id, X-User-Email, X-User-Roles) into downstream requests.
 * <p>
 * Public endpoints bypass JWT validation entirely.
 * <p>
 * The frontend also sends X-User-Id from its apiClient interceptor as a fallback
 * to support the existing microservice @RequestHeader("X-User-Id") contract during
 * the transition period before full gateway-side JWT injection.
 */
@Component
public class JwtAuthGatewayFilterPlaceholder implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthGatewayFilterPlaceholder.class);

    private final SecretKey signingKey;

    // Public endpoints that bypass JWT validation
    private static final List<String> PUBLIC_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/send-email-otp",
            "/api/v1/auth/verify-email-otp",
            "/api/v1/auth/send-phone-otp",
            "/api/v1/auth/verify-phone-otp",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh-token",
            "/actuator/health",
            "/actuator/info"
    );

    public JwtAuthGatewayFilterPlaceholder(
            @Value("${jwt.secret}") String jwtSecret) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. Allow public endpoints without any JWT check
        if (isPublicEndpoint(path)) {
            log.debug("[JWT-GATEWAY] Public route: {}", path);
            return chain.filter(exchange);
        }

        // 2. Check for Authorization header
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // If no token is present on protected route, still pass through
            // (downstream microservices have defaultValue fallbacks for dev)
            // In production this should return 401.
            log.debug("[JWT-GATEWAY] No Bearer token on protected route: {}", path);
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        try {
            // 3. Validate JWT signature and parse claims
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(signingKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            String email = claims.get("email", String.class);
            Object rolesObj = claims.get("roles");
            String rolesStr = rolesObj != null ? rolesObj.toString() : "";

            log.debug("[JWT-GATEWAY] Valid JWT for userId={} path={}", userId, path);

            // 4. Inject identity headers into downstream request
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId : "")
                    .header("X-User-Email", email != null ? email : "")
                    .header("X-User-Roles", rolesStr)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (ExpiredJwtException ex) {
            log.warn("[JWT-GATEWAY] Expired JWT token for path: {}", path);
            return unauthorizedResponse(exchange, "Token expired");
        } catch (SecurityException | MalformedJwtException ex) {
            log.warn("[JWT-GATEWAY] Invalid JWT signature/format for path: {}", path);
            return unauthorizedResponse(exchange, "Invalid token");
        } catch (UnsupportedJwtException ex) {
            log.warn("[JWT-GATEWAY] Unsupported JWT for path: {}", path);
            return unauthorizedResponse(exchange, "Unsupported token");
        } catch (Exception ex) {
            log.error("[JWT-GATEWAY] JWT parsing error for path: {} — {}", path, ex.getMessage());
            // Pass through on unexpected errors to avoid blocking legitimate requests
            return chain.filter(exchange);
        }
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String reason) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("X-Auth-Error", reason);
        return response.setComplete();
    }

    private boolean isPublicEndpoint(String path) {
        return PUBLIC_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 2;
    }
}
