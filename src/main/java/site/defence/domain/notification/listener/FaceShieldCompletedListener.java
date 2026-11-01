package site.defence.domain.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import site.defence.domain.image.event.FaceShieldCompletedEvent;
import site.defence.domain.notification.service.FcmNotificationService;

@Slf4j
@Component
@RequiredArgsConstructor
public class FaceShieldCompletedListener {

    private final FcmNotificationService
            fcmNotificationService;

    @Async("notificationTaskExecutor")
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            FaceShieldCompletedEvent event
    ) {

        try {
            fcmNotificationService.sendImageProtectionCompleted(
                    event.userId(),
                    event.sourceImageId()
            );

        } catch (Exception e) {
            log.error(
                    "FaceShield FCM notification failed. userId={} imageId={}",
                    event.userId(),
                    event.sourceImageId(),
                    e
            );
        }
    }
}