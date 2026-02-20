package com.beingadish.AroundU.infrastructure.config;

import com.beingadish.AroundU.infrastructure.interceptor.ApiVersionInterceptor;
import com.beingadish.AroundU.infrastructure.interceptor.RequestIdInterceptor;
import com.beingadish.AroundU.infrastructure.interceptor.RequestLoggingInterceptor;
import com.beingadish.AroundU.infrastructure.interceptor.UserContextInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers all custom
 * {@link org.springframework.web.servlet.HandlerInterceptor} implementations
 * and configures their URL path patterns.
 *
 * <h3>Interceptor execution order (lowest → highest value = first → last):</h3>
 * <ol>
 * <li><b>RequestIdInterceptor</b> (order 1) — must run first so every
 * subsequent log statement includes the request ID.</li>
 * <li><b>RequestLoggingInterceptor</b> (order 2) — logs method, URI, IP and
 * elapsed time.</li>
 * <li><b>ApiVersionInterceptor</b> (order 3) — resolves the API-Version
 * header.</li>
 * <li><b>UserContextInterceptor</b> (order 4) — loads authenticated user from
 * the database.</li>
 * </ol>
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestIdInterceptor requestIdInterceptor;
    private final RequestLoggingInterceptor requestLoggingInterceptor;
    private final ApiVersionInterceptor apiVersionInterceptor;
    private final UserContextInterceptor userContextInterceptor;

    /**
     * Common paths that should be excluded from logging / version interceptors.
     */
    private static final String[] LOGGING_EXCLUDE_PATHS = {
        "/actuator/health",
        "/actuator/metrics",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs/**"
    };

    /**
     * Paths excluded from the user-context interceptor because the user is not
     * yet authenticated through these endpoints.
     */
    private static final String[] USER_CONTEXT_EXCLUDE_PATHS = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/api/v1/client/register",
        "/api/v1/worker/register"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        // 1. Request ID — runs first, applies to ALL /api/** paths
        registry.addInterceptor(requestIdInterceptor)
                .addPathPatterns("/api/**")
                .order(1);

        // 2. Request logging — /api/** minus health & metrics & swagger
        registry.addInterceptor(requestLoggingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(LOGGING_EXCLUDE_PATHS)
                .order(2);

        // 3. API version — /api/** minus health & metrics & swagger
        registry.addInterceptor(apiVersionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(LOGGING_EXCLUDE_PATHS)
                .order(3);

        // 4. User context — /api/** minus auth endpoints
        registry.addInterceptor(userContextInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(USER_CONTEXT_EXCLUDE_PATHS)
                .order(4);
    }
}
