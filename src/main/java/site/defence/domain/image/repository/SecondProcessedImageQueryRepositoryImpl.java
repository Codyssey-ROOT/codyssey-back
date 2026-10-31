package site.defence.domain.image.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.defence.domain.image.entity.SecondProcessedImage;

import java.util.Optional;

import static site.defence.domain.image.entity.QSecondProcessedImage.secondProcessedImage;

@Repository
@RequiredArgsConstructor
public class SecondProcessedImageQueryRepositoryImpl
        implements SecondProcessedImageQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<SecondProcessedImage> findLatestActiveByFirstImageId(
            Long firstImageId,
            Long userId
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(secondProcessedImage)
                        .where(
                                secondProcessedImage.firstProcessedImage.id
                                        .eq(firstImageId),

                                secondProcessedImage.user.id
                                        .eq(userId),

                                secondProcessedImage.deletedAt.isNull()
                        )
                        .orderBy(secondProcessedImage.id.desc())
                        .fetchFirst()
        );
    }
}