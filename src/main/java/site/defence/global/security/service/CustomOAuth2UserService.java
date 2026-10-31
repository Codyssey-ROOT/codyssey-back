package site.defence.global.security.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import site.defence.domain.user.entity.User;
import site.defence.domain.user.repository.UserRepository;
import site.defence.global.exception.GlobalException;
import site.defence.global.security.dto.KakaoOAuth2UserInfo;
import site.defence.global.security.exception.OAuthErrorCode;
import site.defence.global.security.oauth.CustomOAuth2User;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = super.loadUser(userRequest);
        KakaoOAuth2UserInfo userInfo = new KakaoOAuth2UserInfo(oAuth2User.getAttributes());

        User user = transactionTemplate.execute(status -> findOrCreateUser(userInfo));

        if (user == null) {
            throw new GlobalException(OAuthErrorCode.OAUTH2_USER_PROCESSING_FAILED);
        }

        return new CustomOAuth2User(user.getId(), oAuth2User.getAttributes());
    }

    private User findOrCreateUser(KakaoOAuth2UserInfo userInfo) {

        return userRepository.findBySocialId(userInfo.getSocialId())
                .map(user -> updateUserProfile(user, userInfo))
                .orElseGet(() -> createUser(userInfo));
    }

    private User updateUserProfile(User user, KakaoOAuth2UserInfo userInfo) {

        if (user.getDeletedAt() != null) {
            user.restore();
        }

        user.updateProfile(
                userInfo.getNickname(),
                userInfo.getProfileImageUrl()
        );

        return user;
    }

    private User createUser(KakaoOAuth2UserInfo userInfo) {

        return userRepository.save(User.of(
                userInfo.getSocialId(),
                userInfo.getEmail(),
                userInfo.getNickname(),
                userInfo.getProfileImageUrl()
        ));
    }
}
