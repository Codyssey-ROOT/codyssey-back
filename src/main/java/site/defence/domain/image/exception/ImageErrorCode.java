package site.defence.domain.image.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import site.defence.global.exception.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ImageErrorCode implements ErrorCode {

    INVALID_IMAGE_TYPE(
            HttpStatus.BAD_REQUEST,
            3000,
            "지원하지 않는 이미지 형식입니다."
    ),

    INVALID_FILE_SIZE(
            HttpStatus.BAD_REQUEST,
            3001,
            "올바르지 않은 파일 크기입니다."
    ),

    IMAGE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            3002,
            "이미지를 찾을 수 없습니다."
    ),

    IMAGE_UPLOAD_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            3003,
            "S3에 업로드된 이미지가 없습니다."
    ),

    IMAGE_UPLOAD_MISMATCH(
            HttpStatus.BAD_REQUEST,
            3004,
            "업로드한 이미지 정보가 일치하지 않습니다."
    ),

    IMAGE_NOT_READY(
            HttpStatus.CONFLICT,
            3005,
            "이미지 업로드가 완료되지 않았습니다."
    ),

    INVALID_IMAGE_STATUS(
            HttpStatus.CONFLICT,
            3006,
            "현재 이미지 상태에서는 요청을 처리할 수 없습니다."
    ),

    FACESHIELD_JOB_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            3007,
            "FaceShield 작업을 찾을 수 없습니다."
    ),

    FACESHIELD_LEASE_INVALID(
            HttpStatus.CONFLICT,
            3008,
            "FaceShield 작업 할당이 만료되었거나 유효하지 않습니다."
    ),

    FACESHIELD_MODEL_NOT_FOUND(
            HttpStatus.INTERNAL_SERVER_ERROR,
            3009,
            "FaceShield AI 모델 정보가 등록되지 않았습니다."
    );

    private final HttpStatus status;
    private final int code;
    private final String message;
}