package site.defence.domain.image.dto.response;

import java.util.List;

public record ImageStatusResponse(
        List<ImageListItemResponse> processingImages,
        List<ImageListItemResponse> vulnerableImages
) {
}