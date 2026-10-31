package site.defence.domain.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.image.entity.SecondProcessedImage;

public interface SecondProcessedImageRepository
        extends JpaRepository<SecondProcessedImage, Long>,
        SecondProcessedImageQueryRepository {
}