package site.defence.domain.user.repository;

import site.defence.domain.user.entity.UserDevice;

import java.util.Optional;

public interface UserDeviceQueryRepository {

    Optional<UserDevice> findByFcmTokenWithUser(String fcmToken);
}