package site.defence.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.defence.global.entity.BaseTimeEntity;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.*;

@Getter
@Entity
@Table(
        name = "user_device",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_device_fcm_token",
                columnNames = "fcm_token"
        )
)
@NoArgsConstructor(access = PROTECTED)
public class UserDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "user_device_id")
    private Long id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(
            name = "fcm_token",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String fcmToken;

    @Builder(access = PRIVATE)
    private UserDevice(User user, String fcmToken) {
        validateFcmToken(fcmToken);

        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }

        this.user = user;
        this.fcmToken = fcmToken;
    }

    public static UserDevice of(User user, String fcmToken) {
        return UserDevice.builder()
                .user(user)
                .fcmToken(fcmToken)
                .build();
    }

    public void changeUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("사용자는 필수입니다.");
        }

        this.user = user;
    }

    public void updateFcmToken(String fcmToken) {
        validateFcmToken(fcmToken);
        this.fcmToken = fcmToken;
    }

    private void validateFcmToken(String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            throw new IllegalArgumentException("FCM 토큰은 필수입니다.");
        }
    }
}