package site.defence.global.security.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import site.defence.global.security.cookie.CookieProvider;
import site.defence.global.security.jwt.TokenProvider;
import site.defence.global.security.oauth.CustomOAuth2User;
import site.defence.global.security.provider.RefreshTokenProvider;
import site.defence.global.security.repository.RefreshTokenRedisRepository;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private final RefreshTokenProvider refreshTokenProvider;
    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
    private final CookieProvider cookieProvider;

    @Value("${oauth2.success-redirect-url}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = customOAuth2User.getUserId();
        String accessToken = tokenProvider.generateAccessToken(userId);
        String refreshToken = refreshTokenProvider.generateRefreshToken();
        String hashedRefreshToken = refreshTokenProvider.hashRefreshToken(refreshToken);

        refreshTokenRedisRepository.saveRefreshToken(hashedRefreshToken, userId);

        cookieProvider.addAccessTokenCookie(response, accessToken);
        cookieProvider.addRefreshTokenCookie(response, refreshToken);

        response.sendRedirect(successRedirectUrl);
    }
}
