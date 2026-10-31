package site.defence.domain.user.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.defence.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            2000,
            "사용자를 찾을 수 없습니다."
    ),

    FCM_TOKEN_CONFLICT(
            HttpStatus.CONFLICT,
            2001,
            "FCM 토큰 등록이 충돌했습니다. 다시 시도해주세요."
    );

    private final HttpStatus status;
    private final int code;
    private final String message;
}