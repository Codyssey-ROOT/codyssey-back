package site.defence.global.security.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.defence.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum SecurityErrorCode implements ErrorCode {

    UNAUTHENTICATED_USER(
            HttpStatus.UNAUTHORIZED,
            40104,
            "인증이 필요합니다."
    ),

    ACCESS_DENIED(
            HttpStatus.FORBIDDEN,
            40301,
            "접근 권한이 없습니다."
    );

    private final HttpStatus status;
    private final int code;
    private final String message;
}
