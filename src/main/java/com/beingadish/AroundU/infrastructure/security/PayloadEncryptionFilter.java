package com.beingadish.AroundU.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Servlet filter that handles ChaCha20-Poly1305 payload encryption.
 *
 * <ul>
 * <li><b>Request</b>: If header {@code X-Encrypted: true} is present, unwraps
 * {@code {"data":"<base64>"}} and decrypts before passing downstream.</li>
 * <li><b>Response</b>: Encrypts JSON response bodies and wraps as
 * {@code {"data":"<base64>"}} with {@code X-Encrypted: true} header.</li>
 * </ul>
 *
 * Excluded paths (actuator, docs, swagger) are passed through unmodified.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@RequiredArgsConstructor
public class PayloadEncryptionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(PayloadEncryptionFilter.class);
    private static final String ENCRYPTED_HEADER = "X-Encrypted";
    private static final String DATA_FIELD = "data";

    private static final Set<String> EXCLUDED_PATTERNS = Set.of(
            "/actuator/**",
            "/docs/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/api-docs/**"
    );

    private final PayloadCryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!cryptoService.isEnabled()) {
            return true;
        }
        String path = request.getRequestURI();
        return EXCLUDED_PATTERNS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        HttpServletRequest processedRequest = decryptRequest(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        filterChain.doFilter(processedRequest, responseWrapper);

        encryptResponse(responseWrapper);
        responseWrapper.copyBodyToResponse();
    }

    /**
     * If the request carries X-Encrypted: true, decrypt the body.
     */
    private HttpServletRequest decryptRequest(HttpServletRequest request) throws IOException {
        String encryptedHeader = request.getHeader(ENCRYPTED_HEADER);
        if (!"true".equalsIgnoreCase(encryptedHeader)) {
            return request;
        }

        byte[] rawBody = request.getInputStream().readAllBytes();
        if (rawBody.length == 0) {
            return request;
        }

        try {
            JsonNode node = objectMapper.readTree(rawBody);
            JsonNode dataNode = node.get(DATA_FIELD);
            if (dataNode == null || !dataNode.isTextual()) {
                log.warn("Encrypted request missing 'data' field");
                return request;
            }

            String decrypted = cryptoService.decrypt(dataNode.asText());
            return new CachedBodyHttpServletRequest(request, decrypted.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Failed to decrypt request payload", e);
            throw new IOException("Payload decryption failed", e);
        }
    }

    /**
     * Encrypt the response body if it is JSON.
     */
    private void encryptResponse(ContentCachingResponseWrapper responseWrapper) throws IOException {
        byte[] body = responseWrapper.getContentAsByteArray();
        if (body.length == 0) {
            return;
        }

        String contentType = responseWrapper.getContentType();
        if (contentType == null || !contentType.contains(MediaType.APPLICATION_JSON_VALUE)) {
            return;
        }

        // Don't encrypt error responses from this filter itself
        int status = responseWrapper.getStatus();
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return;
        }

        try {
            String plaintext = new String(body, StandardCharsets.UTF_8);
            String encrypted = cryptoService.encrypt(plaintext);

            ObjectNode wrapper = objectMapper.createObjectNode();
            wrapper.put(DATA_FIELD, encrypted);
            byte[] encryptedBody = objectMapper.writeValueAsBytes(wrapper);

            responseWrapper.resetBuffer();
            responseWrapper.getResponse().setContentLength(encryptedBody.length);
            responseWrapper.getResponse().getOutputStream().write(encryptedBody);
            responseWrapper.setHeader(ENCRYPTED_HEADER, "true");
        } catch (Exception e) {
            log.error("Failed to encrypt response payload", e);
            // Fall through with original unencrypted body
        }
    }
}
