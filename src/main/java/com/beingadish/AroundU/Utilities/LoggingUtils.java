package com.beingadish.AroundU.Utilities;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Utility class for logging-related operations. Provides methods to sanitize
 * sensitive data, format request bodies, and identify sensitive paths.
 */
public final class LoggingUtils {

    private LoggingUtils() {
        // Utility class — no instantiation
    }

    private static final int MAX_BODY_LENGTH = 500;

    /**
     * Regex that matches common sensitive key-value patterns in JSON or form
     * data. Covers: password, token, apiKey, api_key, secret, authorization,
     * access_token, refresh_token, credit_card
     */
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(\"(?:password|token|apiKey|api_key|secret|authorization|access_token|refresh_token|credit_card)\"\\s*:\\s*\")(.*?)(\")",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> SENSITIVE_PATHS = Set.of(
            "/login",
            "/register",
            "/password-reset",
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/password-reset"
    );

    /**
     * Removes passwords, tokens, API keys and other sensitive values from a log
     * message. Replaces the value portion with {@code [REDACTED]}.
     *
     * @param message the raw message
     * @return sanitized message with sensitive values masked
     */
    public static String sanitizeMessage(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }
        return SENSITIVE_PATTERN.matcher(message).replaceAll("$1[REDACTED]$3");
    }

    /**
     * Truncates a request body to a maximum length for logging. Appends an
     * ellipsis indicator when truncated.
     *
     * @param body the raw request body
     * @return the formatted (potentially truncated) body
     */
    public static String formatRequestBody(String body) {
        if (body == null || body.isEmpty()) {
            return "[empty]";
        }
        String sanitized = sanitizeMessage(body);
        if (sanitized.length() <= MAX_BODY_LENGTH) {
            return sanitized;
        }
        return sanitized.substring(0, MAX_BODY_LENGTH) + "... [truncated, total=" + body.length() + " chars]";
    }

    /**
     * Checks whether the given URI path is considered sensitive and should not
     * have its body or parameters logged.
     *
     * @param path the request URI path
     * @return {@code true} if the path is sensitive
     */
    public static boolean isPathSensitive(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }
        String normalizedPath = path.toLowerCase().split("\\?")[0]; // strip query params
        return SENSITIVE_PATHS.stream().anyMatch(normalizedPath::endsWith);
    }
}
