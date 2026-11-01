package site.defence.domain.image.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import site.defence.domain.image.entity.FaceShieldJob;
import site.defence.domain.image.entity.FaceShieldJobStatus;

import java.time.Instant;
import java.util.Optional;

import static site.defence.domain.image.entity.QFaceShieldJob.faceShieldJob;

@Repository
@RequiredArgsConstructor
public class FaceShieldJobQueryRepositoryImpl
        implements FaceShieldJobQueryRepository {

    private final JPAQueryFactory queryFactory;

    private static final String LOCK_TIMEOUT_HINT =
            "jakarta.persistence.lock.timeout";

    private static final int SKIP_LOCKED = -2;

    /**
     * 대기 중이거나 Lease가 만료된 작업 1건 조회.
     * 다른 Worker가 잠근 작업은 건너뛴다.
     */
    @Override
    public Optional<FaceShieldJob> findNextClaimable(
            int maxAttempts,
            Instant now
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(faceShieldJob)
                        .where(
                                faceShieldJob.status.eq(
                                        FaceShieldJobStatus.PENDING
                                ).or(
                                        faceShieldJob.status.eq(
                                                FaceShieldJobStatus.PROCESSING
                                        ).and(
                                                faceShieldJob.leaseExpiresAt
                                                        .lt(now)
                                        )
                                ),
                                faceShieldJob.attemptCount.lt(
                                        maxAttempts
                                )
                        )
                        .orderBy(
                                faceShieldJob.createdAt.asc(),
                                faceShieldJob.id.asc()
                        )
                        .setLockMode(
                                LockModeType.PESSIMISTIC_WRITE
                        )
                        .setHint(
                                LOCK_TIMEOUT_HINT,
                                SKIP_LOCKED
                        )
                        .fetchFirst()
        );
    }

    /**
     * 최대 재시도 횟수를 사용하고
     * Lease까지 만료된 작업을 조회한다.
     */
    @Override
    public Optional<FaceShieldJob> findExpiredExhausted(
            int maxAttempts,
            Instant now
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(faceShieldJob)
                        .where(
                                faceShieldJob.status.eq(
                                        FaceShieldJobStatus.PROCESSING
                                ),
                                faceShieldJob.leaseExpiresAt.lt(
                                        now
                                ),
                                faceShieldJob.attemptCount.goe(
                                        maxAttempts
                                )
                        )
                        .orderBy(
                                faceShieldJob.createdAt.asc(),
                                faceShieldJob.id.asc()
                        )
                        .setLockMode(
                                LockModeType.PESSIMISTIC_WRITE
                        )
                        .setHint(
                                LOCK_TIMEOUT_HINT,
                                SKIP_LOCKED
                        )
                        .fetchFirst()
        );
    }

    /**
     * 완료/실패 요청 시 동일 작업에 대한 동시 수정을 방지한다.
     */
    @Override
    public Optional<FaceShieldJob> findByIdForUpdate(
            Long jobId
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(faceShieldJob)
                        .where(
                                faceShieldJob.id.eq(jobId)
                        )
                        .setLockMode(
                                LockModeType.PESSIMISTIC_WRITE
                        )
                        .fetchOne()
        );
    }
}