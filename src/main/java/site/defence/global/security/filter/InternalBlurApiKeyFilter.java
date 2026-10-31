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

    private final byte[] expected;

    public InternalBlurApiKeyFilter(
            String expectedKey
    ) {
        if (expectedKey == null || expectedKey.isBlank()) {
            throw new IllegalArgumentException(
                    "INTERNAL_API_KEY must not be blank"
            );
        }

        this.expected = expectedKey.getBytes(
                StandardCharsets.UTF_8
        );
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        String path = request.getServletPath();

        return !path.equals("/blur/images")
                && !path.startsWith("/blur/images/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String provided = request.getHeader(
                HEADER_NAME
        );

        boolean valid = provided != null
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