package site.defence.global.validator;

import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.stream.Collectors;

public final class ValidationErrorMessageBuilder {

    private ValidationErrorMessageBuilder() {
    }

    public static String build(MethodArgumentNotValidException e) {
        return e.getBindingResult()
                .getAllErrors()
                .stream()
                .map(ValidationErrorMessageBuilder::formatError)
                .collect(Collectors.joining(", "));
    }

    private static String formatError(ObjectError error) {
        if (error instanceof FieldError fieldError) {
            return fieldError.getField() + ": " + fieldError.getDefaultMessage();
        }

        return error.getObjectName() + ": " + error.getDefaultMessage();
    }
}
