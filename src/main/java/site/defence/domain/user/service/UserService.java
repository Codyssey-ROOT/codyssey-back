package site.defence.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.defence.domain.user.dto.response.UserInfoResponse;
import site.defence.domain.user.entity.User;
import site.defence.domain.user.exception.UserErrorCode;
import site.defence.domain.user.repository.UserRepositoryPort;
import site.defence.global.exception.GlobalException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepositoryPort userRepositoryPort;

    public UserInfoResponse findMe(Long userId) {
        User user = userRepositoryPort.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        return UserInfoResponse.from(user);
    }

    @Transactional
    public void deleteMe(Long userId) {

        User user = userRepositoryPort.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GlobalException(UserErrorCode.USER_NOT_FOUND));

        user.delete();
    }
}
