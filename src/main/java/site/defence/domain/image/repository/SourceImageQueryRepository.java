package site.defence.domain.image.repository;

import site.defence.domain.image.entity.ImageStatus;
import site.defence.domain.image.entity.RiskLevel;
import site.defence.domain.image.entity.SourceImage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SourceImageQueryRepository {

    Optional<SourceImage> findOwnedById(
            Long imageId,
            Long userId
    );

    List<SourceImage> findPage(
            Long userId,
            Long cursor,
            Collection<ImageStatus> statuses,
            int limit
    );

    List<SourceImage> findByUserIdAndStatus(
            Long userId,
            ImageStatus status
    );

    List<SourceImage> findVulnerableByUserId(
            Long userId,
            RiskLevel riskLevel,
            Collection<ImageStatus> statuses
    );

    Optional<SourceImage> findOwnedByObjectKey(
            String objectKey,
            Long userId
    );
}