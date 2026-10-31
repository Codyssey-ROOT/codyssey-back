package site.defence.domain.image.dto.response;

public record ImageUploadResponse(
        Long imageId,
        String uploadUrl,
        String objectKey,
        String publicUrl,
        String contentType,
        long expiresInSeconds
) {
}