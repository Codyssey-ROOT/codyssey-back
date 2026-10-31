package site.defence.domain.image.event;

public record BlurCompletedEvent(
        Long sourceImageId,
        Long firstProcessedImageId
) {
}