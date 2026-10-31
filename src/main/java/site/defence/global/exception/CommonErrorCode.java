package site.defence.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum CommonErrorCode implements ErrorCode {
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, 1000, "잘못된 입력값입니다."),
    INVALID_TIME_ZONE(HttpStatus.BAD_REQUEST, 1001, "올바르지 않은 TimeZone 값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, 1002, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final int code;
    private final String message;

    CommonErrorCode(HttpStatus status, int code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
