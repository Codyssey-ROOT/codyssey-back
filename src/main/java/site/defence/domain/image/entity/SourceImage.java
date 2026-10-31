package site.defence.domain.image.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.defence.domain.user.entity.User;
import site.defence.global.entity.BaseTimeEntity;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.*;

@Getter
@Entity
@Table(name = "source_image")
@NoArgsConstructor(access = PROTECTED)
public class SourceImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "source_image_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImageStatus status;

    @Builder(access = PRIVATE)
    private SourceImage(
            User user,
            String objectKey,
            String contentType,
            Long fileSize
    ) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "사용자는 필수입니다."
            );
        }

        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException(
                    "이미지 객체 키는 필수입니다."
            );
        }

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException(
                    "Content-Type은 필수입니다."
            );
        }

        if (fileSize == null || fileSize <= 0) {
            throw new IllegalArgumentException(
                    "파일 크기는 0보다 커야 합니다."
            );
        }

        this.user = user;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.status = ImageStatus.UPLOADING;
    }

    public static SourceImage of(
            User user,
            String objectKey,
            String contentType,
            Long fileSize
    ) {
        return SourceImage.builder()
                .user(user)
                .objectKey(objectKey)
                .contentType(contentType)
                .fileSize(fileSize)
                .build();
    }

    public void markUploaded() {
        if (status != ImageStatus.UPLOADING) {
            throw new IllegalStateException(
                    "업로드 대기 상태가 아닙니다."
            );
        }

        this.status = ImageStatus.UPLOADED;
    }

    public void startProcessing() {
        if (status != ImageStatus.UPLOADED) {
            throw new IllegalStateException(
                    "AI 처리를 시작할 수 없는 상태입니다."
            );
        }

        this.status = ImageStatus.PROCEED;
    }

    public void complete() {
        if (status != ImageStatus.PROCEED) {
            throw new IllegalStateException(
                    "AI 처리 중인 이미지가 아닙니다."
            );
        }

        this.status = ImageStatus.COMPLETED;
    }

    public void fail() {
        if (status == ImageStatus.COMPLETED) {
            throw new IllegalStateException(
                    "이미 완료된 이미지는 실패 처리할 수 없습니다."
            );
        }

        this.status = ImageStatus.FAILED;
    }
}