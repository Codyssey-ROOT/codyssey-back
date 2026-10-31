package site.defence.global.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import site.defence.global.exception.ErrorCode;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static lombok.AccessLevel.PRIVATE;

@Getter
@AllArgsConstructor(access = PRIVATE)
@Builder(access = PRIVATE)
public class ResponseBody<T> {

    private final int statusCode;
    private final String message;

    @JsonInclude(NON_NULL)
    private final Integer errorCode;

    @JsonInclude(NON_NULL)
    private final T data;

    public static <T> ResponseBody<T> success(T data) {
        return ResponseBody.<T>builder()
                .statusCode(HttpStatus.OK.value())
                .message(HttpStatus.OK.getReasonPhrase())
                .data(data)
                .build();
    }

    public static ResponseBody<Void> success() {
        return ResponseBody.<Void>builder()
                .statusCode(HttpStatus.OK.value())
                .message(HttpStatus.OK.getReasonPhrase())
                .build();
    }

    public static ResponseBody<Void> error(ErrorCode errorCode) {
        return ResponseBody.<Void>builder()
                .statusCode(errorCode.getStatus().value())
                .message(errorCode.getMessage())
                .errorCode(errorCode.getCode())
                .build();
    }

    public static ResponseBody<Void> error(ErrorCode errorCode, String message) {
        return ResponseBody.<Void>builder()
                .statusCode(errorCode.getStatus().value())
                .message(message)
                .errorCode(errorCode.getCode())
                .build();
    }

    public static ResponseBody<Void> error(HttpStatusCode status, String message) {
        return ResponseBody.<Void>builder()
                .statusCode(status.value())
                .message(message)
                .errorCode(status.value())
                .build();
    }
}
