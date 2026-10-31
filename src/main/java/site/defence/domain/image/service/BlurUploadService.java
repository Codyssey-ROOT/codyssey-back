package site.defence.domain.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.defence.domain.image.dto.request.BlurCompleteRequest;
import site.defence.domain.image.dto.request.BlurPresignedUrlRequest;
import site.defence.domain.image.dto.response.BlurPresignedUrlResponse;
import site.defence.domain.image.entity.BlurUploadTicket;
import site.defence.domain.image.entity.FirstProcessedImage;
import site.defence.domain.image.entity.ImageStatus;
import site.defence.domain.image.entity.SourceImage;
import site.defence.domain.image.event.BlurCompletedEvent;
import site.defence.domain.image.exception.ImageErrorCode;
import site.defence.domain.image.repository.BlurUploadTicketRepository;
import site.defence.domain.image.repository.FirstProcessedImageRepository;
import site.defence.domain.image.repository.SourceImageRepository;
import site.defence.global.exception.GlobalException;
import site.defence.global.storage.S3StorageService;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlurUploadService {

    private final SourceImageRepository sourceImageRepository;
    private final BlurUploadTicketRepository ticketRepository;
    private final FirstProcessedImageRepository firstImageRepository;
    private final S3StorageService s3StorageService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.images.presigned-put-ttl:300}")
    private long ttlSeconds;

    @Value("${app.images.max-file-size:10485760}")
    private long maxFileSize;

    @Transactional
    public BlurPresignedUrlResponse createPresignedUrl(
            BlurPresignedUrlRequest request
    ) {
        // 1. 원본 이미지 조회 및 비관적 락 획득
        SourceImage source = findSourceForUpdate(
                request.sourceImageId()
        );

        // 2. 처리 상태 검증
        if (source.getStatus() != ImageStatus.PROCEED) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_STATUS
            );
        }

        // 3. 이미 블러 결과 저장이 완료되었는지 확인
        if (ticketRepository.existsBySourceImage_IdAndCompletedTrue(
                source.getId()
        )) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_STATUS
            );
        }

        // 4. Content-Type 검증
        if (!"image/jpeg".equalsIgnoreCase(
                request.contentType().trim()
        )) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_TYPE
            );
        }

        // 5. 파일 크기 검증
        if (request.fileSize() <= 0
                || request.fileSize() > maxFileSize) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_FILE_SIZE
            );
        }

        // 6. 동일 원본의 기존 미완료 티켓 조회
        BlurUploadTicket ticket = ticketRepository
                .findFirstBySourceImage_IdAndCompletedFalseOrderByIdDesc(
                        source.getId()
                )
                .orElse(null);

        if (ticket == null) {

            // 7. Spring에서 블러 결과 Object Key 생성
            String objectKey = String.format(
                    "images/blur/%d/%s.jpg",
                    source.getUser().getId(),
                    UUID.randomUUID()
            );

            ticket = ticketRepository.save(
                    BlurUploadTicket.of(
                            source,
                            objectKey,
                            "image/jpeg",
                            request.fileSize()
                    )
            );

        } else {

            // 미완료 요청 재시도 시 기존 티켓 재사용
            if (ticket.getFileSize() != request.fileSize()
                    || !"image/jpeg".equalsIgnoreCase(
                    ticket.getContentType()
            )) {
                throw new GlobalException(
                        ImageErrorCode.IMAGE_UPLOAD_MISMATCH
                );
            }
        }

        // 8. Presigned PUT URL 발급
        String uploadUrl = s3StorageService
                .createPresignedPutUrl(
                        ticket.getObjectKey(),
                        ticket.getContentType()
                );

        return new BlurPresignedUrlResponse(
                uploadUrl,
                ticket.getObjectKey(),
                ttlSeconds
        );
    }

    @Transactional
    public void completeUpload(
            BlurCompleteRequest request
    ) {
        // 1. 원본 이미지 조회 및 비관적 락 획득
        SourceImage source = findSourceForUpdate(
                request.sourceImageId()
        );

        // 2. Spring에서 발급했던 업로드 티켓 확인
        BlurUploadTicket ticket = ticketRepository
                .findByObjectKey(request.objectKey())
                .orElseThrow(() -> new GlobalException(
                        ImageErrorCode.IMAGE_NOT_FOUND
                ));

        // 3. 티켓과 원본 이미지 관계 확인
        if (!Objects.equals(
                ticket.getSourceImage().getId(),
                source.getId()
        )) {
            throw new GlobalException(
                    ImageErrorCode.IMAGE_UPLOAD_MISMATCH
            );
        }

        // 4. 중복 완료 요청이면 그대로 성공
        if (ticket.isCompleted()) {
            return;
        }

        // 5. 원본 이미지 상태 검증
        if (source.getStatus() != ImageStatus.PROCEED) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_STATUS
            );
        }

        // 6. 실제 S3 객체 존재 여부 확인
        HeadObjectResponse metadata =
                s3StorageService.getObjectMetadata(
                        ticket.getObjectKey()
                );

        // 7. 파일 크기와 Content-Type 검증
        boolean sameSize = Objects.equals(
                metadata.contentLength(),
                ticket.getFileSize()
        );

        boolean sameContentType =
                metadata.contentType() != null
                        && ticket.getContentType().equalsIgnoreCase(
                        metadata.contentType()
                );

        if (!sameSize || !sameContentType) {
            throw new GlobalException(
                    ImageErrorCode.IMAGE_UPLOAD_MISMATCH
            );
        }

        // 8. 블러 결과 이미지 엔티티 생성
        FirstProcessedImage firstProcessedImage =
                FirstProcessedImage.of(
                        source.getUser(),
                        source,
                        ticket.getObjectKey()
                );

        // 9. DB 저장
        FirstProcessedImage saved =
                firstImageRepository.save(
                        firstProcessedImage
                );

        // 10. 티켓 완료 상태로 변경
        ticket.markCompleted();

        // 11. 후속 FaceShield 처리를 위한 이벤트 발행
        eventPublisher.publishEvent(
                new BlurCompletedEvent(
                        source.getId(),
                        saved.getId()
                )
        );

        // FaceShield 연동 전이므로
        // SourceImage 상태는 PROCEED 유지
    }

    private SourceImage findSourceForUpdate(
            Long imageId
    ) {
        return sourceImageRepository
                .findByIdForUpdate(imageId)
                .orElseThrow(() -> new GlobalException(
                        ImageErrorCode.IMAGE_NOT_FOUND
                ));
    }
}