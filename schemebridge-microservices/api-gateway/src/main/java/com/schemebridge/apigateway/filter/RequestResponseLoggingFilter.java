package com.schemebridge.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Global Request & Response Logging Filter.
 * <p>
 * Logs structured request details (Method, Path, Client IP, Correlation ID) upon arrival,
 * and measures total execution latency (ms) and response HTTP status code upon completion.
 */
@Component
public class RequestResponseLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        String path = request.getURI().getPath();
        String method = request.getMethod().name();
        String correlationId = request.getHeaders().getFirst(CorrelationIdFilter.CORRELATION_ID_HEADER);
        String clientIp = request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress() 
                : "UNKNOWN";

        log.info("[GATEWAY-REQ] CorrelationId: {} | Method: {} | Path: {} | ClientIP: {}", 
                correlationId, method, path, clientIp);

        return chain.filter(exchange).doFinally(signalType -> {
            long durationMs = System.currentTimeMillis() - startTime;
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode();
            int code = statusCode != null ? statusCode.value() : 500;

            log.info("[GATEWAY-RESP] CorrelationId: {} | Method: {} | Path: {} | Status: {} | Duration: {}ms",
                    correlationId, method, path, code, durationMs);
        });
    }

    @Override
    public int getOrder() {
        // Execute immediately after Correlation ID filter
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
