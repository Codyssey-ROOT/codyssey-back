package site.defence.domain.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.image.entity.SourceImage;

public interface SourceImageRepository
        extends JpaRepository<SourceImage, Long>,
                SourceImageQueryRepository {
}