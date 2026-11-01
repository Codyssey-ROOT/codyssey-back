package site.defence.domain.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import site.defence.domain.image.dto.request.FaceShieldCompleteRequest;
import site.defence.domain.image.dto.request.FaceShieldFailRequest;
import site.defence.domain.image.dto.request.FaceShieldPresignedUrlRequest;

import site.defence.domain.image.dto.response.FaceShieldClaimResponse;
import site.defence.domain.image.dto.response.FaceShieldPresignedUrlResponse;

import site.defence.domain.image.service.FaceShieldService;
import site.defence.global.response.ResponseBody;

@Tag(
        name = "FaceShield (Internal)",
        description = "NCP GPU Worker 전용 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/faceshield")
public class FaceShieldImageController {

    private final FaceShieldService faceShieldService;

    @Operation(
            summary = "FaceShield 작업 할당"
    )
    @PostMapping("/jobs/claim")
    public ResponseEntity<ResponseBody<FaceShieldClaimResponse>> claim() {

        return faceShieldService.claim()
                .map(data ->
                        ResponseEntity.ok(
                                ResponseBody.success(data)
                        )
                )
                .orElseGet(() ->
                        ResponseEntity.noContent().build()
                );
    }

    @Operation(
            summary = "FaceShield 보호 이미지 업로드 URL 발급"
    )
    @PostMapping("/images/presigned-url")
    public ResponseBody<FaceShieldPresignedUrlResponse> createPresignedUrl(
            @Valid @RequestBody FaceShieldPresignedUrlRequest request
    ) {
        return ResponseBody.success(
                faceShieldService.createPresignedUrl(request)
        );
    }

    @Operation(
            summary = "FaceShield 보호 이미지 업로드 완료"
    )
    @PostMapping("/images/complete")
    public ResponseBody<Void> complete(
            @Valid @RequestBody FaceShieldCompleteRequest request
    ) {
        faceShieldService.complete(request);

        return ResponseBody.success();
    }

    @Operation(
            summary = "FaceShield 작업 실패 보고"
    )
    @PostMapping("/jobs/fail")
    public ResponseBody<Void> fail(
            @Valid @RequestBody FaceShieldFailRequest request
    ) {
        faceShieldService.fail(request);

        return ResponseBody.success();
    }
}