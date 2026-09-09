package com.ford.fordretain.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class AuditLogFilter extends OncePerRequestFilter {
    private final ObjectProvider<SecurityMetrics> securityMetricsProvider;
    public AuditLogFilter(ObjectProvider<SecurityMetrics> securityMetricsProvider) { this.securityMetricsProvider = securityMetricsProvider; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        long inicio = System.currentTimeMillis();
        MDC.put("request_id", UUID.randomUUID().toString());
        try { chain.doFilter(request, wrappedResponse); }
        finally {
            long duracao = System.currentTimeMillis() - inicio;
            int status = wrappedResponse.getStatus();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usuario = (auth != null && auth.isAuthenticated()) ? mask(auth.getName()) : "anonimo";
            MDC.put("event", "http_audit"); MDC.put("http_method", request.getMethod()); MDC.put("http_path", request.getRequestURI());
            MDC.put("http_status", String.valueOf(status)); MDC.put("duration_ms", String.valueOf(duracao)); MDC.put("client_ip", request.getRemoteAddr()); MDC.put("principal", usuario);
            log.info("Requisição processada");
            if (status == 401 || status == 403) {
                SecurityMetrics metrics = securityMetricsProvider.getIfAvailable();
                if (metrics != null) metrics.accessDenied();
                MDC.put("event", "access_denied"); log.warn("Acesso negado");
            }
            MDC.clear(); wrappedResponse.copyBodyToResponse();
        }
    }

    private String mask(String value) {
        if (value == null || value.isBlank()) return "anonimo";
        int at = value.indexOf('@');
        if (at > 1) return value.charAt(0) + "***" + value.substring(at);
        return "***";
    }
}
