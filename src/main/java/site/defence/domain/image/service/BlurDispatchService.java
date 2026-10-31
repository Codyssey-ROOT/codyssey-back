package site.defence.domain.image.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import site.defence.domain.image.event.ImageUploadedEvent;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class BlurDispatchService {

    private final BlurDispatchStateService stateService;
    private final RestClient restClient;
    private final String apiKey;

    public BlurDispatchService(
            BlurDispatchStateService stateService,
            @Value("${app.blur.base-url}") String baseUrl,
            @Value("${app.blur.api-key}") String apiKey
    ) {
        this.stateService = stateService;
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(
                Duration.ofSeconds(5)
        );

        factory.setReadTimeout(
                Duration.ofSeconds(45)
        );

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Async("blurTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onImageUploaded(
            ImageUploadedEvent event
    ) {
        try {
            // 1. 이미지 상태 변경 및 커밋
            boolean started = stateService.start(
                    event.imageId()
            );

            if (!started) {
                log.info(
                        "Skip duplicate blur request imageId={}",
                        event.imageId()
                );
                return;
            }

            // 2. FastAPI 요청
            restClient.post()
                    .uri("/process")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-API-Key", apiKey)
                    .body(Map.of(
                            "imageId", event.imageId(),
                            "objectKey", event.objectKey()
                    ))
                    .retrieve()
                    .toBodilessEntity();

            // FastAPI 200 OK = S3 원본 다운로드 성공
            log.info(
                    "FastAPI source download accepted imageId={}",
                    event.imageId()
            );

        } catch (Exception e) {
            log.error(
                    "FastAPI dispatch failed or uncertain imageId={}",
                    event.imageId(),
                    e
            );
        }
    }
}