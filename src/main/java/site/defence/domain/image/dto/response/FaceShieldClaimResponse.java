package site.defence.domain.image.dto.response;

public record FaceShieldClaimResponse(

        Long jobId,

        Long sourceImageId,

        Long firstProcessedImageId,

        String downloadUrl,

        String leaseToken,

        long leaseSeconds
) {
}