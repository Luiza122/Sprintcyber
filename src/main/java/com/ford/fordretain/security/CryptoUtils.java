package com.ford.fordretain.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Slf4j
@Component
public class CryptoUtils {
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 256;
    private static final int PBKDF2_ITERATIONS = 65536;

    @Value("${app.crypto.secret}") private String secret;
    @Value("${app.crypto.salt}") private String salt;

    private SecretKeySpec deriveKey() {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(secret.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), PBKDF2_ITERATIONS, KEY_LENGTH);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao derivar chave de criptografia", e);
        }
    }

    public String encrypt(String data) {
        if (data == null) return null;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv).put(ciphertext);
            return "v1:" + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Erro ao criptografar dado sensível");
            throw new RuntimeException("Erro de criptografia");
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null) return null;
        try {
            // Dados anteriores à criptografia podem existir no Oracle. Apenas
            // telefones legados plausíveis são aceitos em texto; uma tag GCM
            // inválida jamais é devolvida como se fosse um valor legítimo.
            if (!encrypted.startsWith("v1:") && encrypted.matches("\\d{10,11}")) {
                return encrypted;
            }
            byte[] decoded = Base64.getDecoder().decode(encrypted.startsWith("v1:")
                    ? encrypted.substring(3) : encrypted);
            if (decoded.length < GCM_IV_LENGTH + 16) {
                throw new IllegalArgumentException("Dado criptografado inválido");
            }
            ByteBuffer buffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Falha de integridade ao descriptografar dado sensível");
            throw new IllegalStateException("Dado sensível inválido ou corrompido", e);
        }
    }

    public String anonymize(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
