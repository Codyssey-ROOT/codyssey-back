package site.defence.global.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {
    HttpStatus getStatus();

    int getCode();

    String getMessage();
}
