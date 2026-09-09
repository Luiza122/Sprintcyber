package com.ford.fordretain.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectProvider<SecurityMetrics> securityMetricsProvider;
    @Value("${security.rate-limit.requests-per-minute:60}") private long requestsPerMinute;
    @Value("${security.rate-limit.login-requests-per-minute:10}") private long loginRequestsPerMinute;

    public RateLimitFilter(ObjectProvider<SecurityMetrics> securityMetricsProvider) { this.securityMetricsProvider = securityMetricsProvider; }
    private Bucket getBucket(String key, long limit) {
        return buckets.computeIfAbsent(key, ignored -> Bucket.builder().addLimit(Bandwidth.builder().capacity(limit).refillGreedy(limit, Duration.ofMinutes(1)).build()).build());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String ip = request.getRemoteAddr();
        boolean login = "/api/v1/auth/login".equals(request.getRequestURI());
        long limit = login ? loginRequestsPerMinute : requestsPerMinute;
        String bucketKey = ip + (login ? ":login" : ":general");
        if (getBucket(bucketKey, limit).tryConsume(1)) { chain.doFilter(request, response); return; }
        SecurityMetrics metrics = securityMetricsProvider.getIfAvailable();
        if (metrics != null) metrics.rateLimited();
        log.warn("Rate limit excedido | endpoint={} | ip={}", request.getRequestURI(), ip);
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"erro\":\"Muitas requisições. Tente novamente em instantes.\"}");
    }
}
