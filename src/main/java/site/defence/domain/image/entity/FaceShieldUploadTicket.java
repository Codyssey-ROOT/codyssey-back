package site.defence.domain.image.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.defence.global.entity.BaseTimeEntity;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;

@Getter
@Entity
@Table(
        name = "faceshield_upload_ticket",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_faceshield_upload_object_key",
                columnNames = "object_key"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FaceShieldUploadTicket
        extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "faceshield_upload_ticket_id")
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(
            name = "faceshield_job_id",
            nullable = false
    )
    private FaceShieldJob job;

    @Column(name = "lease_token", nullable = false, length = 36)
    private String leaseToken;

    @Column(
            name = "object_key",
            nullable = false,
            length = 1024
    )
    private String objectKey;

    @Column(
            name = "content_type",
            nullable = false,
            length = 100
    )
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private boolean completed;

    private FaceShieldUploadTicket(
            FaceShieldJob job,
            String leaseToken,
            String objectKey,
            String contentType,
            long fileSize
    ) {
        if (job == null
                || leaseToken == null
                || leaseToken.isBlank()
                || objectKey == null
                || objectKey.isBlank()
                || contentType == null
                || contentType.isBlank()
                || fileSize <= 0) {
            throw new IllegalArgumentException(
                    "업로드 티켓 정보가 유효하지 않습니다."
            );
        }

        this.job = job;
        this.leaseToken = leaseToken;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.completed = false;
    }

    public static FaceShieldUploadTicket of(
            FaceShieldJob job,
            String leaseToken,
            String objectKey,
            String contentType,
            long fileSize
    ) {
        return new FaceShieldUploadTicket(
                job,
                leaseToken,
                objectKey,
                contentType,
                fileSize
        );
    }

    public void markCompleted() {
        this.completed = true;
    }
}