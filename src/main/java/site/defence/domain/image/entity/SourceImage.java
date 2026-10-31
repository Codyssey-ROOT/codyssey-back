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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ImageStatus status;

    @Builder(access = PRIVATE)
    private SourceImage(User user, String objectKey) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }

        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("사진 객체 키는 필수입니다.");
        }

        this.user = user;
        this.objectKey = objectKey;
        this.status = ImageStatus.PROCEED;
    }

    public static SourceImage of(User user, String objectKey) {
        return SourceImage.builder()
                .user(user)
                .objectKey(objectKey)
                .build();
    }

    public void complete() {
        this.status = ImageStatus.COMPLETED;
    }
}
