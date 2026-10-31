
package site.defence.domain.image.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.defence.domain.user.entity.User;
import site.defence.global.entity.BaseTimeEntity;

import java.time.Instant;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.*;

@Getter
@Entity
@Table(name = "first_proceed_image")
@NoArgsConstructor(access = PROTECTED)
public class FirstProcessedImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "first_proceed_image_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "source_image_id", nullable = false)
    private SourceImage sourceImage;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder(access = PRIVATE)
    private FirstProcessedImage(
            User user,
            SourceImage sourceImage,
            String objectKey
    ) {
        if (user == null || sourceImage == null) {
            throw new IllegalArgumentException("사용자와 원본 사진은 필수입니다.");
        }

        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("사진 객체 키는 필수입니다.");
        }

        this.user = user;
        this.sourceImage = sourceImage;
        this.objectKey = objectKey;
    }

    public static FirstProcessedImage of(
            User user,
            SourceImage sourceImage,
            String objectKey
    ) {
        return FirstProcessedImage.builder()
                .user(user)
                .sourceImage(sourceImage)
                .objectKey(objectKey)
                .build();
    }

    public void delete() {
        this.deletedAt = Instant.now();
    }
}
