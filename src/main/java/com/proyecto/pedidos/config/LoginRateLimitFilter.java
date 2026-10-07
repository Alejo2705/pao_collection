package com.proyecto.pedidos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.ArrayDeque;

/** Global bounded limiter for this single-admin application; no trust in proxy IP headers. */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private final ArrayDeque<Long> attempts = new ArrayDeque<>();

    private synchronized boolean allow() {
        long now = System.currentTimeMillis();
        while (!attempts.isEmpty() && now - attempts.peekFirst() >= 60_000) attempts.removeFirst();
        if (attempts.size() >= 10) return false;
        attempts.addLast(now);
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("POST".equals(request.getMethod()) &&
                (request.getContextPath() + "/login").equals(request.getRequestURI()) && !allow()) {
            response.setHeader("Retry-After", "60");
            response.setStatus(429);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Demasiados intentos. Espera un minuto.");
            return;
        }
        chain.doFilter(request, response);
    }
}
