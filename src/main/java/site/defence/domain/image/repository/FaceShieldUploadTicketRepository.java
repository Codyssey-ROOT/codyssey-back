package site.defence.domain.image.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import site.defence.domain.image.entity.FaceShieldUploadTicket;

import java.util.Optional;

public interface FaceShieldUploadTicketRepository
        extends JpaRepository<FaceShieldUploadTicket, Long> {

    Optional<FaceShieldUploadTicket> findByObjectKey(
            String objectKey
    );

    Optional<FaceShieldUploadTicket>
    findFirstByJob_IdAndLeaseTokenAndCompletedFalseOrderByIdDesc(
            Long jobId,
            String leaseToken
    );
}