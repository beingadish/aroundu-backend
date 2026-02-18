package com.beingadish.AroundU.Exceptions;

import com.beingadish.AroundU.DTO.Common.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that the GlobalExceptionHandler correctly handles
 * {@link RateLimitExceededException} with proper status, headers, and body.
 */
@DisplayName("Rate Limit Exception Handler")
class RateLimitExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("should return 429 status code")
    void returns429Status() {
        RateLimitExceededException ex = new RateLimitExceededException(5, 45);

        ResponseEntity<ApiResponse<?>> response = handler.handleRateLimitExceeded(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("should include X-RateLimit-Limit header")
    void includesRateLimitHeader() {
        RateLimitExceededException ex = new RateLimitExceededException(5, 45);

        ResponseEntity<ApiResponse<?>> response = handler.handleRateLimitExceeded(ex);

        assertThat(response.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("5");
    }

    @Test
    @DisplayName("should include X-RateLimit-Remaining header as 0")
    void includesRemainingHeader() {
        RateLimitExceededException ex = new RateLimitExceededException(5, 45);

        ResponseEntity<ApiResponse<?>> response = handler.handleRateLimitExceeded(ex);

        assertThat(response.getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
    }

    @Test
    @DisplayName("should include Retry-After header in seconds")
    void includesRetryAfterHeader() {
        RateLimitExceededException ex = new RateLimitExceededException(5, 45);

        ResponseEntity<ApiResponse<?>> response = handler.handleRateLimitExceeded(ex);

        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("45");
    }

    @Test
    @DisplayName("should include helpful error message in body")
    void includesErrorMessage() {
        RateLimitExceededException ex = new RateLimitExceededException(5, 45);

        ResponseEntity<ApiResponse<?>> response = handler.handleRateLimitExceeded(ex);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).contains("45 seconds");
    }

    @Test
    @DisplayName("should handle different limit and retry values")
    void handlesDifferentValues() {
        RateLimitExceededException ex = new RateLimitExceededException(20, 120);

        ResponseEntity<ApiResponse<?>> response = handler.handleRateLimitExceeded(ex);

        assertThat(response.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo("20");
        assertThat(response.getHeaders().getFirst("Retry-After")).isEqualTo("120");
        assertThat(response.getBody().getMessage()).contains("120 seconds");
    }
}
