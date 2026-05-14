package com.socialpublish.posts.service;

import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.repository.PostRepository;
import com.socialpublish.scheduling.service.PostSchedulerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringPostService {

    private final PostRepository postRepository;
    private final PostSchedulerService postSchedulerService;
    private final PostMediaSyncService postMediaSyncService;

    @Transactional
    public Optional<Post> createNextOccurrence(Post publishedPost) {
        if (!publishedPost.isRecurring()) {
            return Optional.empty();
        }

        Instant nextAt = calculateNextOccurrence(
                publishedPost.getRecurringDays(),
                publishedPost.getRecurringTime(),
                publishedPost.getScheduledAt()
        );

        if (nextAt == null) {
            log.info("No valid next occurrence for recurring post {}", publishedPost.getId());
            return Optional.empty();
        }
        if (publishedPost.getRecurringEndDate() != null && nextAt.isAfter(publishedPost.getRecurringEndDate())) {
            log.info("Recurring post {} reached end date, stopping recurrence", publishedPost.getId());
            return Optional.empty();
        }

        Post next = new Post();
        next.setOwner(publishedPost.getOwner());
        next.setTitle(publishedPost.getTitle());
        next.setContent(publishedPost.getContent());
        next.setPlatforms(publishedPost.getPlatforms());
        next.setStatus(PostStatus.SCHEDULED);
        next.setScheduledAt(nextAt);
        next.setRecurring(true);
        next.setRecurringDays(publishedPost.getRecurringDays());
        next.setRecurringTime(publishedPost.getRecurringTime());
        next.setRecurringEndDate(publishedPost.getRecurringEndDate());
        next.setMaxRetries(publishedPost.getMaxRetries());

        next.setParentRecurringId(
                publishedPost.getParentRecurringId() != null
                        ? publishedPost.getParentRecurringId()
                        : publishedPost.getId()
        );

        Post saved = postRepository.save(next);
        postMediaSyncService.copyMedia(publishedPost, saved, publishedPost.getOwner().getId());
        saved = postRepository.save(saved);
        postSchedulerService.schedulePost(saved);
        log.info("Created next recurring occurrence {} scheduled at {} for parent {}",
                saved.getId(), nextAt, saved.getParentRecurringId());
        return Optional.of(saved);
    }

    public Instant calculateNextOccurrence(String recurringDays, String recurringTime, Instant afterInstant) {
        if (recurringDays == null || recurringDays.isBlank() || recurringTime == null || recurringTime.isBlank()) {
            return null;
        }
        Set<DayOfWeek> days = parseDays(recurringDays);
        if (days.isEmpty()) {
            return null;
        }
        LocalTime time = parseTime(recurringTime);
        if (time == null) {
            return null;
        }
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime afterLdt = afterInstant.atZone(zone).toLocalDateTime();
        LocalDate startDate = afterLdt.toLocalDate();
        LocalDateTime reference = afterLdt.plusMinutes(1);
        LocalDate checkDate = reference.toLocalDate();
        for (int i = 0; i < 8; i++) {
            LocalDate candidate = checkDate.plusDays(i);
            if (days.contains(candidate.getDayOfWeek())) {
                LocalDateTime candidateDt = LocalDateTime.of(candidate, time);
                if (candidateDt.isAfter(afterLdt)) {
                    return candidateDt.atZone(zone).toInstant();
                }
            }
        }

        return null;
    }

    public Instant calculateFirstOccurrence(String recurringDays, String recurringTime) {
        return calculateNextOccurrence(recurringDays, recurringTime, Instant.now().minusSeconds(60));
    }

    private Set<DayOfWeek> parseDays(String recurringDays) {
        return Arrays.stream(recurringDays.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try {
                        return DayOfWeek.valueOf(s.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        return switch (s.toUpperCase(Locale.ROOT)) {
                            case "MON" -> DayOfWeek.MONDAY;
                            case "TUE" -> DayOfWeek.TUESDAY;
                            case "WED" -> DayOfWeek.WEDNESDAY;
                            case "THU" -> DayOfWeek.THURSDAY;
                            case "FRI" -> DayOfWeek.FRIDAY;
                            case "SAT" -> DayOfWeek.SATURDAY;
                            case "SUN" -> DayOfWeek.SUNDAY;
                            default -> null;
                        };
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private LocalTime parseTime(String timeStr) {
        try {
            return LocalTime.parse(timeStr);
        } catch (Exception e) {
            log.warn("Invalid recurring time format: {}", timeStr);
            return null;
        }
    }
}
