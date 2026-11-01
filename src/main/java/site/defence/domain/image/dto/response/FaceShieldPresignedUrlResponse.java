package site.defence.domain.image.dto.response;

public record FaceShieldPresignedUrlResponse(

        String uploadUrl,

        String objectKey,

        long expiresInSeconds
) {
}