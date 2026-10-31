package site.defence.domain.user.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import site.defence.domain.user.dto.request.FcmTokenRegisterRequest;

import static org.assertj.core.api.Assertions.assertThat;

class FcmTokenRegisterRequestTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    void validToken_hasNoViolations() {
        assertThat(validator.validate(
                new FcmTokenRegisterRequest("abc123:_-")
        )).isEmpty();
    }

    @Test
    void blankToken_isRejected() {
        assertThat(validator.validate(
                new FcmTokenRegisterRequest("  ")
        )).isNotEmpty();
    }

    @Test
    void missingToken_isRejected() {
        assertThat(validator.validate(
                new FcmTokenRegisterRequest(null)
        )).isNotEmpty();
    }

    @Test
    void whitespaceInsideToken_isRejected() {
        assertThat(validator.validate(
                new FcmTokenRegisterRequest("abc 123")
        )).isNotEmpty();
    }

    @Test
    void excessivelyLongToken_isRejected() {
        assertThat(validator.validate(
                new FcmTokenRegisterRequest("x".repeat(2049))
        )).isNotEmpty();
    }
}