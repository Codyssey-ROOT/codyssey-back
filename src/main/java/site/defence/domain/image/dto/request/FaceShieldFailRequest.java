package site.defence.domain.image.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record FaceShieldFailRequest(

        @NotNull
        @Positive
        Long jobId,

        @NotBlank
        String leaseToken,

        @Size(max = 300)
        String reason
) {
}