package site.defence.domain.user.repository;

import site.defence.domain.user.entity.User;

import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findByIdAndDeletedAtIsNull(Long id);
}
