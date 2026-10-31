package site.defence.domain.image.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.defence.domain.image.entity.FirstProcessedImage;

import java.util.Optional;

import static site.defence.domain.image.entity.QFirstProcessedImage.firstProcessedImage;

@Repository
@RequiredArgsConstructor
public class FirstProcessedImageQueryRepositoryImpl
        implements FirstProcessedImageQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<FirstProcessedImage> findLatestActiveBySourceImageId(
            Long sourceImageId,
            Long userId
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(firstProcessedImage)
                        .where(
                                firstProcessedImage.sourceImage.id
                                        .eq(sourceImageId),

                                firstProcessedImage.user.id
                                        .eq(userId),

                                firstProcessedImage.deletedAt.isNull()
                        )
                        .orderBy(firstProcessedImage.id.desc())
                        .fetchFirst()
        );
    }
}