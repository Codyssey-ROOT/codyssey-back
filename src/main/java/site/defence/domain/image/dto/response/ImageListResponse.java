package site.defence.domain.image.dto.response;

import java.util.List;

public record ImageListResponse(
        List<ImageListItemResponse> images,
        Long nextCursor,
        boolean hasNext
) {
}