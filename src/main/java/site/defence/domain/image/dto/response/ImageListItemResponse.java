package site.defence.domain.image.dto.response;

public record ImageListItemResponse(
        Long id,
        String imageUrl,
        String objectKey
) {
}