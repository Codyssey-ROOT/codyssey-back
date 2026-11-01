package site.defence.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Configuration
@ConditionalOnProperty(
        prefix = "app.firebase",
        name = "enabled",
        havingValue = "true"
)
public class FirebaseConfig {

    @Bean(destroyMethod = "delete")
    public FirebaseApp firebaseApp(
            @Value("${app.firebase.service-account-base64}")
            String serviceAccountBase64
    ) throws IOException {

        byte[] serviceAccountJson;

        try {
            serviceAccountJson = Base64.getDecoder()
                    .decode(serviceAccountBase64);

        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "Firebase 서비스 계정 Base64 값이 올바르지 않습니다.",
                    e
            );
        }

        GoogleCredentials credentials =
                GoogleCredentials.fromStream(
                        new ByteArrayInputStream(
                                serviceAccountJson
                        )
                );

        FirebaseOptions options =
                FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

        return FirebaseApp.initializeApp(
                options,
                "deepfence"
        );
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(
            FirebaseApp firebaseApp
    ) {
        return FirebaseMessaging.getInstance(
                firebaseApp
        );
    }
}