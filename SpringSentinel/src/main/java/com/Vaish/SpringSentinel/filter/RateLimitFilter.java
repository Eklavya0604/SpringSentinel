package com.Vaish.SpringSentinel.filter;

import com.Vaish.SpringSentinel.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // ========================================================
        // BYPASS — Public paths (no rate limiting needed)
        // ========================================================

        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ========================================================
        // RATE LIMIT CHECK
        // ========================================================

        String ip = getClientIp(request);

        if (rateLimiterService.isRateLimited(ip)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429," +
                            "\"message\":\"Too many requests. Max 100 per minute per IP.\"}"
            );
            return;
        }

        filterChain.doFilter(request, response);
    }

    // ============================================================
    // PUBLIC PATHS — Skip rate limiting
    // ============================================================

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth")    ||
                path.startsWith("/swagger-ui")  ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api-docs")    ||
                path.startsWith("/webjars");
    }

    // ============================================================
    // CLIENT IP — Handles proxies and load balancers
    // ============================================================

    private String getClientIp(HttpServletRequest request) {

        // X-Forwarded-For header is set by proxies/load balancers
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            // Can contain multiple IPs: "client, proxy1, proxy2"
            // First one is the real client IP
            return forwarded.split(",")[0].trim();
        }

        // Fallback to direct connection IP
        return request.getRemoteAddr();
    }
}