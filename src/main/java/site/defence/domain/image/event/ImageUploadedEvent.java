package site.defence.domain.image.event;

public record ImageUploadedEvent(
        Long imageId,
        String objectKey
) {
}