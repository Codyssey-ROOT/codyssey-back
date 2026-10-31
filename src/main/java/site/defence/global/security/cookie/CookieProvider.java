package site.defence.global.security.cookie;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class CookieProvider {

    private static final String COOKIE_PATH = "/";
    private static final String SAME_SITE = "Lax";
    private static final int DELETE_COOKIE_MAX_AGE = 0;

    private final String accessTokenName;
    private final long accessTokenTtl;
    private final String refreshTokenName;
    private final long refreshTokenTtl;
    private final String cookieDomain;

    public CookieProvider(
            @Value("${jwt.access-token-name}") String accessTokenName,
            @Value("${jwt.access-token-ttl}") long accessTokenTtl,
            @Value("${jwt.refresh-token-name}") String refreshTokenName,
            @Value("${jwt.refresh-token-ttl}") long refreshTokenTtl,
            @Value("${cookie.domain}") String cookieDomain
    ) {

        this.accessTokenName = accessTokenName;
        this.accessTokenTtl = accessTokenTtl;
        this.refreshTokenName = refreshTokenName;
        this.refreshTokenTtl = refreshTokenTtl;
        this.cookieDomain = cookieDomain;
    }

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {

        ResponseCookie cookie = createCookie(
                accessTokenName,
                accessToken,
                accessTokenTtl
        );

        addSetCookieHeader(response, cookie);
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {

        ResponseCookie cookie = createCookie(
                refreshTokenName,
                refreshToken,
                refreshTokenTtl
        );

        addSetCookieHeader(response, cookie);
    }

    public Optional<String> getAccessToken(HttpServletRequest request) {

        return getCookieValue(request, accessTokenName);
    }

    public Optional<String> getRefreshToken(HttpServletRequest request) {

        return getCookieValue(request, refreshTokenName);
    }

    public void deleteAccessTokenCookie(HttpServletResponse response) {

        ResponseCookie cookie = createDeleteCookie(accessTokenName);

        addSetCookieHeader(response, cookie);
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {

        ResponseCookie cookie = createDeleteCookie(refreshTokenName);

        addSetCookieHeader(response, cookie);
    }

    private Optional<String> getCookieValue(HttpServletRequest request, String cookieName) {

        Cookie[] cookies = request.getCookies();

        if (cookies == null) {

            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private ResponseCookie createCookie(String cookieName, String value, long maxAge) {

        return createCookieBuilder(cookieName, value)
                .maxAge(maxAge)
                .build();
    }

    private ResponseCookie createDeleteCookie(String cookieName) {

        return createCookieBuilder(cookieName, "")
                .maxAge(DELETE_COOKIE_MAX_AGE)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder createCookieBuilder(String cookieName, String value) {

        return ResponseCookie
                .from(cookieName, value)
                .httpOnly(true)
                .secure(true)
                .sameSite(SAME_SITE)
                .domain(cookieDomain)
                .path(COOKIE_PATH);
    }

    private void addSetCookieHeader(HttpServletResponse response, ResponseCookie cookie) {

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
