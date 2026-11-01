package site.defence.domain.image.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FaceShieldCompleteRequest(

        @NotNull
        @Positive
        Long jobId,

        @NotBlank
        String leaseToken,

        @NotBlank
        String objectKey
) {
}