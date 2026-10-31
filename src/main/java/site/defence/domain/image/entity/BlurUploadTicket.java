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
        name = "blur_upload_ticket",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_blur_upload_ticket_object_key",
                        columnNames = "object_key"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BlurUploadTicket extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "blur_upload_ticket_id")
    private Long id;

    @ManyToOne(fetch = LAZY, optional = false)
    @JoinColumn(
            name = "source_image_id",
            nullable = false
    )
    private SourceImage sourceImage;

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

    @Column(
            name = "file_size",
            nullable = false
    )
    private long fileSize;

    @Column(
            name = "completed",
            nullable = false
    )
    private boolean completed;

    private BlurUploadTicket(
            SourceImage sourceImage,
            String objectKey,
            String contentType,
            long fileSize
    ) {
        if (sourceImage == null) {
            throw new IllegalArgumentException(
                    "원본 이미지는 필수입니다."
            );
        }

        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException(
                    "S3 Object Key는 필수입니다."
            );
        }

        if (fileSize <= 0) {
            throw new IllegalArgumentException(
                    "파일 크기는 0보다 커야 합니다."
            );
        }

        this.sourceImage = sourceImage;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.completed = false;
    }

    public static BlurUploadTicket of(
            SourceImage sourceImage,
            String objectKey,
            String contentType,
            long fileSize
    ) {
        return new BlurUploadTicket(
                sourceImage,
                objectKey,
                contentType,
                fileSize
        );
    }

    public void markCompleted() {
        if (completed) {
            return;
        }

        this.completed = true;
    }
}