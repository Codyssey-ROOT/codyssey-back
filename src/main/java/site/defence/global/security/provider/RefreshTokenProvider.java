package site.defence.global.security.provider;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class RefreshTokenProvider {

    private static final String HASH_ALGORITHM = "SHA-256";

    public String generateRefreshToken() {

        return UUID.randomUUID().toString();
    }

    public String hashRefreshToken(String refreshToken) {

        try {
            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashedRefreshToken =
                    messageDigest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hashedRefreshToken);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
