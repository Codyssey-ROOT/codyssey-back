package site.defence.domain.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import site.defence.domain.image.dto.request.FaceShieldCompleteRequest;
import site.defence.domain.image.dto.request.FaceShieldFailRequest;
import site.defence.domain.image.dto.request.FaceShieldPresignedUrlRequest;

import site.defence.domain.image.dto.response.FaceShieldClaimResponse;
import site.defence.domain.image.dto.response.FaceShieldPresignedUrlResponse;

import site.defence.domain.image.entity.AiModel;
import site.defence.domain.image.entity.FaceShieldJob;
import site.defence.domain.image.entity.FaceShieldJobStatus;
import site.defence.domain.image.entity.FaceShieldUploadTicket;
import site.defence.domain.image.entity.FirstProcessedImage;
import site.defence.domain.image.entity.ImageStatus;
import site.defence.domain.image.entity.SecondProcessedImage;
import site.defence.domain.image.entity.SourceImage;

import site.defence.domain.image.exception.ImageErrorCode;

import site.defence.domain.image.repository.AiModelRepository;
import site.defence.domain.image.repository.FaceShieldJobRepository;
import site.defence.domain.image.repository.FaceShieldUploadTicketRepository;
import site.defence.domain.image.repository.SecondProcessedImageRepository;
import site.defence.domain.image.repository.SourceImageRepository;

import site.defence.global.exception.GlobalException;
import site.defence.global.storage.S3StorageService;

