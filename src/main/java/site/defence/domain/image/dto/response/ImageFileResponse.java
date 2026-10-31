package site.defence.domain.image.dto.response;

public record ImageFileResponse(
        Long id,
        String objectKey,
        String getUrl
) {
}