package site.defence.domain.image.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ImageUploadRequest(
        @NotBlank String contentType,
        @NotBlank String fileExt,
        @NotNull @Positive Long fileSize
) {
}