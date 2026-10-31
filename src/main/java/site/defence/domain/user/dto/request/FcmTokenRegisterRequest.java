package site.defence.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FcmTokenRegisterRequest(
        @NotBlank(message = "FCM 토큰은 필수입니다.")
        @Size(max = 2048, message = "FCM 토큰은 2048자를 초과할 수 없습니다.")
        @Pattern(
                regexp = "[!-~]+",
                message = "FCM 토큰은 공백 없는 ASCII 문자열이어야 합니다."
        )
        String fcmToken
) {
}