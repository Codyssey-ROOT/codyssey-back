package site.defence.global.security.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.defence.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum TokenErrorCode implements ErrorCode {

    REFRESH_TOKEN_COOKIE_NOT_FOUND(
            HttpStatus.UNAUTHORIZED,
            40102,
            "Refresh Token Cookie가 존재하지 않습니다."
    ),

    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            40103,
            "유효하지 않은 Refresh Token입니다."
    );

    private final HttpStatus status;
    private final int code;
    private final String message;
}
