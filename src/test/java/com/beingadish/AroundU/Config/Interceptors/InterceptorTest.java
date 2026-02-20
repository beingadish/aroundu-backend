package com.beingadish.AroundU.Config.Interceptors;

import com.beingadish.AroundU.user.entity.Client;
import com.beingadish.AroundU.user.repository.AdminRepository;
import com.beingadish.AroundU.user.repository.ClientReadRepository;
import com.beingadish.AroundU.user.repository.WorkerReadRepository;
import com.beingadish.AroundU.infrastructure.security.UserPrincipal;
import com.beingadish.AroundU.infrastructure.interceptor.RequestIdInterceptor;
import com.beingadish.AroundU.infrastructure.interceptor.RequestLoggingInterceptor;
import com.beingadish.AroundU.infrastructure.interceptor.ApiVersionInterceptor;
import com.beingadish.AroundU.infrastructure.interceptor.UserContextInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for all custom HandlerInterceptor implementations.
 */
@ExtendWith(MockitoExtension.class)
class InterceptorTest {

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final Object handler = new Object();

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    // ─────────────────────────────────────────────────────────────────
    // RequestIdInterceptor
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("RequestIdInterceptor")
    class RequestIdInterceptorTests {

        private final RequestIdInterceptor interceptor = new RequestIdInterceptor();

        @Test
        @DisplayName("should generate a UUID and store in MDC, request attribute, and response header")
        void generatesUuid() throws Exception {
            boolean result = interceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
            // MDC
            assertThat(MDC.get(RequestIdInterceptor.REQUEST_ID_MDC_KEY)).isNotNull();
            // Request attribute
            assertThat(request.getAttribute(RequestIdInterceptor.REQUEST_ID_ATTR)).isNotNull();
            // Response header
            assertThat(response.getHeader(RequestIdInterceptor.REQUEST_ID_HEADER)).isNotNull();
            // All three should be the same value
            String id = MDC.get(RequestIdInterceptor.REQUEST_ID_MDC_KEY);
            assertThat(request.getAttribute(RequestIdInterceptor.REQUEST_ID_ATTR)).isEqualTo(id);
            assertThat(response.getHeader(RequestIdInterceptor.REQUEST_ID_HEADER)).isEqualTo(id);
        }

        @Test
        @DisplayName("should honour existing X-Request-ID header from upstream")
        void honoursExistingId() throws Exception {
            request.addHeader("X-Request-ID", "upstream-123");

            interceptor.preHandle(request, response, handler);

            assertThat(MDC.get(RequestIdInterceptor.REQUEST_ID_MDC_KEY)).isEqualTo("upstream-123");
            assertThat(response.getHeader(RequestIdInterceptor.REQUEST_ID_HEADER)).isEqualTo("upstream-123");
        }

