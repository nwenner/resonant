package com.wenroe.resonant.service.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting AWS credentials.
 * NOT USED in role-only setup, but kept for future flexibility.
 */
@Service
public class CredentialEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private final SecretKey secretKey;
    private final boolean enabled;

    public CredentialEncryptionService(@Value("${resonant.encryption.key}") String base64Key) {
        // Check if encryption is actually needed (role-only setup doesn't need it)
        if (base64Key == null || base64Key.equals("not-needed-for-roles-only")) {
            this.secretKey = null;
            this.enabled = false;
        } else {
            try {
                byte[] decodedKey = Base64.getDecoder().decode(base64Key);
                this.secretKey = new SecretKeySpec(decodedKey, "AES");
                this.enabled = true;
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid encryption key format. Must be valid Base64.", e);
            }
        }
    }

    public String encrypt(String plaintext) {
        if (!enabled) {
            throw new UnsupportedOperationException("Encryption is disabled for role-only setup");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        }
    }

    public String decrypt(String encryptedData) {
        if (!enabled) {
            throw new UnsupportedOperationException("Encryption is disabled for role-only setup");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedData);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext);

        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt credentials", e);
        }
    }

    public static String generateNewKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey key = keyGenerator.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static void main(String[] args) throws Exception {
        String key = generateNewKey();
        System.out.println("Generated encryption key (store in RESONANT_ENCRYPTION_KEY env var):");
        System.out.println(key);
    }
}