package site.defence.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class InternalBlurApiKeyFilter
        extends OncePerRequestFilter {

    private static final String HEADER_NAME =
            "X-Internal-API-Key";

    private final byte[] blurExpected;
    private final byte[] faceShieldExpected;

    public InternalBlurApiKeyFilter(
            String blurKey,
            String faceShieldKey
    ) {
        if (blurKey == null || blurKey.isBlank()
                || faceShieldKey == null
                || faceShieldKey.isBlank()) {
            throw new IllegalArgumentException(
                    "Internal API keys must not be blank"
            );
        }

        this.blurExpected =
                blurKey.getBytes(StandardCharsets.UTF_8);

        this.faceShieldExpected =
                faceShieldKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        String path = request.getServletPath();

        return !path.startsWith("/blur/images/")
                && !path.startsWith("/faceshield/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getServletPath();

        // API 경로에 따라 서로 다른 키 사용
        byte[] expected =
                path.startsWith("/faceshield/")
                        ? faceShieldExpected
                        : blurExpected;

        String provided = request.getHeader(
                HEADER_NAME
        );

        boolean valid =
                provided != null
                        && MessageDigest.isEqual(
                        expected,
                        provided.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        if (!valid) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
            );

            response.setContentType(
                    "application/json;charset=UTF-8"
            );

            response.getWriter().write(
                    "{\"statusCode\":401,\"message\":\"Unauthorized\"}"
            );

            return;
        }

        filterChain.doFilter(
                request,
                response
        );
    }
}