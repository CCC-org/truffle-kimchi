package ccc.keewedomain.persistence.repository.insight;

import static ccc.keewedomain.persistence.domain.challenge.QChallenge.challenge;
import static ccc.keewedomain.persistence.domain.challenge.QChallengeParticipation.challengeParticipation;
import static ccc.keewedomain.persistence.domain.common.QInterest.interest;
import static ccc.keewedomain.persistence.domain.insight.QBookmark.bookmark;
import static ccc.keewedomain.persistence.domain.insight.QComment.comment;
import static ccc.keewedomain.persistence.domain.insight.QInsight.insight;
import static ccc.keewedomain.persistence.domain.title.QTitle.title;
import static ccc.keewedomain.persistence.domain.user.QFollow.follow;
import static ccc.keewedomain.persistence.domain.user.QProfilePhoto.profilePhoto;
import static ccc.keewedomain.persistence.domain.user.QUser.user;

import ccc.keewedomain.persistence.domain.challenge.Challenge;
import ccc.keewedomain.persistence.domain.challenge.ChallengeParticipation;
import ccc.keewedomain.persistence.domain.insight.Insight;
import ccc.keewedomain.persistence.domain.title.QTitle;
import ccc.keewedomain.persistence.domain.user.QUser;
import ccc.keewedomain.persistence.domain.user.User;
import ccc.keewedomain.persistence.repository.utils.CursorPageable;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InsightQueryRepository {
    private final JPAQueryFactory queryFactory;

    public Insight findByIdWithWriter(Long insightId) {
        return queryFactory.select(insight)
                .from(insight)
                .where(insight.id.eq(insightId))
                .leftJoin(insight.writer, user)
                .fetchJoin()
                .leftJoin(user.interests, interest)
                .fetchJoin()
                .fetchOne();

    }

    public List<Insight> findAllValidByParticipation(ChallengeParticipation participation) {
        return queryFactory.select(insight)
                .from(insight)
                .leftJoin(insight.comments, comment)
                .fetchJoin()
                .where(insight.challengeParticipation.eq(participation)
                        .and(insight.deleted.isFalse())
                        .and(insight.valid.isTrue()))
                .distinct()
                .fetch();
    }

    public Long countValidByParticipation(ChallengeParticipation participation) {
        return queryFactory.select(insight.id.count())
                .from(insight)
                .where(insight.challengeParticipation.id.eq(participation.getId())
                        .and(insight.deleted.isFalse())
                        .and(insight.valid.isTrue()))
                .fetchFirst();
    }

    public Map<Long, Long> countValidPerParticipation(List<ChallengeParticipation> participations) {
        return queryFactory.select(insight.id.count())
                .from(insight)
                .where(insight.challengeParticipation.in(participations))
                .where(insight.valid.isTrue())
                .transform(GroupBy.groupBy(challengeParticipation.id).as(insight.count()));
    }

    public List<Insight> findAllForHome(User user, CursorPageable<Long> cPage, Boolean follow) {
        BooleanExpression followFilter = Expressions.asBoolean(true).isTrue();
        if (follow != null && follow)
            followFilter = insight.writer.id.in(findFolloweesId(user));

        return queryFactory.select(insight)
                .from(insight)
                .where(followFilter
                        .and(insight.id.lt(cPage.getCursor()))
                        .and(insight.deleted.isFalse())
                )
                .innerJoin(insight.writer, QUser.user).fetchJoin()
                .leftJoin(QUser.user.profilePhoto, profilePhoto).fetchJoin()
                .leftJoin(QUser.user.repTitle, title).fetchJoin()
                .orderBy(insight.id.desc())
                .limit(cPage.getLimit())
                .fetch();
    }

    private JPQLQuery<Long> findFolloweesId(User user) {
        return JPAExpressions
                .select(follow.followee.id)
                .from(follow)
                .where(follow.follower.id.eq(user.getId()));
    }

    public Long countByParticipationBetween(ChallengeParticipation participation, LocalDateTime startDate, LocalDateTime endDate) {
        return queryFactory.select(insight.id.count())
                .from(insight)
                .where(insight.challengeParticipation.id.eq(participation.getId())
                        .and(insight.deleted.isFalse())
                        .and(insight.valid.isTrue())
                        .and(insight.createdAt.goe(startDate).and(insight.createdAt.lt(endDate))))
                .fetchFirst();
    }

    public Optional<Insight> findByIdWithParticipationAndChallenge(Long insightId) {
        return Optional.ofNullable(queryFactory
                .select(insight)
                .from(insight)
                .leftJoin(insight.challengeParticipation, challengeParticipation)
                .fetchJoin()
                .leftJoin(challengeParticipation.challenge, challenge)
                .fetchJoin()
                .where(insight.id.eq(insightId))
                .fetchOne());
    }

    // countByValidTrueAndIdBefore
    public Long countValidByIdBeforeAndParticipation(ChallengeParticipation participation, Long insightId) {
        return queryFactory
                .select(insight.count())
                .from(insight)
                .where(insight.challengeParticipation.eq(participation)
                        .and(insight.valid.isTrue())
                        .and(insight.deleted.isFalse())
                        .and(insight.id.lt(insightId)))
                .fetchFirst();
    }

    public List<Insight> findAllByUserIdAndDrawerId(Long userId, Long drawerId, CursorPageable<Long> cPage) {
        return queryFactory
                .select(insight)
                .from(insight)
                .where(insight.writer.id.eq(userId)
                        .and(insight.id.lt(cPage.getCursor()))
                        .and(drawerIdEq(drawerId))
                )
                .orderBy(insight.id.desc())
                .limit(cPage.getLimit())
                .fetch();
    }

    public List<Insight> findAllByUserIdAndDrawerId(Long userId, Long drawerId) {
        return queryFactory
                .select(insight)
                .from(insight)
                .where(insight.writer.id.eq(userId)
                        .and(insight.drawer.id.eq(drawerId))
                )
                .fetch();
    }

    public Map<Long, Long> countPerChallenge(List<Challenge> challenges) {
        return queryFactory
                .from(insight)
                .innerJoin(insight.challengeParticipation, challengeParticipation)
                .innerJoin(challengeParticipation.challenge, challenge)
                .where(challenge.in(challenges))
                .groupBy(challenge.id)
                .transform(GroupBy.groupBy(challenge.id).as(insight.count()));
    }

    public Long countByChallenge(Challenge target, Long writerId) {
        return queryFactory
                .select(insight.count())
                .from(insight)
                .innerJoin(insight.challengeParticipation, challengeParticipation)
                .innerJoin(challengeParticipation.challenge, challenge)
                .where(challenge.eq(target)
                        .and(writerIdEq(writerId))
                )
                .fetchFirst();
    }

    public boolean existByWriterAndCreatedAtBetweenAndValidTrue(User writer, LocalDateTime startDate, LocalDateTime endDate) {
        return queryFactory
                .selectOne()
                .from(insight)
                .where(insight.writer.eq(writer))
                .where(insight.createdAt.between(startDate, endDate))
                .where(insight.valid.isTrue())
                .where(insight.deleted.isFalse())
                .fetchFirst() != null;
    }

    public List<Insight> findBookmarkedInsight(User user, CursorPageable<Long> cPage) {
        return queryFactory
                .select(insight)
                .from(insight)
                .where(insight.deleted.isFalse())
                .where(insight.in(findBookmarkedInsightId(user)))
                .where(insight.id.lt(cPage.getCursor()))
                .orderBy(insight.id.desc())
                .limit(cPage.getLimit())
                .fetch();
    }

    private JPQLQuery<Insight> findBookmarkedInsightId(User user) {
        return queryFactory
                .select(bookmark.insight)
                .from(bookmark)
                .where(bookmark.user.id.eq(user.getId()));
    }

    public List<Insight> findByChallenge(Challenge challenge, CursorPageable<Long> cPage, Long writerId) {
        return queryFactory
                .select(insight)
                .from(insight)
                .innerJoin(insight.challengeParticipation, challengeParticipation)
                .innerJoin(insight.writer, user)
                .fetchJoin()
                .innerJoin(user.profilePhoto, profilePhoto)
                .fetchJoin()
                .where(insight.challengeParticipation.challenge.eq(challenge)
                        .and(insight.id.lt(cPage.getCursor()))
                        .and(writerIdEq(writerId))
                )
                .orderBy(insight.id.desc())
                .fetch();
    }

    private BooleanExpression drawerIdEq(Long drawerId) {
        return drawerId != null ? insight.drawer.id.eq(drawerId) : null;
    }

    private BooleanExpression writerIdEq(Long writerId) {
        return writerId != null ? insight.writer.id.eq(writerId) : null;
    }
}
