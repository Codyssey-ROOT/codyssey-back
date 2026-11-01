package site.defence.domain.notification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.notification.entity.Notification;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {
}