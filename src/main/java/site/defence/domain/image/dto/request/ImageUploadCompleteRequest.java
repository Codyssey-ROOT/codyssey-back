package site.defence.domain.image.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ImageUploadCompleteRequest(
        @NotBlank String objectKey
) {
}