package site.defence.domain.image.dto.response;

public record BlurPresignedUrlResponse(

        String uploadUrl,
        String objectKey,
        long expiresInSeconds
) {
}