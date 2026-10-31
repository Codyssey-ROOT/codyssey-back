package site.defence.global.security.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import site.defence.global.security.dto.TokenReissueResult;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRedisRepository {

    private static final String REFRESH_TOKEN_KEY_PREFIX = "auth:refresh-token:";
    private static final String REISSUE_RESULT_KEY_PREFIX = "auth:refresh-token:reissue-result:";
    private static final long REISSUE_RESULT_TTL_SECONDS = 10;

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.refresh-token-ttl}")
    private long refreshTokenTtl;

    public void saveRefreshToken(String hashedRefreshToken, Long userId) {

        stringRedisTemplate
                .opsForValue()
                .set(createRefreshTokenKey(hashedRefreshToken), String.valueOf(userId),
                        Duration.ofSeconds(refreshTokenTtl));
    }

    public Optional<Long> findUserIdByRefreshToken(String hashedRefreshToken) {

        String userId = stringRedisTemplate
                .opsForValue()
                .get(createRefreshTokenKey(hashedRefreshToken));

        if (userId == null) {

            return Optional.empty();
        }

        return Optional.of(Long.valueOf(userId));
    }

    public Optional<Long> findUserIdAndDeleteRefreshToken(String hashedRefreshToken) {

        String userId = stringRedisTemplate
                .opsForValue()
                .getAndDelete(createRefreshTokenKey(hashedRefreshToken));

        if (userId == null) {

            return Optional.empty();
        }

        return Optional.of(Long.valueOf(userId));
    }

    public void deleteRefreshToken(String hashedRefreshToken) {

        stringRedisTemplate.delete(createRefreshTokenKey(hashedRefreshToken));
    }

    public void saveReissueResult(String hashedRefreshToken,
                                  TokenReissueResult tokenReissueResult) {

        stringRedisTemplate
                .opsForValue()
                .set(
                        createReissueResultKey(hashedRefreshToken),
                        tokenReissueResult.toRedisValue(),
                        Duration.ofSeconds(REISSUE_RESULT_TTL_SECONDS)
                );
    }

    public Optional<TokenReissueResult> findReissueResult(String hashedRefreshToken) {

        String redisValue = stringRedisTemplate
                .opsForValue()
                .get(createReissueResultKey(hashedRefreshToken));

        if (redisValue == null) {

            return Optional.empty();
        }

        return Optional.of(TokenReissueResult.from(redisValue));
    }

    private String createRefreshTokenKey(String hashedRefreshToken) {

        return REFRESH_TOKEN_KEY_PREFIX + hashedRefreshToken;
    }

    private String createReissueResultKey(String hashedRefreshToken) {

        return REISSUE_RESULT_KEY_PREFIX + hashedRefreshToken;
    }
}
