package site.defence.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String KAKAO_LOGIN_URL = "/oauth2/authorization/kakao";

    @Bean
    public OpenAPI openAPI() {

        return new OpenAPI()
                .info(apiInfo());
    }

    private Info apiInfo() {

        return new Info()
                .title("Deepfence API")
                .description("""
                        DeepFence 백엔드 API 문서입니다.
                        
                        Kakao 로그인을 통해 Access Token과 Refresh Token을 HttpOnly Cookie로 발급받은 뒤,
                        같은 브라우저에서 인증이 필요한 API를 테스트할 수 있습니다.
                        
                        [Kakao 로그인하기](%s)
                        """.formatted(KAKAO_LOGIN_URL))
                .version("v1.0.0");
    }
}
