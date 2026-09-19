package com.tanvir.core.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Firebase Admin SDK bootstrap.
 *
 * Service-account credentials are supplied from GitHub Secrets via environment variables.
 * Resolution order:
 *
 *   1. FIREBASE_CREDENTIALS_JSON  - the raw service-account JSON
 *   2. FIREBASE_CREDENTIALS_FILE  - path to a JSON file on disk (classpath fallback for local dev)
 *
 * See README.md -> "Configuration".
 */
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.json:}")
    private String firebaseCredentialsJson;

    @Value("${firebase.credentials.file:}")
    private String firebaseCredentialsPath;

    @Value("${firebase.database.url}")
    private String firebaseDatabaseUrl;

    @PostConstruct
    public void initialize() {
        try (InputStream credentialsStream = openCredentials()) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialsStream);
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setDatabaseUrl(firebaseDatabaseUrl)
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream openCredentials() throws IOException {
        if (firebaseCredentialsJson != null && !firebaseCredentialsJson.isBlank()) {
            return new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8));
        }
        if (firebaseCredentialsPath == null || firebaseCredentialsPath.isBlank()) {
            throw new IOException("Firebase credentials not configured. Set FIREBASE_CREDENTIALS_JSON "
                + "or FIREBASE_CREDENTIALS_FILE (see README.md -> Configuration).");
        }
        Resource file = new FileSystemResource(firebaseCredentialsPath);
        Resource resource = file.exists() ? file : new ClassPathResource(firebaseCredentialsPath);
        return resource.getInputStream();
    }
}
