package com.socialpublish.scheduling.jobs;

import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.dashboard.dto.DashboardStatsView;
import com.socialpublish.dashboard.service.DashboardStatsBuilder;
import com.socialpublish.mail.service.EmailService;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.posts.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class WeeklyStatsJob implements Job {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final DashboardStatsBuilder statsBuilder;
    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            .withZone(ZoneId.systemDefault());

    public WeeklyStatsJob(
            UserRepository userRepository,
            PostRepository postRepository,
            DashboardStatsBuilder statsBuilder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.statsBuilder = statsBuilder;
        this.emailService = emailService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting WeeklyStatsJob...");
        List<User> users = userRepository.findByEmailNotificationsEnabledTrue();
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        String period = DATE_FORMATTER.format(sevenDaysAgo) + " - " + DATE_FORMATTER.format(Instant.now());
        List<Post> posts = postRepository.findByUpdatedAtAfter(sevenDaysAgo);
        Map<UUID, List<Post>> postsByOwner = posts.stream()
                .filter(post -> post.getOwner() != null)
                .collect(Collectors.groupingBy(post -> post.getOwner().getId()));

        for (User user : users) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }

            try {
                List<Post> userPosts = postsByOwner.getOrDefault(user.getId(), List.of());
                DashboardStatsView stats = statsBuilder.build(userPosts);
                if (stats.totalPosts() == 0) {
                    continue;
                }

                Map<String, Object> variables = new HashMap<>();
                variables.put("stats", stats);
                variables.put("period", period);
                variables.put("userName", user.getFullName());

                emailService.sendHtmlMessage(
                        user.getEmail(),
                        "📊 Your Weekly Social Publish Report",
                        "weekly-stats",
                        variables
                );
                log.info("Weekly stats sent to user {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send weekly stats to user {}", user.getId(), e);
            }
        }
        log.info("WeeklyStatsJob finished.");
    }
}
