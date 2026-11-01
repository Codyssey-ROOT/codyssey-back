package site.defence.domain.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import site.defence.domain.user.repository.UserDeviceRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmNotificationService {

    private static final int MAX_BATCH_SIZE = 500;

    private static final String TITLE =
            "DeepFence";

    private static final String BODY =
            "이미지 보호 처리가 완료되었습니다.";

    private final UserDeviceRepository userDeviceRepository;

    private final ObjectProvider<FirebaseMessaging>
            firebaseMessagingProvider;

    public void sendImageProtectionCompleted(
            Long userId,
            Long sourceImageId
    ) {

        // Firebase가 비활성화된 경우 전송 생략
        FirebaseMessaging messaging =
                firebaseMessagingProvider.getIfAvailable();

        if (messaging == null) {
            log.info(
                    "FCM disabled. userId={} imageId={}",
                    userId,
                    sourceImageId
            );
            return;
        }

        // 사용자의 모든 FCM 토큰 조회
        List<String> tokens =
                userDeviceRepository.findFcmTokensByUserId(
                        userId
                );

        if (tokens.isEmpty()) {
            log.info(
                    "No FCM tokens. userId={} imageId={}",
                    userId,
                    sourceImageId
            );
            return;
        }

        int totalSuccess = 0;
        int totalFailure = 0;

        // Firebase 전송 한도에 맞춰 배치 처리
        for (
                int start = 0;
                start < tokens.size();
                start += MAX_BATCH_SIZE
        ) {

            int end = Math.min(
                    start + MAX_BATCH_SIZE,
                    tokens.size()
            );

            List<String> batch =
                    tokens.subList(start, end);

            MulticastMessage message =
                    MulticastMessage.builder()

                            .addAllTokens(batch)

                            .setNotification(
                                    Notification.builder()
                                            .setTitle(TITLE)
                                            .setBody(BODY)
                                            .build()
                            )

                            .putData(
                                    "type",
                                    "IMAGE_PROTECTION_COMPLETED"
                            )

                            .putData(
                                    "imageId",
                                    String.valueOf(sourceImageId)
                            )

                            .build();

            try {
                BatchResponse response =
                        messaging.sendEachForMulticast(
                                message
                        );

                totalSuccess += response.getSuccessCount();

                totalFailure += response.getFailureCount();

            } catch (FirebaseMessagingException e) {

                totalFailure += batch.size();

                log.error(
                        "FCM batch send failed. userId={} imageId={} errorCode={}",
                        userId,
                        sourceImageId,
                        e.getMessagingErrorCode()
                );
            }
        }

        log.info(
                "FCM send completed. userId={} imageId={} success={} failure={}",
                userId,
                sourceImageId,
                totalSuccess,
                totalFailure
        );
    }
}