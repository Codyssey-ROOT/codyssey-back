package site.defence.domain.user.dto.response;

import site.defence.domain.user.entity.User;

public record UserInfoResponse(
        Long userId,
        String email,
        String nickname,
        String profileImageUrl
) {

    public static UserInfoResponse from(User user) {

        return new UserInfoResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}
