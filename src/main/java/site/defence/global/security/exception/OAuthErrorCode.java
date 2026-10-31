package site.defence.global.security.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.defence.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum OAuthErrorCode implements ErrorCode {

    OAUTH2_USER_PROCESSING_FAILED(
            HttpStatus.UNAUTHORIZED,
            40101,
            "OAuth2 사용자 인증 처리에 실패했습니다."
    );

    private final HttpStatus status;
    private final int code;
    private final String message;
}
