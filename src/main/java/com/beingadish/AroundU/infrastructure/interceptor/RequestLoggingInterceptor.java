package com.beingadish.AroundU.infrastructure.interceptor;

import com.beingadish.AroundU.common.util.LoggingUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that logs each incoming HTTP request and outgoing response.
 * <p>
 * On request arrival it records the HTTP method, URI, and source IP address.
 * After the response is committed it calculates the elapsed time and logs the
 * status code together with the duration.
 * <p>
 * Format example: {@code GET /api/jobs/123 from 192.168.1.1 - 200 OK (145ms)}
 */
@Slf4j
@Component
public class RequestLoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME_ATTR = "requestStartTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis());

        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String clientIp = resolveClientIp(request);

        String fullUri = queryString != null ? uri + "?" + queryString : uri;

        if (LoggingUtils.isPathSensitive(uri)) {
            log.info(">> {} {} from {} [sensitive path - body not logged]", method, fullUri, clientIp);
        } else {
            log.info(">> {} {} from {}", method, fullUri, clientIp);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Long startTime = (Long) request.getAttribute(START_TIME_ATTR);
        long elapsed = startTime != null ? System.currentTimeMillis() - startTime : -1;

        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        String statusText = resolveStatusText(status);
        String clientIp = resolveClientIp(request);

        log.info("{} {} from {} - {} {} ({}ms)", method, uri, clientIp, status, statusText, elapsed);

        if (ex != null) {
            log.error("Request {} {} completed with exception", method, uri, ex);
        }
    }

    /**
     * Resolves the real client IP, respecting common proxy headers.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    /**
     * Maps the most common HTTP status codes to a human-readable text.
     */
    private String resolveStatusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            default -> "";
        };
    }
}
