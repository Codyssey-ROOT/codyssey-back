package site.defence.domain.user.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import site.defence.global.entity.BaseTimeEntity;

import java.time.Instant;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.*;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "social_id", nullable = false, unique = true, length = 100)
    private String socialId;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", nullable = false, length = 500)
    private String profileImageUrl;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder(access = PRIVATE)
    private User(String socialId, String email, String nickname, String profileImageUrl) {
        validateRequiredFields(socialId, email, nickname, profileImageUrl);

        this.socialId = socialId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public static User of(String socialId, String email, String nickname, String profileImageUrl) {

        return User
                .builder()
                .socialId(socialId)
                .email(email)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .build();
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        validateProfile(nickname, profileImageUrl);

        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }

    public void delete() {
        this.deletedAt = Instant.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    private void validateRequiredFields(
            String socialId,
            String email,
            String nickname,
            String profileImageUrl
    ) {
        validateSocialId(socialId);
        validateEmail(email);
        validateProfile(nickname, profileImageUrl);
    }

    private void validateSocialId(String socialId) {
        if (socialId == null || socialId.isBlank()) {
            throw new IllegalArgumentException("소셜 로그인 식별자는 필수입니다.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("이메일은 필수입니다.");
        }
    }

    private void validateProfile(String nickname, String profileImageUrl) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }

        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            throw new IllegalArgumentException("프로필 이미지 URL은 필수입니다.");
        }
    }
}
