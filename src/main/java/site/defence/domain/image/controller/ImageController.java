package site.defence.domain.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import site.defence.domain.image.dto.request.ImageUploadCompleteRequest;
import site.defence.domain.image.dto.request.ImageUploadRequest;
import site.defence.domain.image.dto.response.*;
import site.defence.domain.image.service.ImageService;
import site.defence.global.response.ResponseBody;

@Tag(name = "Image", description = "이미지 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/images")
public class ImageController {

    private final ImageService imageService;

    @Operation(
            summary = "Presigned URL 발급",
            description = "S3에 이미지를 업로드하기 위한 Presigned PUT URL을 발급합니다."
    )
    @PostMapping("/presigned-url")
    public ResponseBody<ImageUploadResponse> createUploadUrl(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ImageUploadRequest request
    ) {
        return ResponseBody.success(
                imageService.createUploadUrl(userId, request)
        );
    }

    @Operation(
            summary = "이미지 업로드 완료",
            description = "S3에 업로드된 이미지를 Object Key로 검증하고 업로드를 완료합니다."
    )
    @PostMapping("/complete")
    public ResponseBody<Void> completeUpload(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ImageUploadCompleteRequest request
    ) {
        imageService.completeUpload(userId, request);

        return ResponseBody.success();
    }

    @Operation(
            summary = "이미지 목록 조회",
            description = "이미지 ID 기반 커서 페이지네이션으로 10장씩 조회합니다."
    )
    @GetMapping
    public ResponseBody<ImageListResponse> getImages(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false)
            @Positive Long cursor
    ) {
        return ResponseBody.success(
                imageService.getImages(
                        userId,
                        cursor
                )
        );
    }

    @Operation(
            summary = "이미지 상태 조회",
            description = "모델링 진행 중인 이미지와 취약 모델에 연결된 이미지 목록을 조회합니다."
    )
    @GetMapping("/status")
    public ResponseBody<ImageStatusResponse> getImageStatus(
            @AuthenticationPrincipal Long userId
    ) {
        return ResponseBody.success(
                imageService.getImageStatus(userId)
        );
    }

    @Operation(
            summary = "이미지 상세 조회",
            description = "원본, 블러 및 FaceShield 이미지 정보를 조회합니다."
    )
    @GetMapping("/{imageId}")
    public ResponseBody<ImageDetailResponse> getImage(
            @AuthenticationPrincipal Long userId,
            @PathVariable @Positive Long imageId
    ) {
        return ResponseBody.success(
                imageService.getImage(
                        userId,
                        imageId
                )
        );
    }
}