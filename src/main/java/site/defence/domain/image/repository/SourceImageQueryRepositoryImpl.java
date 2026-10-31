package site.defence.domain.image.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.defence.domain.image.entity.ImageStatus;
import site.defence.domain.image.entity.RiskLevel;
import site.defence.domain.image.entity.SourceImage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static site.defence.domain.image.entity.QAiModel.aiModel;
import static site.defence.domain.image.entity.QFirstProcessedImage.firstProcessedImage;
import static site.defence.domain.image.entity.QSecondProcessedImage.secondProcessedImage;
import static site.defence.domain.image.entity.QSourceImage.sourceImage;

@Repository
@RequiredArgsConstructor
public class SourceImageQueryRepositoryImpl
        implements SourceImageQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<SourceImage> findOwnedById(
            Long imageId,
            Long userId
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(sourceImage)
                        .where(
                                sourceImage.id.eq(imageId),
                                sourceImage.user.id.eq(userId)
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<SourceImage> findPage(
            Long userId,
            Long cursor,
            Collection<ImageStatus> statuses,
            int limit
    ) {
        return queryFactory
                .selectFrom(sourceImage)
                .where(
                        sourceImage.user.id.eq(userId),
                        sourceImage.status.in(statuses),
                        cursorLt(cursor)
                )
                .orderBy(sourceImage.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<SourceImage> findByUserIdAndStatus(
            Long userId,
            ImageStatus status
    ) {
        return queryFactory
                .selectFrom(sourceImage)
                .where(
                        sourceImage.user.id.eq(userId),
                        sourceImage.status.eq(status)
                )
                .orderBy(sourceImage.id.desc())
                .fetch();
    }

    @Override
    public List<SourceImage> findVulnerableByUserId(
            Long userId,
            RiskLevel riskLevel,
            Collection<ImageStatus> statuses
    ) {
        return queryFactory
                .selectFrom(sourceImage)
                .where(
                        sourceImage.user.id.eq(userId),
                        sourceImage.status.in(statuses),

                        JPAExpressions
                                .selectOne()
                                .from(secondProcessedImage)
                                .join(
                                        secondProcessedImage.firstProcessedImage,
                                        firstProcessedImage
                                )
                                .join(
                                        secondProcessedImage.aiModel,
                                        aiModel
                                )
                                .where(
                                        firstProcessedImage.sourceImage.id
                                                .eq(sourceImage.id),

                                        firstProcessedImage.user.id
                                                .eq(userId),

                                        secondProcessedImage.user.id
                                                .eq(userId),

                                        firstProcessedImage.deletedAt.isNull(),
                                        secondProcessedImage.deletedAt.isNull(),

                                        aiModel.riskLevel.eq(riskLevel)
                                )
                                .exists()
                )
                .orderBy(sourceImage.id.desc())
                .fetch();
    }

    @Override
    public Optional<SourceImage> findOwnedByObjectKey(
            String objectKey,
            Long userId
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(sourceImage)
                        .where(
                                sourceImage.objectKey.eq(objectKey),
                                sourceImage.user.id.eq(userId)
                        )
                        .fetchOne()
        );
    }

    private BooleanExpression cursorLt(Long cursor) {
        return cursor == null
                ? null
                : sourceImage.id.lt(cursor);
    }
}