import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaceShieldService {

    private final FaceShieldJobRepository jobRepository;
    private final FaceShieldUploadTicketRepository ticketRepository;
    private final SourceImageRepository sourceImageRepository;
    private final SecondProcessedImageRepository secondImageRepository;
    private final AiModelRepository aiModelRepository;
    private final S3StorageService s3StorageService;

    @Value("${app.faceshield.lease-seconds:1800}")
    private long leaseSeconds;

    @Value("${app.faceshield.max-attempts:3}")
    private int maxAttempts;

    @Value("${app.faceshield.model-name:FaceShield}")
    private String modelName;

    @Value("${app.images.max-file-size:10485760}")
    private long maxFileSize;

    @Value("${app.images.presigned-put-ttl:300}")
    private long putTtlSeconds;

    /**
     * NCP GPU Worker에 FaceShield 작업을 할당한다.
     */
    @Transactional
    public Optional<FaceShieldClaimResponse> claim() {

        Instant now = Instant.now();

        // 1. 재시도 한도를 소진한 만료 작업 정리
        jobRepository.findExpiredExhausted(
                maxAttempts,
                now
        ).ifPresent(this::finishAsFailed);

        // 2. QueryDSL로 할당 가능한 작업 조회
        Optional<FaceShieldJob> optionalJob =
                jobRepository.findNextClaimable(
                        maxAttempts,
                        now
                );

        if (optionalJob.isEmpty()) {
            return Optional.empty();
        }

        FaceShieldJob job = optionalJob.get();

        FirstProcessedImage firstImage =
                job.getFirstProcessedImage();

        SourceImage sourceImage =
                firstImage.getSourceImage();

        // 3. 원본 이미지 상태 검증
        if (sourceImage.getStatus() != ImageStatus.PROCEED) {
            finishAsFailed(job);
            return Optional.empty();
        }

        // 4. 새로운 Lease Token 발급
        String leaseToken = UUID.randomUUID().toString();

        Instant expiresAt = now.plus(
                leaseSeconds,
                ChronoUnit.SECONDS
        );

        // 5. 작업 상태 변경
        job.claim(
                leaseToken,
                expiresAt
        );

        // 6. 블러 이미지 Presigned GET URL 생성
        String downloadUrl =
                s3StorageService.createPresignedGetUrl(
                        firstImage.getObjectKey()
                );

        // 7. 작업 할당 정보 반환
        return Optional.of(
                new FaceShieldClaimResponse(
                        job.getId(),
                        sourceImage.getId(),
                        firstImage.getId(),
                        downloadUrl,
                        leaseToken,
                        leaseSeconds
                )
        );
    }

    /**
     * FaceShield 보호 이미지 업로드 URL 발급
     */
    @Transactional
    public FaceShieldPresignedUrlResponse createPresignedUrl(
            FaceShieldPresignedUrlRequest request
    ) {

        // 1. 작업 조회 및 비관적 락 획득
        FaceShieldJob job =
                findJobForUpdate(request.jobId());

        // 2. 현재 작업의 유효한 Lease인지 확인
        requireActiveLease(
                job,
                request.leaseToken()
        );

        // 3. PNG 형식만 허용
        if (!"image/png".equalsIgnoreCase(
                request.contentType().trim()
        )) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_TYPE
            );
        }

        // 4. 파일 크기 검증
        if (request.fileSize() <= 0
                || request.fileSize() > maxFileSize) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_FILE_SIZE
            );
        }

        // 5. 기존 미완료 업로드 티켓 확인
        FaceShieldUploadTicket ticket =
                ticketRepository
                        .findFirstByJob_IdAndLeaseTokenAndCompletedFalseOrderByIdDesc(
                                job.getId(),
                                request.leaseToken()
                        )
                        .orElse(null);

        if (ticket == null) {

            // 6. Spring에서 보호 이미지 Object Key 생성
            String objectKey = String.format(
                    "images/faceshield/%d/%s.png",
                    job.getFirstProcessedImage()
                            .getUser()
                            .getId(),
                    UUID.randomUUID()
            );

            // 7. 업로드 티켓 저장
            ticket = ticketRepository.save(
                    FaceShieldUploadTicket.of(
                            job,
                            request.leaseToken(),
                            objectKey,
                            "image/png",
                            request.fileSize()
                    )
            );

        } else if (ticket.getFileSize()
                != request.fileSize()) {

            throw new GlobalException(
                    ImageErrorCode.IMAGE_UPLOAD_MISMATCH
            );
        }

        // 8. Presigned PUT URL 발급
        String uploadUrl =
                s3StorageService.createPresignedPutUrl(
                        ticket.getObjectKey(),
                        ticket.getContentType()
                );

        return new FaceShieldPresignedUrlResponse(
                uploadUrl,
                ticket.getObjectKey(),
                putTtlSeconds
        );
    }

    /**
     * FaceShield 보호 이미지 업로드 완료 처리
     */
    @Transactional
    public void complete(
            FaceShieldCompleteRequest request
    ) {

        // 1. 작업 조회 및 비관적 락
        FaceShieldJob job =
                findJobForUpdate(request.jobId());

        // 2. Lease Token 일치 여부 확인
        requireMatchingLease(
                job,
                request.leaseToken()
        );

        // 3. Object Key에 해당하는 티켓 조회
        FaceShieldUploadTicket ticket =
                ticketRepository
                        .findByObjectKey(
                                request.objectKey()
                        )
                        .orElseThrow(() -> new GlobalException(
                                ImageErrorCode.IMAGE_NOT_FOUND
                        ));

        // 4. 티켓과 작업의 관계 검증
        if (!Objects.equals(
                ticket.getJob().getId(),
                job.getId()
        ) || !Objects.equals(
                ticket.getLeaseToken(),
                request.leaseToken()
        )) {
            throw new GlobalException(
                    ImageErrorCode.IMAGE_UPLOAD_MISMATCH
            );
        }

        // 5. 동일 완료 요청 재전송 시 성공 반환
        if (ticket.isCompleted()
                && job.getStatus()
                == FaceShieldJobStatus.COMPLETED) {
            return;
        }

        if (ticket.isCompleted()) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_STATUS
            );
        }

        // 6. 유효한 작업 할당인지 검증
        requireActiveLease(
                job,
                request.leaseToken()
        );

        // 7. 원본 이미지 비관적 락 획득
        SourceImage sourceImage =
                sourceImageRepository.findByIdForUpdate(
                        job.getFirstProcessedImage()
                                .getSourceImage()
                                .getId()
                ).orElseThrow(() -> new GlobalException(
                        ImageErrorCode.IMAGE_NOT_FOUND
                ));

        if (sourceImage.getStatus()
                != ImageStatus.PROCEED) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_STATUS
            );
        }

        // 8. S3 객체 메타데이터 확인
        HeadObjectResponse metadata =
                s3StorageService.getObjectMetadata(
                        ticket.getObjectKey()
                );

        boolean sameSize = Objects.equals(
                metadata.contentLength(),
                ticket.getFileSize()
        );

        boolean sameType =
                metadata.contentType() != null
                        && ticket.getContentType()
                        .equalsIgnoreCase(
                                metadata.contentType()
                        );

        if (!sameSize || !sameType) {
            throw new GlobalException(
                    ImageErrorCode.IMAGE_UPLOAD_MISMATCH
            );
        }

        // 9. FaceShield AI 모델 조회
        AiModel model =
                aiModelRepository
                        .findFirstByModelNameOrderByIdAsc(
                                modelName
                        )
                        .orElseThrow(() -> new GlobalException(
                                ImageErrorCode.FACESHIELD_MODEL_NOT_FOUND
                        ));

        // 10. 최종 보호 이미지 저장
        SecondProcessedImage secondImage =
                SecondProcessedImage.of(
                        job.getFirstProcessedImage(),
                        sourceImage.getUser(),
                        model,
                        ticket.getObjectKey()
                );

        secondImageRepository.save(secondImage);

        // 11. 업로드 티켓 완료
        ticket.markCompleted();

        // 12. FaceShield 작업 완료
        job.complete();

        // 13. 원본 이미지 최종 처리 완료
        sourceImage.complete();

        log.info(
                "FaceShield completed. jobId={}, sourceImageId={}",
                job.getId(),
                sourceImage.getId()
        );

        // 실제 FCM 발송은 후속 단계에서 연결
    }

    /**
     * GPU Worker가 실패를 보고한 경우
     */
    @Transactional
    public void fail(
            FaceShieldFailRequest request
    ) {

        FaceShieldJob job =
                findJobForUpdate(request.jobId());

        requireActiveLease(
                job,
                request.leaseToken()
        );

        log.warn(
                "FaceShield failed. jobId={}, attempt={}/{}, reason={}",
                job.getId(),
                job.getAttemptCount(),
                maxAttempts,
                request.reason()
        );

        // 최대 재시도 횟수 초과
        if (job.getAttemptCount() >= maxAttempts) {
            finishAsFailed(job);
        } else {
            // 재시도 가능하면 대기 상태로 복귀
            job.requeue();
        }
    }

    private FaceShieldJob findJobForUpdate(
            Long jobId
    ) {
        return jobRepository
                .findByIdForUpdate(jobId)
                .orElseThrow(() -> new GlobalException(
                        ImageErrorCode.FACESHIELD_JOB_NOT_FOUND
                ));
    }

    private void requireMatchingLease(
            FaceShieldJob job,
            String leaseToken
    ) {
        if (!job.matchesLease(leaseToken)) {
            throw new GlobalException(
                    ImageErrorCode.FACESHIELD_LEASE_INVALID
            );
        }
    }

    private void requireActiveLease(
            FaceShieldJob job,
            String leaseToken
    ) {
        requireMatchingLease(
                job,
                leaseToken
        );

        if (!job.hasActiveLease(Instant.now())) {
            throw new GlobalException(
                    ImageErrorCode.FACESHIELD_LEASE_INVALID
            );
        }
    }

    /**
     * 작업 및 원본 이미지 실패 처리
     */
    private void finishAsFailed(
            FaceShieldJob job
    ) {

        job.fail();

        Long sourceImageId =
                job.getFirstProcessedImage()
                        .getSourceImage()
                        .getId();

        SourceImage sourceImage =
                sourceImageRepository
                        .findByIdForUpdate(sourceImageId)
                        .orElseThrow(() -> new GlobalException(
                                ImageErrorCode.IMAGE_NOT_FOUND
                        ));

        if (sourceImage.getStatus()
                != ImageStatus.COMPLETED
                && sourceImage.getStatus()
                != ImageStatus.FAILED) {
            sourceImage.fail();
        }

        log.warn(
                "FaceShield permanently failed. jobId={}, sourceImageId={}",
                job.getId(),
                sourceImageId
        );
    }
}