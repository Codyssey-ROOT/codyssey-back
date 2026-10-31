package site.defence.domain.image.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.defence.domain.image.entity.ImageStatus;
import site.defence.domain.image.entity.SourceImage;
import site.defence.domain.image.exception.ImageErrorCode;
import site.defence.domain.image.repository.SourceImageRepository;
import site.defence.global.exception.GlobalException;

@Service
@RequiredArgsConstructor
public class BlurDispatchStateService {

    private final SourceImageRepository sourceImageRepository;

    @Transactional
    public boolean start(Long imageId) {

        SourceImage image = sourceImageRepository
                .findByIdForUpdate(imageId)
                .orElseThrow(() -> new GlobalException(
                        ImageErrorCode.IMAGE_NOT_FOUND
                ));

        if (image.getStatus() != ImageStatus.UPLOADED) {
            return false;
        }

        image.startProcessing();

        return true;
    }
}