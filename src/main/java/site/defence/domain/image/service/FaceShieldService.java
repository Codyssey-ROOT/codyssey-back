package site.defence.domain.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
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

import site.defence.domain.image.event.FaceShieldCompletedEvent;
import site.defence.domain.image.exception.ImageErrorCode;

import site.defence.domain.image.repository.AiModelRepository;
import site.defence.domain.image.repository.FaceShieldJobRepository;
import site.defence.domain.image.repository.FaceShieldUploadTicketRepository;
import site.defence.domain.image.repository.SecondProcessedImageRepository;
import site.defence.domain.image.repository.SourceImageRepository;

import site.defence.domain.notification.NotificationType;
import site.defence.domain.notification.entity.Notification;
import site.defence.domain.notification.repository.NotificationRepository;

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

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

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
     * NCP GPU Worker에 처리할 작업 할당
     */
    @Transactional
    public Optional<FaceShieldClaimResponse> claim() {

        Instant now = Instant.now();

        // 최대 시도 횟수를 소진한 만료 작업 정리
        jobRepository.findExpiredExhausted(
                maxAttempts,
                now
        ).ifPresent(this::finishAsFailed);

        // QueryDSL 기반 다음 작업 조회
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

        if (sourceImage.getStatus() != ImageStatus.PROCEED) {
            finishAsFailed(job);
            return Optional.empty();
        }

        // 새로운 Lease Token 생성
        String leaseToken =
                UUID.randomUUID().toString();

        Instant expiresAt = now.plus(
                leaseSeconds,
                ChronoUnit.SECONDS
        );

        job.claim(
                leaseToken,
                expiresAt
        );

        // 블러 이미지 다운로드 URL 발급
        String downloadUrl =
                s3StorageService.createPresignedGetUrl(
                        firstImage.getObjectKey()
                );

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
     * 보호 이미지 업로드용 Presigned URL 발급
     */
    @Transactional
    public FaceShieldPresignedUrlResponse createPresignedUrl(
            FaceShieldPresignedUrlRequest request
    ) {

        FaceShieldJob job =
                findJobForUpdate(request.jobId());

        requireActiveLease(
                job,
                request.leaseToken()
        );

        // PNG 이미지 형식 검증
        if (!"image/png".equalsIgnoreCase(
                request.contentType().trim()
        )) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_TYPE
            );
        }

        // 파일 크기 검증
        if (request.fileSize() <= 0
                || request.fileSize() > maxFileSize) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_FILE_SIZE
            );
        }

        FaceShieldUploadTicket ticket =
                ticketRepository
                        .findFirstByJob_IdAndLeaseTokenAndCompletedFalseOrderByIdDesc(
                                job.getId(),
                                request.leaseToken()
                        )
                        .orElse(null);

        if (ticket == null) {

            String objectKey = String.format(
                    "images/faceshield/%d/%s.png",
                    job.getFirstProcessedImage()
                            .getUser()
                            .getId(),
                    UUID.randomUUID()
            );

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

        // 작업 조회 및 비관적 락
        FaceShieldJob job =
                findJobForUpdate(request.jobId());

        requireMatchingLease(
                job,
                request.leaseToken()
        );

        // 업로드 티켓 조회
        FaceShieldUploadTicket ticket =
                ticketRepository.findByObjectKey(
                        request.objectKey()
                ).orElseThrow(() -> new GlobalException(
                        ImageErrorCode.IMAGE_NOT_FOUND
                ));

        // 작업과 업로드 티켓 검증
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

        // 중복 완료 요청 방지
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

        requireActiveLease(
                job,
                request.leaseToken()
        );

        // 원본 이미지 조회 및 비관적 락
        SourceImage sourceImage =
                sourceImageRepository.findByIdForUpdate(
                        job.getFirstProcessedImage()
                                .getSourceImage()
                                .getId()
                ).orElseThrow(() -> new GlobalException(
                        ImageErrorCode.IMAGE_NOT_FOUND
                ));

        if (sourceImage.getStatus() != ImageStatus.PROCEED) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_STATUS
            );
        }

        // S3 실제 업로드 여부 검증
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

        // FaceShield 모델 조회
        AiModel model =
                aiModelRepository
                        .findFirstByModelNameOrderByIdAsc(
                                modelName
                        )
                        .orElseThrow(() -> new GlobalException(
                                ImageErrorCode.FACESHIELD_MODEL_NOT_FOUND
                        ));

        // 최종 보호 이미지 저장
        SecondProcessedImage secondImage =
                SecondProcessedImage.of(
                        job.getFirstProcessedImage(),
                        sourceImage.getUser(),
                        model,
                        ticket.getObjectKey()
                );

        secondImageRepository.save(secondImage);

        // 업로드 티켓 완료
        ticket.markCompleted();

        // 작업 상태 변경
        job.complete();

        // 최종 이미지 처리 완료
        sourceImage.complete();

        // 사용자 알림 DB 저장
        Notification notification = Notification.of(
                sourceImage.getUser(),
                "이미지 보호 처리가 완료되었습니다.",
                NotificationType.IMAGE_PROTECTION_COMPLETED
        );

        notificationRepository.save(notification);

        // DB 커밋 완료 후 FCM 발송 이벤트 실행
        eventPublisher.publishEvent(
                new FaceShieldCompletedEvent(
                        sourceImage.getUser().getId(),
                        sourceImage.getId()
                )
        );

        log.info(
                "FaceShield completed. jobId={} sourceImageId={}",
                job.getId(),
                sourceImage.getId()
        );
    }

    /**
     * GPU Worker 실패 보고
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
                "FaceShield failed. jobId={} attempt={}/{} reason={}",
                job.getId(),
                job.getAttemptCount(),
                maxAttempts,
                request.reason()
        );

        if (job.getAttemptCount() >= maxAttempts) {
            finishAsFailed(job);

        } else {
            job.requeue();
        }
    }

    /**
     * 작업 조회 및 비관적 락
     */
    private FaceShieldJob findJobForUpdate(
            Long jobId
    ) {
        return jobRepository
                .findByIdForUpdate(jobId)
                .orElseThrow(() -> new GlobalException(
                        ImageErrorCode.FACESHIELD_JOB_NOT_FOUND
                ));
    }

    /**
     * Lease Token 일치 여부 검증
     */
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

    /**
     * Lease Token 및 만료 시간 검증
     */
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
     * 최종 실패 상태 처리
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
                "FaceShield permanently failed. jobId={} sourceImageId={}",
                job.getId(),
                sourceImageId
        );
    }
}