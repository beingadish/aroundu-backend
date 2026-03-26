package com.beingadish.AroundU.unit.security;

import com.beingadish.AroundU.infrastructure.security.PayloadCryptoService;
import com.beingadish.AroundU.infrastructure.security.PayloadEncryptionFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PayloadEncryptionFilter")
class PayloadEncryptionFilterTest {

    private TestPayloadEncryptionFilter filter;
    private PayloadCryptoService cryptoService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FilterChain filterChain;

    private static final String TEST_KEY = "dGhpcyBpcyBhIDMyIGJ5dGUga2V5ISEhMTIzNDU2Nzg=";

    @BeforeEach
    void setUp() {
        cryptoService = new PayloadCryptoService();
        ReflectionTestUtils.setField(cryptoService, "base64Key", TEST_KEY);
        init(cryptoService);

        filter = new TestPayloadEncryptionFilter(cryptoService, objectMapper);
    }

    @Test
    @DisplayName("excluded path /actuator/health passes through")
    void excludedActuatorPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(filter.shouldSkip(request)).isTrue();
    }

    @Test
    @DisplayName("excluded path /docs passes through")
    void excludedDocsPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/docs/index.html");
        assertThat(filter.shouldSkip(request)).isTrue();
    }

    @Test
    @DisplayName("excluded path /swagger-ui passes through")
    void excludedSwaggerPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/swagger-ui/index.html");
        assertThat(filter.shouldSkip(request)).isTrue();
    }

    @Test
    @DisplayName("excluded path /v3/api-docs passes through")
    void excludedApiDocsPath() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v3/api-docs/openapi.json");
        assertThat(filter.shouldSkip(request)).isTrue();
    }

    @Test
    @DisplayName("API path is NOT excluded")
    void apiPathNotExcluded() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        assertThat(filter.shouldSkip(request)).isFalse();
    }

    @Test
    @DisplayName("filter is disabled when crypto key is not set")
    void filterDisabledWhenNoKey() throws Exception {
        PayloadCryptoService disabledService = new PayloadCryptoService();
        ReflectionTestUtils.setField(disabledService, "base64Key", "");
        init(disabledService);

        TestPayloadEncryptionFilter disabledFilter = new TestPayloadEncryptionFilter(disabledService, objectMapper);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");

        assertThat(disabledFilter.shouldSkip(request)).isTrue();
    }

    @Test
    @DisplayName("encrypted request is decrypted and passed to filter chain")
    void decryptRequest() throws Exception {
        String originalBody = "{\"username\":\"test\",\"password\":\"secret\"}";
        String encrypted = cryptoService.encrypt(originalBody);
        String wrappedBody = objectMapper.writeValueAsString(
                java.util.Map.of("data", encrypted)
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setContent(wrappedBody.getBytes());
        request.addHeader("X-Encrypted", "true");

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    @Test
    @DisplayName("request without X-Encrypted header passes through unchanged")
    void unencryptedRequestPassesThrough() throws Exception {
        String body = "{\"username\":\"test\"}";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setContentType("application/json");
        request.setContent(body.getBytes());

        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(any(), any());
    }

    private static void init(PayloadCryptoService service) {
        ReflectionTestUtils.invokeMethod(service, "init");
    }

    private static final class TestPayloadEncryptionFilter extends PayloadEncryptionFilter {

        private TestPayloadEncryptionFilter(PayloadCryptoService cryptoService, ObjectMapper objectMapper) {
            super(cryptoService, objectMapper);
        }

        private boolean shouldSkip(MockHttpServletRequest request) {
            return super.shouldNotFilter(request);
        }
    }
}
