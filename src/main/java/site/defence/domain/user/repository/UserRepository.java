package site.defence.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryPort {

    Optional<User> findBySocialId(String socialId);

    @Override
    Optional<User> findByIdAndDeletedAtIsNull(Long id);
}
