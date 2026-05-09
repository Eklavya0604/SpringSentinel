package com.Vaish.SpringSentinel.security;

import com.Vaish.SpringSentinel.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication
        .UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority
        .SimpleGrantedAuthority;
import org.springframework.security.core.context
        .SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getServletPath();

        // ── Bypass public paths ──────────────────────────────────
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader =
                request.getHeader("Authorization");

        // ── No token ─────────────────────────────────────────────
        if (authHeader == null ||
                !authHeader.startsWith("Bearer ")) {
            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"message\":" +
                            "\"Authorization header missing or invalid\"}"
            );
            return;
        }

        String token = authHeader.substring(7);

        // ── Invalid token ────────────────────────────────────────
        if (!jwtService.isTokenValid(token)) {
            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":401,\"message\":" +
                            "\"Invalid or expired token\"}"
            );
            return;
        }

        // ── Valid token → load user → set auth ───────────────────
        String username = jwtService.extractUsername(token);

        var user = userRepository
                .findByUsername(username)
                .orElseThrow();

        var authority = new SimpleGrantedAuthority(
                "ROLE_" + user.getRole().name()
        );

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        username, null, List.of(authority)
                );

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth")    ||
                path.startsWith("/swagger-ui")  ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/api-docs")    ||
                path.startsWith("/webjars");
    }
}