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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WeeklyStatsJob implements Job {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private DashboardStatsBuilder statsBuilder;

    @Autowired
    private EmailService emailService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy")
            .withZone(ZoneId.systemDefault());

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.info("Starting WeeklyStatsJob...");
        
        List<User> users = userRepository.findAll();
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        String period = DATE_FORMATTER.format(sevenDaysAgo) + " - " + DATE_FORMATTER.format(Instant.now());

        for (User user : users) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }

            if (!user.isEmailNotificationsEnabled()) {
                continue;
            }

            try {
                List<Post> userPosts = postRepository.findByOwnerIdAndUpdatedAtAfter(user.getId(), sevenDaysAgo);
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
