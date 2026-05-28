package com.socialpublish.aiassistant.service;

import com.socialpublish.aiassistant.dto.AiAssistantChatRequest;
import com.socialpublish.dashboard.dto.DashboardStatsView;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.posts.dto.PostView;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class PromptBuilder {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int RECENT_POSTS_FOR_CONTEXT = 5;
    private static final int MAX_PREVIEW_LENGTH = 120;
    private static final int TRUNCATION_LENGTH = MAX_PREVIEW_LENGTH - 3;

    private final LanguageDetector languageDetector;

    public PromptBuilder(LanguageDetector languageDetector) {
        this.languageDetector = languageDetector;
    }

    public String buildUserPrompt(AiAssistantChatRequest request, DashboardView dashboard) {
        StringBuilder prompt = new StringBuilder();
        String userMessage = request.message().trim();
        prompt.append("User language hint: ")
                .append(languageDetector.looksUkrainian(userMessage) ? "uk" : "en")
                .append('\n');
        prompt.append("User request:\n").append(userMessage).append("\n\n");

        if (request.currentPostMessage() != null && !request.currentPostMessage().isBlank()) {
            prompt.append("Current Post Creator Message field text:\n")
                    .append(request.currentPostMessage().trim())
                    .append("\n\n");
        }

        prompt.append("Dashboard statistics context:\n")
                .append(buildStatsSummary(dashboard));

        return prompt.toString();
    }

    private String buildStatsSummary(DashboardView dashboard) {
        DashboardStatsView stats = dashboard.stats();
        StringBuilder out = new StringBuilder();
        out.append("- Total posts: ").append(stats.totalPosts()).append('\n')
                .append("- Draft: ").append(stats.draftPosts()).append('\n')
                .append("- Scheduled: ").append(stats.scheduledPosts()).append('\n')
                .append("- Publishing: ").append(stats.publishingPosts()).append('\n')
                .append("- Published: ").append(stats.publishedPosts()).append('\n')
                .append("- Retrying: ").append(stats.retryingPosts()).append('\n')
                .append("- Failed: ").append(stats.failedPosts()).append('\n')
                .append("- Cancelled: ").append(stats.cancelledPosts()).append('\n')
                .append("- Success rate: ").append(stats.successRate()).append("%\n");

        if (dashboard.nextPublish() != null) {
            out.append("- Next publish: ")
                    .append(formatDateTime(dashboard.nextPublish().scheduledAt()))
                    .append(" | overdue=")
                    .append(dashboard.nextPublish().overdue())
                    .append(" | countdown=")
                    .append(dashboard.nextPublish().countdownValue())
                    .append(" | platforms=")
                    .append(String.join(", ", dashboard.nextPublish().platforms()))
                    .append('\n');
        }

        List<PostView> recentPosts = dashboard.recentPosts();
        if (recentPosts != null && !recentPosts.isEmpty()) {
            out.append("- Recent posts:\n");
            recentPosts.stream()
                    .limit(RECENT_POSTS_FOR_CONTEXT)
                    .forEach(post -> out.append("  - [")
                            .append(post.status())
                            .append("] ")
                            .append(safeText(post.title()))
                            .append(" | ")
                            .append(post.platformList() == null ? "-" : String.join(", ", post.platformList()))
                            .append(" | updated ")
                            .append(formatDateTime(post.updatedAt()))
                            .append('\n'));
        }

        return out.toString();
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "Untitled";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= MAX_PREVIEW_LENGTH) {
            return trimmed;
        }
        return trimmed.substring(0, TRUNCATION_LENGTH) + "...";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATETIME_FORMATTER);
    }
}
