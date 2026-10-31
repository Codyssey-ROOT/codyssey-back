
package site.defence.domain.notification.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.defence.domain.notification.NotificationType;
import site.defence.domain.user.entity.User;
import site.defence.global.entity.BaseTimeEntity;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.*;

@Getter
@Entity
@Table(name = "notification")
@NoArgsConstructor(access = PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Builder(access = PRIVATE)
    private Notification(
            User user,
            String message,
            NotificationType notificationType
    ) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("알림 메시지는 필수입니다.");
        }

        if (notificationType == null) {
            throw new IllegalArgumentException("알림 종류는 필수입니다.");
        }

        this.user = user;
        this.message = message;
        this.notificationType = notificationType;
    }

    public static Notification of(
            User user,
            String message,
            NotificationType notificationType
    ) {
        return Notification.builder()
                .user(user)
                .message(message)
                .notificationType(notificationType)
                .build();
    }
}
