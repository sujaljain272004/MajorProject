package com.chargeup.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 180;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ApiRateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            filterChain.doFilter(request, response);
            return;
        }

        long window = Instant.now().getEpochSecond() / 60;
        String key = request.getRemoteAddr() + ":" + window;
        WindowCounter counter = counters.compute(key, (ignored, existing) ->
            existing == null || existing.window() != window ? new WindowCounter(window, 1) : existing.increment()
        );
        counters.entrySet().removeIf(entry -> entry.getValue().window() < window - 1);

        if (counter.count() > MAX_REQUESTS_PER_MINUTE) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), Map.of(
                "status", 429,
                "message", "Rate limit exceeded. Retry shortly."
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private record WindowCounter(long window, int count) {
        WindowCounter increment() {
            return new WindowCounter(window, count + 1);
        }
    }
}
