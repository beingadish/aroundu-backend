package com.beingadish.AroundU.infrastructure.security;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * ChaCha20-Poly1305 payload encryption/decryption service. Wire format: Base64(
 * nonce[12] || ciphertext || tag[16] )
 */
@Service
public class PayloadCryptoService {

    private static final Logger log = LoggerFactory.getLogger(PayloadCryptoService.class);
    private static final String ALGORITHM = "ChaCha20-Poly1305";
    private static final int NONCE_LENGTH = 12;

    @Value("${app.encryption.key:}")
    private String base64Key;

    private SecretKeySpec secretKey;
    private boolean enabled;

    @PostConstruct
    void init() {
        if (base64Key == null || base64Key.isBlank()) {
            log.warn("Payload encryption key not configured – encryption disabled");
            enabled = false;
            return;
        }
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "Payload encryption key must be exactly 32 bytes (256-bit), got " + keyBytes.length);
        }
        secretKey = new SecretKeySpec(keyBytes, "ChaCha20");
        enabled = true;
        log.info("Payload encryption enabled (ChaCha20-Poly1305)");
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Encrypt plaintext to Base64( nonce || ciphertext || tag ).
     */
    public String encrypt(String plaintext) throws Exception {
        if (!enabled) {
            throw new IllegalStateException("Encryption is not enabled");
        }

        byte[] nonce = new byte[NONCE_LENGTH];
        SecureRandom.getInstanceStrong().nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(nonce));

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        // nonce || ciphertext (includes appended tag)
        ByteBuffer buffer = ByteBuffer.allocate(nonce.length + ciphertext.length);
        buffer.put(nonce);
        buffer.put(ciphertext);

        return Base64.getEncoder().encodeToString(buffer.array());
    }

    /**
     * Decrypt Base64( nonce || ciphertext || tag ) to plaintext.
     */
    public String decrypt(String base64Ciphertext) throws Exception {
        if (!enabled) {
            throw new IllegalStateException("Encryption is not enabled");
        }

        byte[] combined = Base64.getDecoder().decode(base64Ciphertext);
        if (combined.length < NONCE_LENGTH + 16) {
            throw new IllegalArgumentException("Ciphertext too short");
        }

        byte[] nonce = new byte[NONCE_LENGTH];
        System.arraycopy(combined, 0, nonce, 0, NONCE_LENGTH);

        byte[] ciphertext = new byte[combined.length - NONCE_LENGTH];
        System.arraycopy(combined, NONCE_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(nonce));

        byte[] plainBytes = cipher.doFinal(ciphertext);
        return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
