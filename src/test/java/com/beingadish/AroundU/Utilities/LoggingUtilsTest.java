package com.beingadish.AroundU.Utilities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LoggingUtils}.
 */
class LoggingUtilsTest {

    // ─── sanitizeMessage ─────────────────────────────────────────────
    @Test
    @DisplayName("should redact password values in JSON")
    void sanitizesPassword() {
        String input = "{\"email\":\"a@b.com\",\"password\":\"s3cret!\"}";
        String result = LoggingUtils.sanitizeMessage(input);
        assertThat(result).contains("[REDACTED]").doesNotContain("s3cret!");
    }

    @Test
    @DisplayName("should redact token values in JSON")
    void sanitizesToken() {
        String input = "{\"token\":\"eyJhbGciOiJIUz\"}";
        String result = LoggingUtils.sanitizeMessage(input);
        assertThat(result).contains("[REDACTED]").doesNotContain("eyJhbGciOiJIUz");
    }

    @Test
    @DisplayName("should redact apiKey values in JSON")
    void sanitizesApiKey() {
        String input = "{\"apiKey\":\"ak_live_abc123\"}";
        String result = LoggingUtils.sanitizeMessage(input);
        assertThat(result).contains("[REDACTED]").doesNotContain("ak_live_abc123");
    }

    @Test
    @DisplayName("should return null for null input")
    void sanitizeNull() {
        assertThat(LoggingUtils.sanitizeMessage(null)).isNull();
    }

    @Test
    @DisplayName("should return empty string for empty input")
    void sanitizeEmpty() {
        assertThat(LoggingUtils.sanitizeMessage("")).isEmpty();
    }

    @Test
    @DisplayName("should leave non-sensitive data untouched")
    void sanitizeNonSensitive() {
        String input = "{\"name\":\"John\",\"email\":\"john@test.com\"}";
        assertThat(LoggingUtils.sanitizeMessage(input)).isEqualTo(input);
    }

    // ─── formatRequestBody ───────────────────────────────────────────
    @Test
    @DisplayName("should return [empty] for null body")
    void formatNull() {
        assertThat(LoggingUtils.formatRequestBody(null)).isEqualTo("[empty]");
    }

    @Test
    @DisplayName("should return [empty] for empty body")
    void formatEmpty() {
        assertThat(LoggingUtils.formatRequestBody("")).isEqualTo("[empty]");
    }

    @Test
    @DisplayName("should return body as-is when under 500 chars")
    void formatShortBody() {
        String body = "{\"name\":\"test\"}";
        assertThat(LoggingUtils.formatRequestBody(body)).isEqualTo(body);
    }

    @Test
    @DisplayName("should truncate body exceeding 500 chars")
    void formatLongBody() {
        String body = "a".repeat(600);
        String result = LoggingUtils.formatRequestBody(body);
        assertThat(result).hasSize(500 + "... [truncated, total=600 chars]".length());
        assertThat(result).contains("[truncated");
    }

    @Test
    @DisplayName("should sanitize sensitive data in body before formatting")
    void formatSanitized() {
        String body = "{\"password\":\"secret123\",\"data\":\"ok\"}";
        String result = LoggingUtils.formatRequestBody(body);
        assertThat(result).contains("[REDACTED]").doesNotContain("secret123");
    }

    // ─── isPathSensitive ─────────────────────────────────────────────
    @Test
    @DisplayName("should identify /login as sensitive")
    void loginSensitive() {
        assertThat(LoggingUtils.isPathSensitive("/api/v1/auth/login")).isTrue();
    }

    @Test
    @DisplayName("should identify /register as sensitive")
    void registerSensitive() {
        assertThat(LoggingUtils.isPathSensitive("/api/v1/auth/register")).isTrue();
    }

    @Test
    @DisplayName("should identify /password-reset as sensitive")
    void passwordResetSensitive() {
        assertThat(LoggingUtils.isPathSensitive("/api/v1/auth/password-reset")).isTrue();
    }

    @Test
    @DisplayName("should not flag /api/v1/jobs as sensitive")
    void jobsNotSensitive() {
        assertThat(LoggingUtils.isPathSensitive("/api/v1/jobs")).isFalse();
    }

    @Test
    @DisplayName("should return false for null path")
    void nullPath() {
        assertThat(LoggingUtils.isPathSensitive(null)).isFalse();
    }

    @Test
    @DisplayName("should strip query params before checking")
    void stripsQueryParams() {
        assertThat(LoggingUtils.isPathSensitive("/api/v1/auth/login?redirect=/home")).isTrue();
    }
}
