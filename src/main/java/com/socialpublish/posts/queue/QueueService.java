package com.socialpublish.posts.queue;

import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.exception.PostNotFoundException;
import com.socialpublish.posts.exception.PostValidationException;
import com.socialpublish.posts.exception.UnauthorizedPostAccessException;
import com.socialpublish.posts.mapper.PostMapper;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final int MAX_QUEUE_PAGE_SIZE = 100;
    private static final int DAYS_IN_WEEK = 7;
    private static final int DAYS_IN_MONTH = 30;
    private static final int LOAD_MORE_STEP = 10;
    private final PostRepository postRepository;
    private final PostService postService;
    private final PostMapper postMapper;

    @Transactional(readOnly = true)
    public QueuePageView getQueue(UUID ownerId, QueueFilterRequest filter) {
        Specification<Post> specification = buildSpecification(ownerId, filter);
        long totalFiltered = postRepository.count(specification);
        PageRequest pageable = PageRequest.of(0, filter.getSize());
        List<Post> filteredPosts = postRepository.findAll(specification, pageable).getContent();

        List<UUID> postIds = filteredPosts.stream().map(Post::getId).toList();
        List<Post> postsWithMedia = postIds.isEmpty()
                ? List.of()
                : postRepository.findAllWithMediaByIdIn(postIds);

        Map<UUID, Post> postsMap = postsWithMedia.stream()
                .collect(Collectors.toMap(Post::getId, post -> post));

        List<Post> orderedPosts = postIds.stream()
                .map(postsMap::get)
                .filter(Objects::nonNull)
                .toList();

        List<PostView> posts = orderedPosts.stream()
                .map(postMapper::toView)
                .toList();

        boolean hasMore = totalFiltered > posts.size();
        int nextSize = Math.min(filter.getSize() + LOAD_MORE_STEP, MAX_QUEUE_PAGE_SIZE);
        List<Object[]> rawStats = postRepository.countByOwnerIdGroupedByStatus(ownerId);
        Map<PostStatus, Long> statsMap = rawStats.stream()
                .collect(Collectors.toMap(
                        row -> (PostStatus) row[0],
                        row -> (Long) row[1]
                ));

        QueueStatsView stats = new QueueStatsView(
                statsMap.getOrDefault(PostStatus.SCHEDULED, 0L),
                statsMap.getOrDefault(PostStatus.PUBLISHED, 0L),
                statsMap.getOrDefault(PostStatus.FAILED, 0L),
                statsMap.getOrDefault(PostStatus.DRAFT, 0L)
        );

        Instant nextScheduledAt = postRepository
                .findFirstByOwnerIdAndStatusAndScheduledAtAfterOrderByScheduledAtAsc(
                        ownerId,
                        PostStatus.SCHEDULED,
                        Instant.now()
                )
                .map(Post::getScheduledAt)
                .orElse(null);

        return new QueuePageView(posts, stats, totalFiltered, hasMore, nextSize, nextScheduledAt);
    }

    public QueueBulkResult runBulkAction(
            UUID ownerId,
            QueueBulkAction action,
            List<UUID> postIds,
            LocalDateTime scheduledAt
    ) {
        if (postIds == null || postIds.isEmpty()) {
            return new QueueBulkResult(0, 0);
        }

        int processed = 0;
        int skipped = 0;

        for (UUID postId : postIds) {
            try {
                switch (action) {
                    case PUBLISH_NOW -> postService.publishNow(ownerId, postId);
                    case DELETE -> postService.deletePost(ownerId, postId);
                    case RESCHEDULE -> {
                        if (scheduledAt == null) {
                            throw new PostValidationException("Date and time are required for reschedule action");
                        }
                        postService.reschedulePost(ownerId, postId, scheduledAt);
                    }
                    case DUPLICATE -> postService.duplicatePost(ownerId, postId);
                    case MOVE_TO_DRAFT -> postService.moveToDraft(ownerId, postId);
                    case RETRY_FAILED -> postService.retryFailedNow(ownerId, postId);
                }
                processed++;
            } catch (PostNotFoundException | PostValidationException | UnauthorizedPostAccessException ex) {
                skipped++;
            }
        }

        return new QueueBulkResult(processed, skipped);
    }

    private Specification<Post> buildSpecification(UUID ownerId, QueueFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("owner").get("id"), ownerId));

            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), PostStatus.valueOf(filter.getStatus())));
            }

            buildSearchPredicate(root, cb, filter, predicates);
            buildPlatformPredicate(root, cb, filter, predicates);
            buildTypePredicate(root, cb, filter, predicates);
            buildDateRangePredicate(root, cb, filter, predicates);

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                List<jakarta.persistence.criteria.Order> orders = new ArrayList<>();
                switch (filter.getSort()) {
                    case OLDEST -> orders.add(cb.asc(root.get("updatedAt")));
                    case SCHEDULED_SOON -> {
                        orders.add(cb.asc(cb.coalesce(root.get("scheduledAt"), root.get("updatedAt"))));
                        orders.add(cb.asc(root.get("updatedAt")));
                    }
                    case FAILED_FIRST -> {
                        orders.add(cb.desc(
                            cb.selectCase()
                                .when(cb.equal(root.get("status"), PostStatus.FAILED), 1)
                                .otherwise(0)
                        ));
                        orders.add(cb.desc(root.get("updatedAt")));
                    }
                    case MOST_PLATFORMS -> {
                        orders.add(cb.desc(cb.length(root.get("platforms"))));
                        orders.add(cb.desc(root.get("updatedAt")));
                    }
                    case NEWEST -> orders.add(cb.desc(root.get("updatedAt")));
                }
                query.orderBy(orders);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void buildSearchPredicate(
            jakarta.persistence.criteria.Root<Post> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            QueueFilterRequest filter,
            List<Predicate> predicates
    ) {
        if (filter.hasSearch()) {
            String token = "%" + filter.normalizedSearch().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), token),
                    cb.like(cb.lower(root.get("content")), token)
            ));
        }

        if (filter.hasTag()) {
            String tagToken = "%#" + filter.normalizedTag().toLowerCase() + "%";
            predicates.add(cb.like(cb.lower(root.get("content")), tagToken));
        }
    }

    private void buildPlatformPredicate(
            jakarta.persistence.criteria.Root<Post> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            QueueFilterRequest filter,
            List<Predicate> predicates
    ) {
        if (!filter.getPlatform().isEmpty() && !filter.getPlatform().contains(QueuePlatformFilter.ALL)) {
            List<Predicate> platformPredicates = new ArrayList<>();
            for (QueuePlatformFilter p : filter.getPlatform()) {
                String platform = p.name();
                Expression<String> normalizedPlatforms = cb.concat(cb.concat(",", root.get("platforms")), ",");
                platformPredicates.add(cb.like(normalizedPlatforms, "%," + platform + ",%"));
            }
            predicates.add(cb.or(platformPredicates.toArray(new Predicate[0])));
        }
    }

    private void buildTypePredicate(
            jakarta.persistence.criteria.Root<Post> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            QueueFilterRequest filter,
            List<Predicate> predicates
    ) {
        if (!filter.getType().isEmpty() && !filter.getType().contains(QueuePostTypeFilter.ALL)) {
            List<Predicate> typePredicates = new ArrayList<>();
            for (QueuePostTypeFilter t : filter.getType()) {
                switch (t) {
                    case IMAGE -> typePredicates.add(cb.isNotEmpty(root.get("media")));
                    case TEXT -> typePredicates.add(cb.isEmpty(root.get("media")));
                    case VIDEO -> {
                        Predicate mp4 = cb.like(cb.lower(root.get("content")), "%.mp4%");
                        Predicate mov = cb.like(cb.lower(root.get("content")), "%.mov%");
                        Predicate webm = cb.like(cb.lower(root.get("content")), "%.webm%");
                        typePredicates.add(cb.or(mp4, mov, webm));
                    }
                    case POLL -> {
                        Predicate withQuestion = cb.like(cb.lower(root.get("content")), "%?%");
                        Predicate withPollKeyword = cb.like(cb.lower(root.get("content")), "%poll%");
                        typePredicates.add(cb.or(withQuestion, withPollKeyword));
                    }
                }
            }
            predicates.add(cb.or(typePredicates.toArray(new Predicate[0])));
        }
    }

    private void buildDateRangePredicate(
            jakarta.persistence.criteria.Root<Post> root,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            QueueFilterRequest filter,
            List<Predicate> predicates
    ) {
        switch (filter.getDateRange()) {
            case TODAY -> predicates.add(buildDateRangePredicate(
                    cb.coalesce(root.<Instant>get("scheduledAt"), root.<Instant>get("updatedAt")),
                    cb,
                    0
            ));
            case WEEK -> predicates.add(buildDateRangePredicate(
                    cb.coalesce(root.<Instant>get("scheduledAt"), root.<Instant>get("updatedAt")),
                    cb,
                    DAYS_IN_WEEK
            ));
            case MONTH -> predicates.add(buildDateRangePredicate(
                    cb.coalesce(root.<Instant>get("scheduledAt"), root.<Instant>get("updatedAt")),
                    cb,
                    DAYS_IN_MONTH
            ));
            case ALL -> {
            }
        }
    }

    private Predicate buildDateRangePredicate(Expression<Instant> field, jakarta.persistence.criteria.CriteriaBuilder cb, int days) {
        LocalDate today = LocalDate.now();
        Instant from = days == 0
                ? today.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : today.minusDays(days - 1L).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return cb.greaterThanOrEqualTo(field, from);
    }
}
