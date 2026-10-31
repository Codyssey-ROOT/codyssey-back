package site.defence.global.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import site.defence.global.exception.CommonErrorCode;
import site.defence.global.exception.ErrorCode;
import site.defence.global.exception.GlobalException;
import site.defence.global.response.ResponseBody;
import site.defence.global.validator.ValidationErrorMessageBuilder;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ResponseBody<Void>> handleGlobalException(GlobalException e) {
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ResponseBody.error(errorCode));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException e,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        String errorMessage = ValidationErrorMessageBuilder.build(e);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ResponseBody.error(CommonErrorCode.INVALID_INPUT_VALUE, errorMessage));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            @NonNull Exception e,
            @Nullable Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request
    ) {
        String message = e.getMessage();

        return ResponseEntity
                .status(status)
                .headers(headers)
                .body(ResponseBody.error(status, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseBody<Void>> handleUnhandledException(Exception e) {
        log.error("Unhandled exception", e);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseBody.error(CommonErrorCode.INTERNAL_SERVER_ERROR));
    }
}
