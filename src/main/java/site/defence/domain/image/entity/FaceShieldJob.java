package site.defence.domain.image.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.defence.global.entity.BaseTimeEntity;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@Entity
@Table(
        name = "faceshield_job",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_faceshield_job_first_image",
                columnNames = "first_proceed_image_id"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaceShieldJob extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "faceshield_job_id")
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(
            name = "first_proceed_image_id",
            nullable = false
    )
    private FirstProcessedImage firstProcessedImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FaceShieldJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "lease_token", length = 36)
    private String leaseToken;

    @Column(name = "lease_expires_at")
    private Instant leaseExpiresAt;

    private FaceShieldJob(
            FirstProcessedImage firstProcessedImage
    ) {
        if (firstProcessedImage == null) {
            throw new IllegalArgumentException(
                    "블러 결과 이미지는 필수입니다."
            );
        }

        this.firstProcessedImage = firstProcessedImage;
        this.status = FaceShieldJobStatus.PENDING;
        this.attemptCount = 0;
    }

    public static FaceShieldJob pending(
            FirstProcessedImage firstProcessedImage
    ) {
        return new FaceShieldJob(firstProcessedImage);
    }

    public void claim(
            String token,
            Instant expiresAt
    ) {
        if (status != FaceShieldJobStatus.PENDING
                && status != FaceShieldJobStatus.PROCESSING) {
            throw new IllegalStateException(
                    "할당할 수 없는 작업입니다."
            );
        }

        if (token == null || token.isBlank()
                || expiresAt == null) {
            throw new IllegalArgumentException(
                    "유효한 작업 할당 정보가 필요합니다."
            );
        }

        this.status = FaceShieldJobStatus.PROCESSING;
        this.attemptCount++;
        this.leaseToken = token;
        this.leaseExpiresAt = expiresAt;
    }

    public boolean matchesLease(String token) {
        return token != null
                && token.equals(leaseToken);
    }

    public boolean hasActiveLease(Instant now) {
        return status == FaceShieldJobStatus.PROCESSING
                && leaseExpiresAt != null
                && now.isBefore(leaseExpiresAt);
    }

    public void requeue() {
        this.status = FaceShieldJobStatus.PENDING;
        this.leaseToken = null;
        this.leaseExpiresAt = null;
    }

    public void complete() {
        if (status != FaceShieldJobStatus.PROCESSING) {
            throw new IllegalStateException(
                    "처리 중인 작업이 아닙니다."
            );
        }

        this.status = FaceShieldJobStatus.COMPLETED;
        this.leaseExpiresAt = null;

        // 완료 요청의 멱등성을 위해 leaseToken 유지
    }

    public void fail() {
        this.status = FaceShieldJobStatus.FAILED;
        this.leaseToken = null;
        this.leaseExpiresAt = null;
    }
}