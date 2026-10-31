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
    );

    private final HttpStatus status;
    private final int code;
    private final String message;
}
