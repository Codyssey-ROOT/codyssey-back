
package site.defence.domain.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.defence.domain.image.dto.request.ImageUploadRequest;
import site.defence.domain.image.dto.response.*;
import site.defence.domain.image.entity.*;
import site.defence.domain.image.exception.ImageErrorCode;
import site.defence.domain.image.repository.*;
import site.defence.domain.user.entity.User;
import site.defence.domain.user.exception.UserErrorCode;
import site.defence.domain.user.repository.UserRepositoryPort;
import site.defence.global.exception.GlobalException;
import site.defence.global.storage.CloudFrontUrlService;
import site.defence.global.storage.S3StorageService;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ImageService {

    private static final int PAGE_SIZE = 10;

    private static final Map<String, Set<String>> EXTENSIONS =
            Map.of(
                    "image/jpeg", Set.of("jpg", "jpeg"),
                    "image/png", Set.of("png"),
                    "image/webp", Set.of("webp")
            );

    private static final List<ImageStatus> VISIBLE_STATUSES =
            List.of(
                    ImageStatus.UPLOADED,
                    ImageStatus.PROCEED,
                    ImageStatus.COMPLETED
            );

    private final UserRepositoryPort userRepository;
    private final SourceImageRepository sourceImageRepository;
    private final FirstProcessedImageRepository firstImageRepository;
    private final SecondProcessedImageRepository secondImageRepository;

    private final S3StorageService s3StorageService;
    private final CloudFrontUrlService cloudFrontUrlService;

    @Value("${app.images.max-file-size:10485760}")
    private long maxFileSize;

    @Value("${app.images.presigned-put-ttl:300}")
    private long putTtlSeconds;

    // S3 Presigned PUT URL 발급
    @Transactional
    public ImageUploadResponse createUploadUrl(
            Long userId,
            ImageUploadRequest request
    ) {
        User user = findActiveUser(userId);

        String contentType = request.contentType()
                .trim()
                .toLowerCase(Locale.ROOT);

        String fileExt = request.fileExt()
                .trim()
                .toLowerCase(Locale.ROOT);

        Set<String> allowedExtensions =
                EXTENSIONS.get(contentType);

        if (allowedExtensions == null
                || !allowedExtensions.contains(fileExt)) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_TYPE
            );
        }

        if (request.fileSize() <= 0
                || request.fileSize() > maxFileSize) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_FILE_SIZE
            );
        }

        String objectKey = String.format(
                "images/source/%d/%s.%s",
                userId,
                UUID.randomUUID(),
                fileExt
        );

        SourceImage image = SourceImage.of(
                user,
                objectKey,
                contentType,
                request.fileSize()
        );

        sourceImageRepository.save(image);

        String uploadUrl =
                s3StorageService.createPresignedPutUrl(
                        objectKey,
                        contentType
                );

        String publicUrl =
                cloudFrontUrlService.createGetUrl(objectKey);

        return new ImageUploadResponse(
                image.getId(),
                uploadUrl,
                objectKey,
                publicUrl,
                contentType,
                putTtlSeconds
        );
    }

    // S3 업로드 완료 검증
    @Transactional
    public void completeUpload(
            Long userId,
            Long imageId
    ) {
        findActiveUser(userId);

        SourceImage image =
                findUserImage(userId, imageId);

        if (image.getStatus() == ImageStatus.UPLOADED
                || image.getStatus() == ImageStatus.PROCEED
                || image.getStatus() == ImageStatus.COMPLETED) {
            return;
        }

        if (image.getStatus() != ImageStatus.UPLOADING) {
            throw new GlobalException(
                    ImageErrorCode.INVALID_IMAGE_STATUS
            );
        }

        HeadObjectResponse metadata =
                s3StorageService.getObjectMetadata(
                        image.getObjectKey()
                );

        boolean sameSize = Objects.equals(
                image.getFileSize(),
                metadata.contentLength()
        );

        boolean sameType = image.getContentType()
                .equalsIgnoreCase(metadata.contentType());

        if (!sameSize || !sameType) {
            throw new GlobalException(
                    ImageErrorCode.IMAGE_UPLOAD_MISMATCH
            );
        }

        image.markUploaded();
    }

    // 이미지 목록 - 10개 커서 페이지네이션
    public ImageListResponse getImages(
            Long userId,
            Long cursor
    ) {
        findActiveUser(userId);

        List<SourceImage> results =
                sourceImageRepository.findPage(
                        userId,
                        cursor,
                        VISIBLE_STATUSES,
                        PAGE_SIZE + 1
                );

        boolean hasNext = results.size() > PAGE_SIZE;

        List<SourceImage> page = results.subList(
                0,
                Math.min(PAGE_SIZE, results.size())
        );

        List<ImageListItemResponse> images = page.stream()
                .map(this::toListItem)
                .toList();

        Long nextCursor = hasNext && !page.isEmpty()
                ? page.get(page.size() - 1).getId()
                : null;

        return new ImageListResponse(
                images,
                nextCursor,
                hasNext
        );
    }

    // 모델링 진행 중 / 취약 이미지 목록
    public ImageStatusResponse getImageStatus(
            Long userId
    ) {
        findActiveUser(userId);

        List<SourceImage> processingImages =
                sourceImageRepository.findByUserIdAndStatus(
                        userId,
                        ImageStatus.PROCEED
                );

        List<SourceImage> vulnerableImages =
                sourceImageRepository.findVulnerableByUserId(
                        userId,
                        RiskLevel.DANGER,
                        VISIBLE_STATUSES
                );

        return new ImageStatusResponse(
                processingImages.stream()
                        .map(this::toListItem)
                        .toList(),
                vulnerableImages.stream()
                        .map(this::toListItem)
                        .toList()
        );
    }

    // 원본 / 블러 / FaceShield 상세 조회
    public ImageDetailResponse getImage(
            Long userId,
            Long imageId
    ) {
        findActiveUser(userId);

        SourceImage source =
                findUserImage(userId, imageId);

        if (!VISIBLE_STATUSES.contains(source.getStatus())) {
            throw new GlobalException(
                    ImageErrorCode.IMAGE_NOT_READY
            );
        }

        ImageFileResponse originalImage =
                createImageFile(
                        source.getId(),
                        source.getObjectKey()
                );

        FirstProcessedImage firstImage =
                firstImageRepository
                        .findLatestActiveBySourceImageId(
                                imageId,
                                userId
                        )
                        .orElse(null);

        ImageFileResponse blurredImage = null;
        ImageFileResponse faceShieldImage = null;

        if (firstImage != null) {
            blurredImage = createImageFile(
                    firstImage.getId(),
                    firstImage.getObjectKey()
            );

            SecondProcessedImage secondImage =
                    secondImageRepository
                            .findLatestActiveByFirstImageId(
                                    firstImage.getId(),
                                    userId
                            )
                            .orElse(null);

            if (secondImage != null) {
                faceShieldImage = createImageFile(
                        secondImage.getId(),
                        secondImage.getObjectKey()
                );
            }
        }

        return new ImageDetailResponse(
                source.getId(),
                originalImage,
                blurredImage,
                faceShieldImage
        );
    }

    private ImageListItemResponse toListItem(
            SourceImage image
    ) {
        return new ImageListItemResponse(
                image.getId(),
                cloudFrontUrlService.createGetUrl(
                        image.getObjectKey()
                ),
                image.getObjectKey()
        );
    }

    private ImageFileResponse createImageFile(
            Long id,
            String objectKey
    ) {
        return new ImageFileResponse(
                id,
                objectKey,
                cloudFrontUrlService.createGetUrl(objectKey)
        );
    }

    private SourceImage findUserImage(
            Long userId,
            Long imageId
    ) {
        return sourceImageRepository
                .findOwnedById(imageId, userId)
                .orElseThrow(() -> new GlobalException(
                        ImageErrorCode.IMAGE_NOT_FOUND
                ));
    }

    private User findActiveUser(Long userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GlobalException(
                        UserErrorCode.USER_NOT_FOUND
                ));
    }
}
