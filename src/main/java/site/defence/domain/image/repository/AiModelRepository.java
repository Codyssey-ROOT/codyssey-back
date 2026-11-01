package site.defence.domain.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.image.entity.AiModel;

import java.util.Optional;

public interface AiModelRepository
        extends JpaRepository<AiModel, Long> {

    Optional<AiModel> findFirstByModelNameOrderByIdAsc(
            String modelName
    );
}