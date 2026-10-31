package site.defence.global.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.defence.domain.image.exception.ImageErrorCode;
import site.defence.global.exception.GlobalException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.bucket}")
    private String bucket;

    @Value("${app.images.presigned-put-ttl:300}")
    private long putTtlSeconds;

    public String createPresignedPutUrl(
            String objectKey,
            String contentType
    ) {
        PutObjectRequest putObjectRequest =
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType)
                        .build();

        PutObjectPresignRequest presignRequest =
                PutObjectPresignRequest.builder()
                        .signatureDuration(
                                Duration.ofSeconds(putTtlSeconds)
                        )
                        .putObjectRequest(putObjectRequest)
                        .build();

        return s3Presigner
                .presignPutObject(presignRequest)
                .url()
                .toExternalForm();
    }

    public HeadObjectResponse getObjectMetadata(
            String objectKey
    ) {
        try {
            return s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build()
            );
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new GlobalException(
                        ImageErrorCode.IMAGE_UPLOAD_NOT_FOUND
                );
            }

            throw e;
        }
    }
}