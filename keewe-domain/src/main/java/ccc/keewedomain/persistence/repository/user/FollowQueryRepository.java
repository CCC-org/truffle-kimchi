package ccc.keewedomain.persistence.repository.user;

import ccc.keewedomain.persistence.domain.user.Follow;
import ccc.keewedomain.persistence.domain.user.User;
import ccc.keewedomain.persistence.repository.utils.CursorPageable;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

import static ccc.keewedomain.persistence.domain.title.QTitle.title;
import static ccc.keewedomain.persistence.domain.user.QFollow.follow;
import static ccc.keewedomain.persistence.domain.user.QUser.user;

@Repository
@RequiredArgsConstructor
public class FollowQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<Long> findFollowingTargetIds(User user, List<User> targets) {
        return queryFactory.select(follow.followee.id)
                .from(follow)
                .where(follow.follower.eq(user).and(follow.followee.in((targets))))
                .fetch();
    }

    public List<Follow> findFollowersByUserCreatedAtDesc(User target, CursorPageable<LocalDateTime> cPage) {
        return queryFactory.select(follow)
                .from(follow)
                .innerJoin(follow.follower, user)
                .fetchJoin()
                .leftJoin(user.repTitle, title)
                .fetchJoin()
                .where(follow.follower.id.in(findFollowerIdsOrderByCreatedAtDesc(target, cPage)),
                        follow.followee.eq(target))
                .fetch();
    }

    public List<Follow> findFolloweesByUserCreatedAtDesc(User target, CursorPageable<LocalDateTime> cPage) {
        return queryFactory.select(follow)
                .from(follow)
                .innerJoin(follow.followee, user)
                .fetchJoin()
                .leftJoin(user.repTitle, title)
                .fetchJoin()
                .where(follow.followee.id.in(findFolloweeIdsOrderByCreatedAtDesc(target, cPage)),
                        follow.follower.eq(target))
                .fetch();
    }

    public List<Follow> findAllByUserIdOrderByCreatedAtDesc(Long userId, CursorPageable<LocalDateTime> cPage) {
        return queryFactory.selectFrom(follow)
                .where(follow.follower.id.eq(userId)
                        .or(follow.followee.id.eq(userId)))
                .where(follow.createdAt.lt(cPage.getCursor()))
                .orderBy(follow.createdAt.desc())
                .limit(cPage.getLimit())
                .fetch();
    }

    public List<Follow> findByUserIdAndStartsWithNickname(Long userId, String word, CursorPageable<String> cPage) {
        return queryFactory.selectFrom(follow)
                .innerJoin(follow.follower, user)
                .fetchJoin()
                .innerJoin(follow.followee, user)
                .fetchJoin()
                .where(follow.follower.id.eq(userId)
                        .or(follow.followee.id.eq(userId)))
                .where(user.nickname.startsWith(word)
                        .and(nicknameGt(cPage.getCursor())))
                .orderBy(user.nickname.asc())
                .limit(cPage.getLimit())
                .fetch();
    }

    // note. cursor가 null인 경우 조건을 추가하지 않음
    private BooleanExpression nicknameGt(String nickname) {
        return nickname != null ? user.nickname.gt(nickname) : null;
    }

    private JPQLQuery<Long> findFolloweeIdsOrderByCreatedAtDesc(User target, CursorPageable<LocalDateTime> cPage) {
        return JPAExpressions.select(follow.followee.id)
                .from(follow)
                .where(follow.follower.eq(target), follow.createdAt.lt(cPage.getCursor()))
                .orderBy(follow.createdAt.desc(), follow.followee.id.asc())
                .limit(cPage.getLimit());
    }

    private JPQLQuery<Long> findFollowerIdsOrderByCreatedAtDesc(User target, CursorPageable<LocalDateTime> cPage) {
        return JPAExpressions.select(follow.follower.id)
                .from(follow)
                .where(follow.followee.eq(target), follow.createdAt.lt(cPage.getCursor()))
                .orderBy(follow.createdAt.desc(), follow.follower.id.asc())
                .limit(cPage.getLimit());
    }
}
