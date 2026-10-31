package site.defence.domain.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import site.defence.domain.user.entity.User;
import site.defence.domain.user.entity.UserDevice;
import site.defence.domain.user.exception.UserErrorCode;
import site.defence.domain.user.repository.UserDeviceRepository;
import site.defence.domain.user.repository.UserRepositoryPort;
import site.defence.global.exception.GlobalException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmTokenServiceTest {

    private static final String FCM_TOKEN = "test-fcm-token";

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private FcmTokenService fcmTokenService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = mock(User.class);
    }

    @Test
    void register_newToken_createsDevice() {
        when(userRepositoryPort.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(currentUser));

        when(userDeviceRepository.findByFcmTokenWithUser(FCM_TOKEN))
                .thenReturn(Optional.empty());

        fcmTokenService.register(1L, FCM_TOKEN);

        verify(userDeviceRepository)
                .saveAndFlush(any(UserDevice.class));
    }

    @Test
    void register_sameTokenSameUser_doesNotWrite() {
        UserDevice device = mock(UserDevice.class);

        when(currentUser.getId()).thenReturn(1L);

        User owner = mock(User.class);
        when(owner.getId()).thenReturn(1L);
        when(device.getUser()).thenReturn(owner);

        when(userRepositoryPort.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(currentUser));

        when(userDeviceRepository.findByFcmTokenWithUser(FCM_TOKEN))
                .thenReturn(Optional.of(device));

        fcmTokenService.register(1L, FCM_TOKEN);

        verify(device, never()).changeUser(any());
        verify(userDeviceRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void register_tokenOfAnotherUser_reassignsOwner() {
        User oldOwner = mock(User.class);

        when(oldOwner.getId()).thenReturn(2L);
        when(currentUser.getId()).thenReturn(1L);

        UserDevice device = UserDevice.of(oldOwner, FCM_TOKEN);

        when(userRepositoryPort.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(currentUser));

        when(userDeviceRepository.findByFcmTokenWithUser(FCM_TOKEN))
                .thenReturn(Optional.of(device));

        fcmTokenService.register(1L, FCM_TOKEN);

        assertThat(device.getUser()).isSameAs(currentUser);

        verify(userDeviceRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void register_deletedOrMissingUser_throws404() {
        when(userRepositoryPort.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> fcmTokenService.register(1L, FCM_TOKEN)
        )
                .isInstanceOf(GlobalException.class)
                .satisfies(e -> assertThat(
                        ((GlobalException) e).getErrorCode()
                ).isEqualTo(UserErrorCode.USER_NOT_FOUND));

        verifyNoInteractions(userDeviceRepository);
    }

    @Test
    void register_concurrentInsertConflict_throws409() {
        when(userRepositoryPort.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(currentUser));

        when(userDeviceRepository.findByFcmTokenWithUser(FCM_TOKEN))
                .thenReturn(Optional.empty());

        when(userDeviceRepository.saveAndFlush(any(UserDevice.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate fcm token"
                ));

        assertThatThrownBy(
                () -> fcmTokenService.register(1L, FCM_TOKEN)
        )
                .isInstanceOf(GlobalException.class)
                .satisfies(e -> assertThat(
                        ((GlobalException) e).getErrorCode()
                ).isEqualTo(UserErrorCode.FCM_TOKEN_CONFLICT));
    }
}