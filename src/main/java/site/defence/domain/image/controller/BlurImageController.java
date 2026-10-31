package site.defence.domain.image.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.defence.domain.image.dto.request.BlurCompleteRequest;
import site.defence.domain.image.dto.request.BlurPresignedUrlRequest;
import site.defence.domain.image.dto.response.BlurPresignedUrlResponse;
import site.defence.domain.image.service.BlurUploadService;
import site.defence.global.response.ResponseBody;

@Tag(
        name = "Blur (Internal)",
        description = "FastAPI 전용 블러 결과 이미지 API"
)
@RestController
@RequiredArgsConstructor
@RequestMapping("/blur/images")
public class BlurImageController {

    private final BlurUploadService blurUploadService;

    @Operation(
            summary = "블러 결과 이미지 Presigned URL 발급"
    )
    @PostMapping("/presigned-url")
    public ResponseBody<BlurPresignedUrlResponse> createPresignedUrl(
            @Valid @RequestBody BlurPresignedUrlRequest request
    ) {
        return ResponseBody.success(
                blurUploadService.createPresignedUrl(request)
        );
    }

    @Operation(
            summary = "블러 결과 이미지 S3 업로드 완료"
    )
    @PostMapping("/complete")
    public ResponseBody<Void> completeUpload(
            @Valid @RequestBody BlurCompleteRequest request
    ) {
        blurUploadService.completeUpload(request);

        return ResponseBody.success();
    }
}