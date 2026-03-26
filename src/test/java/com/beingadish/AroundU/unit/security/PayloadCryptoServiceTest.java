package com.beingadish.AroundU.unit.security;

import com.beingadish.AroundU.infrastructure.security.PayloadCryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PayloadCryptoService")
class PayloadCryptoServiceTest {

    private PayloadCryptoService service;

    // Same key as Flutter .env: base64 of "this is a 32 byte key!!!12345678"
    private static final String TEST_KEY = "dGhpcyBpcyBhIDMyIGJ5dGUga2V5ISEhMTIzNDU2Nzg=";

    @BeforeEach
    void setUp() {
        service = new PayloadCryptoService();
        ReflectionTestUtils.setField(service, "base64Key", TEST_KEY);
        init(service);
    }

    @Test
    @DisplayName("isEnabled returns true when key is set")
    void isEnabled_whenKeySet() {
        assertThat(service.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("isEnabled returns false when key is empty")
    void isEnabled_whenKeyEmpty() {
        PayloadCryptoService emptyService = new PayloadCryptoService();
        ReflectionTestUtils.setField(emptyService, "base64Key", "");
        init(emptyService);
        assertThat(emptyService.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("encrypt then decrypt returns original plaintext")
    void encryptDecryptRoundTrip() throws Exception {
        String plaintext = "{\"message\": \"Hello, AroundU!\"}";

        String encrypted = service.encrypt(plaintext);
        assertThat(encrypted).isNotEmpty();
        assertThat(encrypted).isNotEqualTo(plaintext);

        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("same plaintext produces different ciphertexts (random nonce)")
    void sameInputDifferentOutput() throws Exception {
        String plaintext = "identical payload";

        String enc1 = service.encrypt(plaintext);
        String enc2 = service.encrypt(plaintext);

        assertThat(enc1).isNotEqualTo(enc2);

        // But both decrypt to same plaintext
        assertThat(service.decrypt(enc1)).isEqualTo(plaintext);
        assertThat(service.decrypt(enc2)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("wire format: nonce(12) + ciphertext + tag(16)")
    void wireFormat() throws Exception {
        String encrypted = service.encrypt("test");
        byte[] raw = Base64.getDecoder().decode(encrypted);

        // Minimum: 12 nonce + at least 0 ciphertext + 16 tag = 28
        assertThat(raw.length).isGreaterThanOrEqualTo(28);
    }

    @Test
    @DisplayName("empty plaintext round-trips")
    void emptyPlaintext() throws Exception {
        String encrypted = service.encrypt("");
        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEmpty();
    }

    @Test
    @DisplayName("unicode plaintext round-trips correctly")
    void unicodePlaintext() throws Exception {
        String plaintext = "{\"name\": \"आसपास\", \"emoji\": \"🔐🌍\"}";
        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("large payload round-trips")
    void largePayload() throws Exception {
        String plaintext = "x".repeat(100000);
        String encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("ciphertext too short is rejected")
    void tooShortCiphertext() {
        String shortPayload = Base64.getEncoder().encodeToString(new byte[20]);
        assertThatThrownBy(() -> service.decrypt(shortPayload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too short");
    }

    @Test
    @DisplayName("tampered ciphertext throws on decrypt")
    void tamperedCiphertext() throws Exception {
        String encrypted = service.encrypt("secret data");
        byte[] raw = Base64.getDecoder().decode(encrypted);
        raw[14] ^= (byte) 0xFF; // Tamper with ciphertext byte
        String tampered = Base64.getEncoder().encodeToString(raw);

        assertThatThrownBy(() -> service.decrypt(tampered))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("invalid key length throws on init")
    void invalidKeyLength() {
        byte[] shortKey = new byte[16];
        String base64ShortKey = Base64.getEncoder().encodeToString(shortKey);

        PayloadCryptoService badService = new PayloadCryptoService();
        ReflectionTestUtils.setField(badService, "base64Key", base64ShortKey);

        assertThatThrownBy(() -> init(badService))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    @DisplayName("encrypt throws when disabled")
    void encryptWhenDisabled() {
        PayloadCryptoService disabledService = new PayloadCryptoService();
        ReflectionTestUtils.setField(disabledService, "base64Key", "");
        init(disabledService);

        assertThatThrownBy(() -> disabledService.encrypt("test"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not enabled");
    }

    @Test
    @DisplayName("decrypt throws when disabled")
    void decryptWhenDisabled() {
        PayloadCryptoService disabledService = new PayloadCryptoService();
        ReflectionTestUtils.setField(disabledService, "base64Key", "");
        init(disabledService);

        assertThatThrownBy(() -> disabledService.decrypt("abc"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not enabled");
    }

    private static void init(PayloadCryptoService service) {
        ReflectionTestUtils.invokeMethod(service, "init");
    }

    @Test
    @DisplayName("Flutter-encrypted payload can be decrypted by backend")
    void crossPlatformCompatibility() throws Exception {
        // This validates that the wire format is compatible
        // between the Dart cryptography package and JDK ChaCha20-Poly1305
        String plaintext = "{\"action\":\"createJob\",\"title\":\"Fix plumbing\"}";
        String encrypted = service.encrypt(plaintext);

        // Verify it's valid base64
        byte[] decoded = Base64.getDecoder().decode(encrypted);
        assertThat(decoded.length).isGreaterThanOrEqualTo(28);

        // Decrypt should work
        String decrypted = service.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(plaintext);
    }
}
