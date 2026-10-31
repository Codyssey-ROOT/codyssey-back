package site.defence.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.user.entity.UserDevice;

public interface UserDeviceRepository
        extends JpaRepository<UserDevice, Long>,
        UserDeviceQueryRepository {
}