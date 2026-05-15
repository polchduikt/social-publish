package com.socialpublish.aiassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpublish.aiassistant.config.AiAssistantProperties;
import com.socialpublish.aiassistant.dto.AiAssistantChatRequest;
import com.socialpublish.aiassistant.dto.AiAssistantChatResponse;
import com.socialpublish.aiassistant.provider.AiAssistantProvider;
import com.socialpublish.aiassistant.provider.AiProviderException;
import com.socialpublish.dashboard.dto.DashboardStatsView;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.dashboard.service.DashboardService;
import com.socialpublish.posts.dto.PostView;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AiAssistantService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int RECENT_POSTS_FOR_CONTEXT = 5;

    private final DashboardService dashboardService;
    private final Map<String, AiAssistantProvider> providers;
    private final AiAssistantProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiAssistantService(
            DashboardService dashboardService,
            List<AiAssistantProvider> providers,
            AiAssistantProperties properties
    ) {
        this.dashboardService = dashboardService;
        this.providers = providers.stream().collect(Collectors.toMap(p -> p.name().toLowerCase(), p -> p));
        this.properties = properties;
    }

    public AiAssistantChatResponse chat(UUID ownerId, AiAssistantChatRequest request) {
        DashboardView dashboard = dashboardService.buildDashboard(ownerId);
        String userPrompt = buildUserPrompt(request, dashboard);
        String systemPrompt = properties.getSystemPrompt();
        String providerName = properties.getProvider() == null ? "groq" : properties.getProvider().toLowerCase();
        AiAssistantProvider provider = providers.get(providerName);

        if (provider == null) {
            throw new AiProviderException("AI provider is not supported: " + providerName);
        }

        String rawResponse = provider.complete(systemPrompt, userPrompt);
        ParsedResponse parsed = parseResponse(rawResponse);

        String generatedText = parsed.generatedText == null ? "" : parsed.generatedText.trim();
        String reply;
        if (parsed.reply == null || parsed.reply.isBlank()) {
            reply = generatedText.isBlank()
                    ? "Could not build a clear response. Please rephrase your request."
                    : "";
        } else {
            reply = parsed.reply.trim();
        }
        boolean needsPlacementChoice = parsed.needsPlacementChoice || !generatedText.isBlank();

        return new AiAssistantChatResponse(reply, generatedText, needsPlacementChoice);
    }

    private String buildUserPrompt(AiAssistantChatRequest request, DashboardView dashboard) {
        StringBuilder prompt = new StringBuilder();
        String userMessage = request.message().trim();
        prompt.append("User language hint: ")
                .append(looksUkrainian(userMessage) ? "uk" : "en")
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

    private boolean looksUkrainian(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.UnicodeBlock.of(value.charAt(i)) == Character.UnicodeBlock.CYRILLIC) {
                return true;
            }
        }
        return false;
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
        if (trimmed.length() <= 120) {
            return trimmed;
        }
        return trimmed.substring(0, 117) + "...";
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATETIME_FORMATTER);
    }

    private ParsedResponse parseResponse(String rawResponse) {
        String cleaned = stripCodeFences(rawResponse == null ? "" : rawResponse.trim());
        if (cleaned.isBlank()) {
            return new ParsedResponse("", "", false);
        }

        try {
            JsonNode root = objectMapper.readTree(cleaned);
            String reply = textOrEmpty(root.get("reply"));
            String generatedText = textOrEmpty(root.get("generatedText"));
            boolean needsPlacementChoice = root.path("needsPlacementChoice").asBoolean(false);
            return new ParsedResponse(reply, generatedText, needsPlacementChoice);
        } catch (Exception ignored) {
            return new ParsedResponse(cleaned, "", false);
        }
    }

    private String stripCodeFences(String value) {
        if (value.startsWith("```") && value.endsWith("```")) {
            String noStart = value.replaceFirst("^```[a-zA-Z]*\\s*", "");
            return noStart.replaceFirst("\\s*```$", "");
        }
        return value;
    }

    private String textOrEmpty(JsonNode node) {
        if (node == null || node.isNull()) {
            return "";
        }
        return node.asText("");
    }

    private record ParsedResponse(String reply, String generatedText, boolean needsPlacementChoice) {
    }
}
