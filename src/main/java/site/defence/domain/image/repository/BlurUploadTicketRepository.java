package site.defence.domain.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.image.entity.BlurUploadTicket;

import java.util.Optional;

public interface BlurUploadTicketRepository
        extends JpaRepository<BlurUploadTicket, Long> {

    Optional<BlurUploadTicket> findByObjectKey(
            String objectKey
    );

    Optional<BlurUploadTicket>
    findFirstBySourceImage_IdAndCompletedFalseOrderByIdDesc(
            Long sourceImageId
    );

    boolean existsBySourceImage_IdAndCompletedTrue(
            Long sourceImageId
    );
}