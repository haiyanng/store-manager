package com.storeapi.setup.service;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class LocalSecretKeyService {
    private final SetupPathService paths;

    public LocalSecretKeyService() {
        this(new SetupPathService());
    }

    public LocalSecretKeyService(SetupPathService paths) {
        this.paths = paths;
    }

    public SecretKey loadOrCreateKey() {
        try {
            if (Files.exists(paths.keyFile())) {
                return readKey();
            }
            SecretKey key = generateKey();
            saveKey(key);
            return key;
        } catch (IOException ex) {
            throw new IllegalStateException("Cannot load local secret key", ex);
        }
    }

    private SecretKey readKey() throws IOException {
        String encoded = Files.readString(paths.keyFile(), StandardCharsets.UTF_8).trim();
        if (encoded.isBlank()) {
            throw new IllegalStateException("Local secret key file is empty");
        }
        byte[] bytes = Base64.getDecoder().decode(encoded);
        return new javax.crypto.spec.SecretKeySpec(bytes, "AES");
    }

    private void saveKey(SecretKey key) throws IOException {
        Files.createDirectories(paths.configDir());
        Files.writeString(paths.keyFile(), Base64.getEncoder().encodeToString(key.getEncoded()), StandardCharsets.UTF_8);
    }

    private SecretKey generateKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance("AES");
            generator.init(128);
            return generator.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("AES key generator is unavailable", ex);
        }
    }
}
