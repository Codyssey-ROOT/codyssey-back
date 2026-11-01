package site.defence.domain.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.image.entity.FaceShieldJob;

public interface FaceShieldJobRepository
        extends JpaRepository<FaceShieldJob, Long>,
        FaceShieldJobQueryRepository {
}