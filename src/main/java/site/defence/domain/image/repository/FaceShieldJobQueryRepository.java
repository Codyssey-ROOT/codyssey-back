package site.defence.domain.image.repository;

import site.defence.domain.image.entity.FaceShieldJob;

import java.time.Instant;
import java.util.Optional;

public interface FaceShieldJobQueryRepository {

    Optional<FaceShieldJob> findNextClaimable(
            int maxAttempts,
            Instant now
    );

    Optional<FaceShieldJob> findExpiredExhausted(
            int maxAttempts,
            Instant now
    );

    Optional<FaceShieldJob> findByIdForUpdate(
            Long jobId
    );
}