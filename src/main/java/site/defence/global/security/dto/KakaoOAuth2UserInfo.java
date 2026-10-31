package site.defence.global.security.dto;

import java.util.Map;

public class KakaoOAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {

        this.attributes = attributes;
    }

    public String getSocialId() {

        return String.valueOf(attributes.get("id"));
    }

    public String getEmail() {

        return String.valueOf(getKakaoAccount().get("email"));
    }

    public String getNickname() {

        return String.valueOf(getProfile().get("nickname"));
    }

    public String getProfileImageUrl() {

        return String.valueOf(getProfile().get("profile_image_url"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getKakaoAccount() {

        return (Map<String, Object>) attributes.get("kakao_account");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getProfile() {

        return (Map<String, Object>) getKakaoAccount().get("profile");
    }
}
