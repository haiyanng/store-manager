package com.storeapi.setup.service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class DatabasePasswordCryptoService {
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;

    private final LocalSecretKeyService keyService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DatabasePasswordCryptoService(LocalSecretKeyService keyService) {
        this.keyService = keyService;
    }

    public String encrypt(String plainPassword) {
        if (plainPassword == null) {
            plainPassword = "";
        }
        try {
            SecretKey key = keyService.loadOrCreateKey();
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] cipherText = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot encrypt database password", ex);
        }
    }

    public String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isBlank()) {
            return "";
        }
        try {
            SecretKey key = keyService.loadOrCreateKey();
            byte[] bytes = Base64.getDecoder().decode(encryptedPassword);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot decrypt database password", ex);
        }
    }
}
