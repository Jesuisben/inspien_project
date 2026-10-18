package com.inspien_project.config;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpLogFilter extends OncePerRequestFilter {

    public static final String requestIdAttribute = "inspien.requestId";
    private static final Logger log = LoggerFactory.getLogger(HttpLogFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(requestIdAttribute, requestId);
        response.setHeader("X-Request-Id", requestId);
        long started = System.nanoTime();
        boolean completed = false;
        log.info("requestId={} stage=HTTP_BEGIN method={}", requestId, request.getMethod());
        try {
            chain.doFilter(request, response);
            completed = true;
        } catch (ServletException | IOException | RuntimeException error) {
            if (!response.isCommitted()) response.setStatus(500);
            throw error;
        } finally {
            log.info("requestId={} stage=HTTP_END result={} status={} elapsedMs={}", requestId,
                    completed && response.getStatus() < 400 ? "SUCCESS" : "FAILURE",
                    response.getStatus(), (System.nanoTime() - started) / 1_000_000);
        }
    }
}
