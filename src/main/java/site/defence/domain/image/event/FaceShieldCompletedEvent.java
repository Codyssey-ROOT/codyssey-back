package site.defence.domain.image.event;

public record FaceShieldCompletedEvent(

        Long userId,

        Long sourceImageId
) {
}