package site.defence.domain.user.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import site.defence.domain.user.entity.UserDevice;

import java.util.List;
import java.util.Optional;

import static site.defence.domain.user.entity.QUser.user;
import static site.defence.domain.user.entity.QUserDevice.userDevice;

@Repository
@RequiredArgsConstructor
public class UserDeviceQueryRepositoryImpl
        implements UserDeviceQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<UserDevice> findByFcmTokenWithUser(
            String fcmToken
    ) {
        return Optional.ofNullable(
                queryFactory
                        .selectFrom(userDevice)
                        .join(userDevice.user, user)
                        .fetchJoin()
                        .where(
                                userDevice.fcmToken.eq(fcmToken)
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<String> findFcmTokensByUserId(
            Long userId
    ) {
        return queryFactory
                .select(userDevice.fcmToken)
                .from(userDevice)
                .join(userDevice.user, user)
                .where(
                        user.id.eq(userId),
                        user.deletedAt.isNull()
                )
                .fetch();
    }
}