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

// 다른 객체에 주입할 수 있도록 Spring이 이 클래스를 찾아 생성하게 함
@Component
public class HttpLogFilter extends OncePerRequestFilter {

    // final은 이 참조의 재대입을 막음. 객체 내부까지 불변으로 만드는 뜻은 아니며 private은 별도의 접근 제한
    public static final String requestIdAttribute = "inspien.requestId";
    // final은 이 참조의 재대입을 막음. 객체 내부까지 불변으로 만드는 뜻은 아니며 private은 별도의 접근 제한
    private static final Logger log = LoggerFactory.getLogger(HttpLogFilter.class);

    @Override
    // Controller에 도달하지 못한 요청도 같은 추적 ID와 시작·종료 로그를 남기도록 HTTP 경계를 감쌈
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
