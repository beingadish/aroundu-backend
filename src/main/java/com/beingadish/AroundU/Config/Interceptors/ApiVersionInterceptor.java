package com.beingadish.AroundU.Config.Interceptors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor that extracts the {@code API-Version} header from each request
 * and stores it as a request attribute for downstream controller routing.
 * <p>
 * If the header is absent the default version "1.0" is applied. This enables
 * future API versioning support without URL-path changes.
 */
@Slf4j
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {

    public static final String API_VERSION_ATTR = "apiVersion";
    public static final String API_VERSION_HEADER = "API-Version";
    public static final String DEFAULT_VERSION = "1.0";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String version = request.getHeader(API_VERSION_HEADER);

        if (version == null || version.isBlank()) {
            version = DEFAULT_VERSION;
        }

        request.setAttribute(API_VERSION_ATTR, version);
        log.debug("API version resolved: {} for {} {}", version, request.getMethod(), request.getRequestURI());

        return true;
    }
}
