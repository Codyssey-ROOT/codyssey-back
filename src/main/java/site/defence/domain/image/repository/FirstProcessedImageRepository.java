package site.defence.domain.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.image.entity.FirstProcessedImage;

public interface FirstProcessedImageRepository
        extends JpaRepository<FirstProcessedImage, Long>,
        FirstProcessedImageQueryRepository {
}