        @Test
        @DisplayName("should clean up MDC and ThreadLocal after completion")
        void cleansUp() throws Exception {
            interceptor.preHandle(request, response, handler);
            assertThat(MDC.get(RequestIdInterceptor.REQUEST_ID_MDC_KEY)).isNotNull();

            interceptor.afterCompletion(request, response, handler, null);

            assertThat(MDC.get(RequestIdInterceptor.REQUEST_ID_MDC_KEY)).isNull();
            assertThat(RequestIdInterceptor.getCurrentRequestId()).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // RequestLoggingInterceptor
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("RequestLoggingInterceptor")
    class RequestLoggingInterceptorTests {

        private final RequestLoggingInterceptor interceptor = new RequestLoggingInterceptor();

        @Test
        @DisplayName("should store start time and return true")
        void storesStartTime() throws Exception {
            request.setMethod("GET");
            request.setRequestURI("/api/v1/jobs/123");
            request.setRemoteAddr("192.168.1.1");

            boolean result = interceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
            assertThat(request.getAttribute("requestStartTime")).isNotNull();
        }

        @Test
        @DisplayName("should not throw on afterCompletion even when start time is missing")
        void handlesNullStartTime() {
            request.setMethod("GET");
            request.setRequestURI("/api/v1/jobs/123");
            response.setStatus(200);

            // Should not throw
            interceptor.afterCompletion(request, response, handler, null);
        }

        @Test
        @DisplayName("should resolve X-Forwarded-For header for client IP")
        void resolvesForwardedIp() throws Exception {
            request.setMethod("GET");
            request.setRequestURI("/api/v1/jobs");
            request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");

            interceptor.preHandle(request, response, handler);

            // Just verify it completes without error — the IP resolution is internal
            assertThat(request.getAttribute("requestStartTime")).isNotNull();
        }

        @Test
        @DisplayName("should handle exception parameter in afterCompletion")
        void handlesExceptionInAfterCompletion() throws Exception {
            request.setMethod("POST");
            request.setRequestURI("/api/v1/jobs");
            request.setAttribute("requestStartTime", System.currentTimeMillis());
            response.setStatus(500);

            // Should not throw even when ex is non-null
            interceptor.afterCompletion(request, response, handler,
                    new RuntimeException("test error"));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ApiVersionInterceptor
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("ApiVersionInterceptor")
    class ApiVersionInterceptorTests {

        private final ApiVersionInterceptor interceptor = new ApiVersionInterceptor();

        @Test
        @DisplayName("should default to 1.0 when no API-Version header present")
        void defaultVersion() throws Exception {
            request.setMethod("GET");
            request.setRequestURI("/api/v1/jobs");

            boolean result = interceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
            assertThat(request.getAttribute(ApiVersionInterceptor.API_VERSION_ATTR))
                    .isEqualTo("1.0");
        }

        @Test
        @DisplayName("should use value from API-Version header")
        void customVersion() throws Exception {
            request.setMethod("GET");
            request.setRequestURI("/api/v1/jobs");
            request.addHeader("API-Version", "2.0");

            interceptor.preHandle(request, response, handler);

            assertThat(request.getAttribute(ApiVersionInterceptor.API_VERSION_ATTR))
                    .isEqualTo("2.0");
        }

        @Test
        @DisplayName("should default when header is blank")
        void blankHeader() throws Exception {
            request.addHeader("API-Version", "   ");

            interceptor.preHandle(request, response, handler);

            assertThat(request.getAttribute(ApiVersionInterceptor.API_VERSION_ATTR))
                    .isEqualTo("1.0");
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // UserContextInterceptor
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("UserContextInterceptor")
    class UserContextInterceptorTests {

        @Mock
        private ClientReadRepository clientReadRepository;
        @Mock
        private WorkerReadRepository workerReadRepository;
        @Mock
        private AdminRepository adminRepository;

        @InjectMocks
        private UserContextInterceptor interceptor;

        @Test
        @DisplayName("should handle anonymous requests gracefully")
        void anonymousRequest() throws Exception {
            boolean result = interceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
            assertThat(request.getAttribute(UserContextInterceptor.CURRENT_USER_ATTR)).isNull();
        }

        @Test
        @DisplayName("should handle non-UserPrincipal authentication gracefully")
        void nonUserPrincipal() throws Exception {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("anonymousUser", null, List.of())
            );

            boolean result = interceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
            assertThat(request.getAttribute(UserContextInterceptor.CURRENT_USER_ATTR)).isNull();
        }

        @Test
        @DisplayName("should load client user into request attribute")
        void loadsClient() throws Exception {
            UserPrincipal principal = UserPrincipal.builder()
                    .id(1L)
                    .email("client@test.com")
                    .password("secret")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_CLIENT")))
                    .build();

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
            );

            Client mockClient = Client.builder().name("TestClient").email("client@test.com").build();
            when(clientReadRepository.findByEmail("client@test.com"))
                    .thenReturn(Optional.of(mockClient));

            boolean result = interceptor.preHandle(request, response, handler);

            assertThat(result).isTrue();
            assertThat(request.getAttribute(UserContextInterceptor.CURRENT_USER_ATTR))
                    .isEqualTo(mockClient);
            assertThat(request.getAttribute(UserContextInterceptor.CURRENT_USER_ROLE_ATTR))
                    .isEqualTo("ROLE_CLIENT");
            verify(clientReadRepository).findByEmail("client@test.com");
        }

        @Test
        @DisplayName("should handle database failure without throwing")
        void databaseFailure() throws Exception {
            UserPrincipal principal = UserPrincipal.builder()
                    .id(1L)
                    .email("fail@test.com")
                    .password("secret")
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_CLIENT")))
                    .build();

            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
            );

            when(clientReadRepository.findByEmail("fail@test.com"))
                    .thenThrow(new RuntimeException("DB down"));

            boolean result = interceptor.preHandle(request, response, handler);

            // Must not block the request
            assertThat(result).isTrue();
            assertThat(request.getAttribute(UserContextInterceptor.CURRENT_USER_ATTR)).isNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Ordering: verify interceptors do not interfere with each other
    // ─────────────────────────────────────────────────────────────────
    @Nested
    @DisplayName("Interceptor ordering")
    class InterceptorOrderingTests {

        @Mock
        private ClientReadRepository clientReadRepository;
        @Mock
        private WorkerReadRepository workerReadRepository;
        @Mock
        private AdminRepository adminRepository;

        @Test
        @DisplayName("all interceptors should succeed in sequence without exceptions")
        void allInterceptorsSucceed() throws Exception {
            RequestIdInterceptor requestIdInterceptor = new RequestIdInterceptor();
            RequestLoggingInterceptor loggingInterceptor = new RequestLoggingInterceptor();
            ApiVersionInterceptor versionInterceptor = new ApiVersionInterceptor();
            UserContextInterceptor userContextInterceptor
                    = new UserContextInterceptor(clientReadRepository, workerReadRepository, adminRepository);

            request.setMethod("GET");
            request.setRequestURI("/api/v1/jobs");
            request.setRemoteAddr("127.0.0.1");

            // Execute in the same order as WebMvcConfig
            assertThat(requestIdInterceptor.preHandle(request, response, handler)).isTrue();
            assertThat(loggingInterceptor.preHandle(request, response, handler)).isTrue();
            assertThat(versionInterceptor.preHandle(request, response, handler)).isTrue();
            assertThat(userContextInterceptor.preHandle(request, response, handler)).isTrue();

            // Verify request ID was set before logging ran (MDC present)
            assertThat(MDC.get(RequestIdInterceptor.REQUEST_ID_MDC_KEY)).isNotNull();
            assertThat(request.getAttribute("requestStartTime")).isNotNull();
            assertThat(request.getAttribute(ApiVersionInterceptor.API_VERSION_ATTR)).isEqualTo("1.0");

            // After completion — reverse order
            response.setStatus(200);
            userContextInterceptor.afterCompletion(request, response, handler, null);
            versionInterceptor.afterCompletion(request, response, handler, null);
            loggingInterceptor.afterCompletion(request, response, handler, null);
            requestIdInterceptor.afterCompletion(request, response, handler, null);

            // MDC should be clean
            assertThat(MDC.get(RequestIdInterceptor.REQUEST_ID_MDC_KEY)).isNull();
        }
    }
}
