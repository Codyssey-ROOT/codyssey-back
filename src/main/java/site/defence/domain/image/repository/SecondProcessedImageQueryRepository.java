package site.defence.domain.image.repository;

import site.defence.domain.image.entity.SecondProcessedImage;

import java.util.Optional;

public interface SecondProcessedImageQueryRepository {

    Optional<SecondProcessedImage> findLatestActiveByFirstImageId(
            Long firstImageId,
            Long userId
    );
}