package site.defence.global.security.dto;

public record TokenReissueResult(
        Long userId,
        String refreshToken
) {

    private static final String DELIMITER = ":";

    public String toRedisValue() {

        return userId + DELIMITER + refreshToken;
    }

    public static TokenReissueResult from(String redisValue) {

        String[] values = redisValue.split(DELIMITER);

        return new TokenReissueResult(Long.valueOf(values[0]), values[1]);
    }
}