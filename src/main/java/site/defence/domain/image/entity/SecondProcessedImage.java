
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
@Table(name = "second_proceed_image")
@NoArgsConstructor(access = PROTECTED)
public class SecondProcessedImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "second_proceed_image_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "first_proceed_image_id", nullable = false)
    private FirstProcessedImage firstProcessedImage;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "ai_model_id", nullable = false)
    private AiModel aiModel;

    @Column(name = "object_key", nullable = false, length = 1024)
    private String objectKey;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder(access = PRIVATE)
    private SecondProcessedImage(
            FirstProcessedImage firstProcessedImage,
            User user,
            AiModel aiModel,
            String objectKey
    ) {
        if (firstProcessedImage == null || user == null || aiModel == null) {
            throw new IllegalArgumentException("가공 사진, 사용자, AI 모델은 필수입니다.");
        }

        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("사진 객체 키는 필수입니다.");
        }

        this.firstProcessedImage = firstProcessedImage;
        this.user = user;
        this.aiModel = aiModel;
        this.objectKey = objectKey;
    }

    public static SecondProcessedImage of(
            FirstProcessedImage firstProcessedImage,
            User user,
            AiModel aiModel,
            String objectKey
    ) {
        return SecondProcessedImage.builder()
                .firstProcessedImage(firstProcessedImage)
                .user(user)
                .aiModel(aiModel)
                .objectKey(objectKey)
                .build();
    }

    public void delete() {
        this.deletedAt = Instant.now();
    }
}
