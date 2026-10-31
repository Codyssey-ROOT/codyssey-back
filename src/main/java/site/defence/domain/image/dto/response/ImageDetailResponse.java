package site.defence.domain.image.dto.response;

public record ImageDetailResponse(
        Long imageId,
        ImageFileResponse originalImage,
        ImageFileResponse blurredImage,
        ImageFileResponse faceShieldImage
) {
}