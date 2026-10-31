package site.defence.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.defence.domain.user.dto.response.UserInfoResponse;
import site.defence.domain.user.service.UserService;
import site.defence.global.response.ResponseBody;
import site.defence.global.security.service.AuthService;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    @Operation(
            summary = "내 정보 조회",
            description = "현재 로그인한 사용자의 기본 회원 정보를 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "내 정보 조회 성공")
    @ApiResponse(responseCode = "401", description = "인증 토큰 누락 또는 만료")
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    @GetMapping("/me")
    public ResponseBody<UserInfoResponse> getMe(
            @AuthenticationPrincipal Long userId
    ) {

        UserInfoResponse response = userService.findMe(userId);

        return ResponseBody.success(response);
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자를 탈퇴 처리합니다."
    )
    @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공")
    @ApiResponse(responseCode = "401", description = "인증 토큰 누락 또는 만료")
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    @DeleteMapping("/me")
    public ResponseBody<Void> deleteMe(
            @AuthenticationPrincipal Long userId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        userService.deleteMe(userId);
        authService.logout(request, response);

        return ResponseBody.success();
    }
}
