package site.defence.domain.image.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BlurCompleteRequest(

        @NotNull
        @Positive
        Long sourceImageId,

        @NotBlank
        String objectKey
) {
}