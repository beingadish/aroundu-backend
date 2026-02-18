package com.beingadish.AroundU.RateLimit;

import com.beingadish.AroundU.Exceptions.RateLimitExceededException;
import com.beingadish.AroundU.Security.UserPrincipal;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * AOP aspect that enforces per-user rate limiting on methods annotated with
 * {@link RateLimit}.
 * <p>
 * <b>How it works:</b>
 * <ol>
 * <li>Extracts user identity (userId or IP for anonymous requests)</li>
 * <li>Resolves or creates a token bucket in Redis keyed by
 * {@code rate-limit:{method}:{identity}}</li>
 * <li>Attempts to consume one token</li>
 * <li>If successful, proceeds with the target method</li>
 * <li>If tokens exhausted, throws {@link RateLimitExceededException} with
 * retry-after metadata</li>
 * </ol>
 * Admin users ({@code ROLE_ADMIN}) bypass rate limiting entirely.
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    private final ProxyManager<String> proxyManager;
    private final boolean enabled;

    public RateLimitAspect(
            ProxyManager<String> proxyManager,
            @Value("${rate-limit.enabled:true}") boolean enabled) {
        this.proxyManager = proxyManager;
        this.enabled = enabled;
    }

    @Around("@annotation(rateLimit)")
    public Object enforce(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        if (!enabled) {
            log.debug("Rate limiting is disabled");
            return joinPoint.proceed();
        }

        // Admin bypass
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAdmin(authentication)) {
            log.debug("Rate limit bypassed for admin user");
            return joinPoint.proceed();
        }

        String identity = resolveIdentity(authentication);
        String methodKey = resolveMethodKey(joinPoint);
        String bucketKey = "rate-limit:" + methodKey + ":" + identity;

        Supplier<BucketConfiguration> configSupplier = () -> buildConfiguration(rateLimit);

        ConsumptionProbe probe = proxyManager
                .builder()
                .build(bucketKey, configSupplier)
                .tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            log.debug("Rate limit OK: key={}, remaining={}", bucketKey, probe.getRemainingTokens());
            return joinPoint.proceed();
        }

        long retryAfterNanos = probe.getNanosToWaitForRefill();
        long retryAfterSeconds = Math.max(1, Duration.ofNanos(retryAfterNanos).toSeconds());
        log.warn("Rate limit exceeded: key={}, retryAfter={}s", bucketKey, retryAfterSeconds);
        throw new RateLimitExceededException(rateLimit.capacity(), retryAfterSeconds);
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
    }

    private String resolveIdentity(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return "user:" + principal.getId();
        }
        // Fallback to IP for anonymous/unauthenticated requests
        ServletRequestAttributes attrs
                = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            String ip = (forwarded != null && !forwarded.isBlank())
                    ? forwarded.split(",")[0].trim()
                    : request.getRemoteAddr();
            return "ip:" + ip;
        }
        return "ip:unknown";
    }

    private String resolveMethodKey(ProceedingJoinPoint joinPoint) {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        return sig.getDeclaringType().getSimpleName() + "." + sig.getName();
    }

    private BucketConfiguration buildConfiguration(RateLimit rateLimit) {
        return BucketConfiguration.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(rateLimit.capacity())
                        .refillGreedy(rateLimit.refillTokens(), Duration.ofMinutes(rateLimit.refillMinutes()))
                        .build())
                .build();
    }
}
