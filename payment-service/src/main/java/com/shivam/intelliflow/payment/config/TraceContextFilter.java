package com.shivam.intelliflow.payment.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceContextFilter extends OncePerRequestFilter {
    public static final String TRACE_ID = "traceId";
    public static final String SPAN_ID = "spanId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String SPAN_ID_HEADER = "X-Span-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = getOrCreateHeader(request, TRACE_ID_HEADER);
        String spanId = getOrCreateHeader(request, SPAN_ID_HEADER);

        MDC.put(TRACE_ID, traceId);
        MDC.put(SPAN_ID, spanId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        response.setHeader(SPAN_ID_HEADER, spanId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
            MDC.remove(SPAN_ID);
        }
    }

    private String getOrCreateHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }
}
