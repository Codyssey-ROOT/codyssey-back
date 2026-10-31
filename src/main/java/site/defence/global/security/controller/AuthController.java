package site.defence.global.security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.defence.global.response.ResponseBody;
import site.defence.global.security.service.AuthService;

@Tag(name = "Auth", description = "인증 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Access Token 재발급",
            description = "Refresh Token을 사용하여 Access Token을 재발급합니다."
    )
    @ApiResponse(responseCode = "200", description = "Access Token 재발급 성공")
    @ApiResponse(responseCode = "401", description = "Refresh Token 누락, 만료 또는 유효하지 않은 토큰")
    @PostMapping("/reissue")
    public ResponseBody<Void> reissueToken(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        authService.reissueToken(request, response);

        return ResponseBody.success();
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 사용자의 Refresh Token을 삭제하고 Access Token Cookie를 만료 처리합니다."
    )
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @ApiResponse(responseCode = "401", description = "인증 토큰 누락 또는 유효하지 않은 토큰")
    @PostMapping("/logout")
    public ResponseBody<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        authService.logout(request, response);

        return ResponseBody.success();
    }
}
