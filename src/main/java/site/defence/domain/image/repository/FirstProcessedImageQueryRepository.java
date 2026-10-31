package site.defence.domain.image.repository;

import site.defence.domain.image.entity.FirstProcessedImage;

import java.util.Optional;

public interface FirstProcessedImageQueryRepository {

    Optional<FirstProcessedImage> findLatestActiveBySourceImageId(
            Long sourceImageId,
            Long userId
    );
}