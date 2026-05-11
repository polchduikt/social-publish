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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QueueService {

    private static final int LOAD_MORE_STEP = 10;

    private final PostRepository postRepository;
    private final PostService postService;
    private final PostMapper postMapper;

    @Transactional(readOnly = true)
    public QueuePageView getQueue(UUID ownerId, QueueFilterRequest filter) {
        Specification<Post> specification = buildSpecification(ownerId, filter);
        List<Post> filteredPosts = new ArrayList<>(postRepository.findAll(specification));
        sortPostsInMemory(filteredPosts, filter.getSort());

        long totalFiltered = filteredPosts.size();
        List<PostView> posts = filteredPosts.stream()
                .limit(filter.getSize())
                .map(postMapper::toView)
                .toList();
        boolean hasMore = totalFiltered > posts.size();
        int nextSize = Math.min(filter.getSize() + LOAD_MORE_STEP, 100);

        QueueStatsView stats = new QueueStatsView(
                postRepository.countByOwnerIdAndStatus(ownerId, PostStatus.SCHEDULED),
                postRepository.countByOwnerIdAndStatus(ownerId, PostStatus.PUBLISHED),
                postRepository.countByOwnerIdAndStatus(ownerId, PostStatus.FAILED),
                postRepository.countByOwnerIdAndStatus(ownerId, PostStatus.DRAFT)
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

            if (filter.getPlatform() != QueuePlatformFilter.ALL) {
                String platform = filter.getPlatform().name();
                Expression<String> normalizedPlatforms = cb.concat(cb.concat(",", root.get("platforms")), ",");
                predicates.add(cb.like(normalizedPlatforms, "%," + platform + ",%"));
            }

            switch (filter.getType()) {
                case IMAGE -> predicates.add(cb.isNotEmpty(root.get("media")));
                case TEXT -> predicates.add(cb.isEmpty(root.get("media")));
                case VIDEO -> {
                    Predicate mp4 = cb.like(cb.lower(root.get("content")), "%.mp4%");
                    Predicate mov = cb.like(cb.lower(root.get("content")), "%.mov%");
                    Predicate webm = cb.like(cb.lower(root.get("content")), "%.webm%");
                    predicates.add(cb.or(mp4, mov, webm));
                }
                case POLL -> {
                    Predicate withQuestion = cb.like(cb.lower(root.get("content")), "%?%");
                    Predicate withPollKeyword = cb.like(cb.lower(root.get("content")), "%poll%");
                    predicates.add(cb.or(withQuestion, withPollKeyword));
                }
                case ALL -> {
                }
            }

            switch (filter.getDateRange()) {
                case TODAY -> predicates.add(buildDateRangePredicate(
                        cb.coalesce(root.<Instant>get("scheduledAt"), root.<Instant>get("updatedAt")),
                        cb,
                        0
                ));
                case WEEK -> predicates.add(buildDateRangePredicate(
                        cb.coalesce(root.<Instant>get("scheduledAt"), root.<Instant>get("updatedAt")),
                        cb,
                        7
                ));
                case MONTH -> predicates.add(buildDateRangePredicate(
                        cb.coalesce(root.<Instant>get("scheduledAt"), root.<Instant>get("updatedAt")),
                        cb,
                        30
                ));
                case ALL -> {
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Predicate buildDateRangePredicate(Expression<Instant> field, jakarta.persistence.criteria.CriteriaBuilder cb, int days) {
        LocalDate today = LocalDate.now();
        Instant from = days == 0
                ? today.atStartOfDay(ZoneId.systemDefault()).toInstant()
                : today.minusDays(days - 1L).atStartOfDay(ZoneId.systemDefault()).toInstant();
        return cb.greaterThanOrEqualTo(field, from);
    }

    private void sortPostsInMemory(List<Post> posts, QueueSortOption sortOption) {
        Comparator<Post> newest = Comparator.comparing(Post::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        Comparator<Post> oldest = Comparator.comparing(Post::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Post> scheduledSoon = Comparator
                .comparing(Post::getScheduledAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(oldest);
        Comparator<Post> failedFirst = Comparator
                .comparing((Post post) -> post.getStatus() != PostStatus.FAILED)
                .thenComparing(newest);
        Comparator<Post> mostPlatforms = Comparator
                .comparingInt((Post post) -> countPlatforms(post.getPlatforms()))
                .reversed()
                .thenComparing(newest);

        switch (sortOption) {
            case OLDEST -> posts.sort(oldest);
            case SCHEDULED_SOON -> posts.sort(scheduledSoon);
            case FAILED_FIRST -> posts.sort(failedFirst);
            case MOST_PLATFORMS -> posts.sort(mostPlatforms);
            case NEWEST -> posts.sort(newest);
        }
    }

    private int countPlatforms(String platforms) {
        if (platforms == null || platforms.isBlank()) {
            return 0;
        }
        return (int) java.util.Arrays.stream(platforms.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .count();
    }
}
