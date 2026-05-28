package com.socialpublish.posts.service;

import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.exception.PostValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PostLifecyclePolicy {

    private final PostStatusMachine statusMachine;
    private final RecurringPostService recurringPostService;
    private final PostTitleGenerator postTitleGenerator;

    public PostStatus resolveRequestedStatus(PostUpsertRequest request, PostStatus fallback) {
        return request.getStatus() == null || request.getStatus().isBlank()
                ? fallback
                : PostStatus.valueOf(request.getStatus());
    }

    public void applyCommonFields(Post post, PostUpsertRequest request) {
        String content = request.getContent() == null ? "" : request.getContent().trim();
        post.setTitle(postTitleGenerator.fromContent(content));
        post.setContent(content);

        List<String> platforms = request.getPlatforms();
        post.setPlatforms(platforms == null || platforms.isEmpty() ? "" : String.join(",", platforms));
        post.setRecurring(request.isRecurring());
        if (request.isRecurring()) {
            List<String> days = request.getRecurringDays();
            post.setRecurringDays(days == null || days.isEmpty() ? null : String.join(",", days));
            post.setRecurringTime(request.getRecurringTime());
            post.setRecurringEndDate(request.getRecurringEndDate() != null
                    ? request.getRecurringEndDate().atZone(ZoneId.systemDefault()).toInstant()
                    : null);
        } else {
            post.setRecurringDays(null);
            post.setRecurringTime(null);
            post.setRecurringEndDate(null);
        }

        post.setSilentMode(request.isSilentMode());
        post.setInlineButtons(request.getInlineButtons());
        post.setPollQuestion(request.getPollQuestion());
        post.setPollOptions(request.getPollOptions());
        post.setPollMultipleAnswers(request.isPollMultipleAnswers());
        post.setPollIsQuiz(request.isPollIsQuiz());
        post.setPollCorrectOptionId(request.getPollCorrectOptionId());
    }

    public void applyUserTransition(Post post, PostStatus target, PostUpsertRequest request) {
        if (!PostStatus.userSettable().contains(target)) {
            throw new PostValidationException("Cannot manually set status: " + target);
        }
        statusMachine.transition(post, target);
        switch (target) {
            case DRAFT -> applyDraftFields(post);
            case SCHEDULED -> applyScheduledFields(post, request);
            case CANCELLED -> applyCancelledFields(post);
            default -> { }
        }
    }

    public void moveToDraft(Post post) {
        PostStatus oldStatus = post.getStatus();
        if (oldStatus == PostStatus.DRAFT) {
            return;
        }
        if (oldStatus == PostStatus.SCHEDULED || oldStatus == PostStatus.FAILED || oldStatus == PostStatus.CANCELLED) {
            statusMachine.transition(post, PostStatus.DRAFT);
            applyDraftFields(post);
            return;
        }
        throw new PostValidationException("Cannot move status " + oldStatus + " to draft");
    }

    public void requireRetryable(Post post) {
        if (post.getStatus() != PostStatus.FAILED) {
            throw new PostValidationException("Retry is available only for FAILED posts");
        }
    }

    public void reschedule(Post post, PostUpsertRequest request) {
        PostStatus oldStatus = post.getStatus();
        if (oldStatus == PostStatus.SCHEDULED) {
            applyScheduledFields(post, request);
        } else if (oldStatus == PostStatus.DRAFT) {
            applyUserTransition(post, PostStatus.SCHEDULED, request);
        } else if (oldStatus == PostStatus.FAILED || oldStatus == PostStatus.CANCELLED) {
            statusMachine.transition(post, PostStatus.DRAFT);
            applyUserTransition(post, PostStatus.SCHEDULED, request);
        } else {
            throw new PostValidationException("Cannot reschedule post with status " + oldStatus);
        }
    }

    public void prepareForImmediatePublish(Post post, PostUpsertRequest request) {
        if (post.getPlatforms() == null || post.getPlatforms().isBlank()) {
            throw new PostValidationException("Select at least one platform for publishing");
        }

        request.setScheduledAt(LocalDateTime.now());

        PostStatus current = post.getStatus();
        if (current == PostStatus.FAILED || current == PostStatus.CANCELLED) {
            statusMachine.transition(post, PostStatus.DRAFT);
        }

        if (post.getStatus() == PostStatus.SCHEDULED) {
            applyScheduledFields(post, request);
        } else if (post.getStatus() == PostStatus.DRAFT) {
            applyUserTransition(post, PostStatus.SCHEDULED, request);
        } else {
            throw new PostValidationException("Cannot publish now from status " + post.getStatus());
        }

        statusMachine.transition(post, PostStatus.PUBLISHING);
        post.setRetryCount(0);
        post.setFailedReason(null);
        post.setPublishedAt(null);
    }

    public void applyScheduledFields(Post post, PostUpsertRequest request) {
        if (post.isRecurring()) {
            String days = post.getRecurringDays();
            String time = post.getRecurringTime();
            if (days == null || days.isBlank() || time == null || time.isBlank()) {
                throw new PostValidationException("Recurring days and time are required");
            }
            Instant nextAt = recurringPostService.calculateFirstOccurrence(days, time);
            if (nextAt == null) {
                throw new PostValidationException("Could not calculate next recurring date");
            }
            post.setScheduledAt(nextAt);
        } else {
            if (request.getScheduledAt() == null) {
                throw new PostValidationException("Scheduled date is required for SCHEDULED posts");
            }
            post.setScheduledAt(request.getScheduledAt().atZone(ZoneId.systemDefault()).toInstant());
        }
        if (post.getPlatforms() == null || post.getPlatforms().isBlank()) {
            throw new PostValidationException("Select at least one platform for SCHEDULED posts");
        }
        post.setPublishedAt(null);
        post.setFailedReason(null);
        post.setRetryCount(0);
    }

    private void applyDraftFields(Post post) {
        post.setScheduledAt(null);
        post.setPublishedAt(null);
        post.setFailedReason(null);
        post.setRetryCount(0);
    }

    private void applyCancelledFields(Post post) {
        post.setScheduledAt(null);
        post.setPublishedAt(null);
        post.setFailedReason(null);
        post.setRetryCount(0);
    }
}
