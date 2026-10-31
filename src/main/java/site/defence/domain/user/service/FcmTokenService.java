package site.defence.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.defence.domain.user.entity.User;
import site.defence.domain.user.entity.UserDevice;
import site.defence.domain.user.exception.UserErrorCode;
import site.defence.domain.user.repository.UserDeviceRepository;
import site.defence.domain.user.repository.UserRepositoryPort;
import site.defence.global.exception.GlobalException;

@Service
@RequiredArgsConstructor
public class FcmTokenService {

    private final UserRepositoryPort userRepositoryPort;
    private final UserDeviceRepository userDeviceRepository;

    @Transactional
    public void register(Long userId, String fcmToken) {
        User user = userRepositoryPort.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GlobalException(
                        UserErrorCode.USER_NOT_FOUND
                ));

        userDeviceRepository.findByFcmTokenWithUser(fcmToken)
                .ifPresentOrElse(
                        device -> reassignIfNecessary(device, user),
                        () -> createDevice(user, fcmToken)
                );
    }

    private void reassignIfNecessary(UserDevice device, User user) {
        if (device.getUser().getId().equals(user.getId())) {
            return;
        }

        device.changeUser(user);
    }

    private void createDevice(User user, String fcmToken) {
        try {
            userDeviceRepository.saveAndFlush(
                    UserDevice.of(user, fcmToken)
            );
        } catch (DataIntegrityViolationException e) {
            throw new GlobalException(
                    UserErrorCode.FCM_TOKEN_CONFLICT
            );
        }
    }
}