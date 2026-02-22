package com.beingadish.AroundU.infrastructure.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Interceptor that generates a unique UUID for every incoming HTTP request.
 * <ul>
 * <li>Stores the ID in SLF4J's {@link MDC} so all log statements produced
 * during the same request automatically include it.</li>
 * <li>Stores the ID in {@link RequestContextHolder} so other components
 * (services, filters) in the same thread can retrieve it.</li>
 * <li>Adds the ID as the {@code X-Request-ID} response header for client
 * correlation.</li>
 * </ul>
 */
@Slf4j
@Component
public class RequestIdInterceptor implements HandlerInterceptor {

    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String REQUEST_ID_MDC_KEY = "RequestID";
    public static final String REQUEST_ID_ATTR = "requestId";

    /**
     * Thread-local storage so the request ID is available anywhere in the
     * current thread without needing access to the servlet request.
     */
    private static final ThreadLocal<String> REQUEST_ID_HOLDER = new ThreadLocal<>();

    /**
     * Static accessor so any code running on the same thread can retrieve the
     * current request ID without a reference to the servlet request.
     *
     * @return the current request ID, or {@code null} if not set
     */
    public static String getCurrentRequestId() {
        return REQUEST_ID_HOLDER.get();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Honour an incoming request ID (e.g. from an API gateway) or generate a new one
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        // MDC — every log line for this request will include RequestID
        MDC.put(REQUEST_ID_MDC_KEY, requestId);

        // Thread-local — available anywhere in the same thread
        REQUEST_ID_HOLDER.set(requestId);

        // Request attribute — available in controllers via HttpServletRequest
        request.setAttribute(REQUEST_ID_ATTR, requestId);

        // RequestContextHolder — available in Spring components
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            attrs.setAttribute(REQUEST_ID_ATTR, requestId, RequestAttributes.SCOPE_REQUEST);
        }

        // Response header — so the caller can correlate
        response.setHeader(REQUEST_ID_HEADER, requestId);

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Clean up to prevent memory leaks in pooled threads
        MDC.remove(REQUEST_ID_MDC_KEY);
        REQUEST_ID_HOLDER.remove();
    }
}
