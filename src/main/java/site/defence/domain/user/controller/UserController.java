package site.defence.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.defence.domain.user.dto.request.FcmTokenRegisterRequest;
import site.defence.domain.user.dto.response.UserInfoResponse;
import site.defence.domain.user.service.FcmTokenService;
import site.defence.domain.user.service.UserService;
import site.defence.global.response.ResponseBody;
import site.defence.global.security.service.AuthService;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final FcmTokenService fcmTokenService;
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
            summary = "FCM 토큰 등록",
            description = "로그인한 사용자의 FCM 토큰을 등록합니다. "
                    + "동일한 토큰은 중복 저장하지 않으며 "
                    + "다른 계정에 등록되어 있으면 소유자를 변경합니다."
    )
    @ApiResponse(responseCode = "200", description = "FCM 토큰 등록 성공")
    @ApiResponse(responseCode = "400", description = "FCM 토큰 누락 또는 형식 오류")
    @ApiResponse(responseCode = "401", description = "인증 토큰 누락 또는 만료")
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    @ApiResponse(responseCode = "409", description = "FCM 토큰 동시 등록 충돌")
    @PostMapping("/me/fcm-tokens")
    public ResponseBody<Void> registerFcmToken(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FcmTokenRegisterRequest request
    ) {
        fcmTokenService.register(userId, request.fcmToken());

        return ResponseBody.success();
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