package site.defence.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import site.defence.global.security.cookie.CookieProvider;
import site.defence.global.security.filter.InternalBlurApiKeyFilter;
import site.defence.global.security.filter.JwtAuthenticationFilter;
import site.defence.global.security.handler.CustomAccessDeniedHandler;
import site.defence.global.security.handler.CustomAuthenticationEntryPoint;
import site.defence.global.security.handler.CustomOAuth2SuccessHandler;
import site.defence.global.security.jwt.TokenProvider;
import site.defence.global.security.service.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOAuth2SuccessHandler customOAuth2SuccessHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CookieProvider cookieProvider;
    private final TokenProvider tokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,

            @Value("${app.blur.internal-api-key}")
            String blurInternalApiKey,

            @Value("${app.faceshield.internal-api-key}")
            String faceShieldInternalApiKey
    ) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(sessionManagement ->
                        sessionManagement.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(authorizeHttpRequests ->
                        authorizeHttpRequests
                                .requestMatchers(
                                        "/oauth2/**",
                                        "/login/oauth2/**",
                                        "/users/reissue",
                                        "/users/logout",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/actuator/health",
                                        "/actuator/prometheus",

                                        // Blur FastAPI 내부 API
                                        "/blur/images/presigned-url",
                                        "/blur/images/complete",

                                        // FaceShield GPU Worker 내부 API
                                        "/faceshield/jobs/claim",
                                        "/faceshield/jobs/fail",
                                        "/faceshield/images/presigned-url",
                                        "/faceshield/images/complete"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )

                .exceptionHandling(exceptionHandling ->
                        exceptionHandling
                                .authenticationEntryPoint(
                                        customAuthenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        customAccessDeniedHandler
                                )
                )

                .oauth2Login(oauth2Login ->
                        oauth2Login
                                .userInfoEndpoint(userInfoEndpoint ->
                                        userInfoEndpoint.userService(
                                                customOAuth2UserService
                                        )
                                )
                                .successHandler(
                                        customOAuth2SuccessHandler
                                )
                )

                .addFilterBefore(
                        new JwtAuthenticationFilter(
                                cookieProvider,
                                tokenProvider
                        ),
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterBefore(
                        new InternalBlurApiKeyFilter(
                                blurInternalApiKey,
                                faceShieldInternalApiKey
                        ),
                        JwtAuthenticationFilter.class
                )

                .build();
    }
}