package com.harrish.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter that generates and maintains request context using MDC (Mapped Diagnostic Context).
 * 
 * <p>This filter runs at highest precedence to ensure all logs within a request lifecycle
 * contain correlation identifiers like requestId and userId. These identifiers enable
 * tracing requests across all application layers and services.</p>
 * 
 * <p>MDC Context:</p>
 * <ul>
 *   <li><b>requestId</b> - Unique identifier for each HTTP request (UUID or from X-Request-ID header)</li>
 *   <li><b>userId</b> - Added later by authentication filter after user is authenticated</li>
 * </ul>
 * 
 * <p>The requestId is also added to the HTTP response as X-Request-ID header for client-side correlation.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-ID";
    private static final String REQUEST_ID_MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        try {
            // Generate or extract requestId from header
            String requestId = request.getHeader(REQUEST_ID_HEADER);
            if (requestId == null || requestId.isBlank()) {
                requestId = UUID.randomUUID().toString();
            }
            
            // Put requestId in MDC for all logs in this request
            MDC.put(REQUEST_ID_MDC_KEY, requestId);
            
            // Add requestId to response header for client-side correlation
            response.setHeader(REQUEST_ID_HEADER, requestId);
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } finally {
            // Always clean up MDC to prevent memory leaks
            MDC.clear();
        }
    }
}
