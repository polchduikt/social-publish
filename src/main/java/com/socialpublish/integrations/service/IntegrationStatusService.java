package com.socialpublish.integrations.service;

import com.socialpublish.integrations.discord.dto.DiscordSettingsView;
import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.integrations.linkedin.dto.LinkedInSettingsView;
import com.socialpublish.integrations.linkedin.entity.LinkedInSettingsEntity;
import com.socialpublish.integrations.linkedin.repository.LinkedInSettingsRepository;
import com.socialpublish.integrations.notion.dto.NotionSettingsView;
import com.socialpublish.integrations.notion.entity.NotionSettingsEntity;
import com.socialpublish.integrations.notion.repository.NotionSettingsRepository;
import com.socialpublish.integrations.reddit.dto.RedditSettingsView;
import com.socialpublish.integrations.reddit.entity.RedditSettingsEntity;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import com.socialpublish.integrations.slack.dto.SlackSettingsView;
import com.socialpublish.integrations.slack.entity.SlackSettingsEntity;
import com.socialpublish.integrations.slack.repository.SlackSettingsRepository;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsView;
import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.integrations.dto.UserIntegrationsView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IntegrationStatusService {

    private final TelegramSettingsRepository telegramSettingsRepository;
    private final DiscordSettingsRepository discordSettingsRepository;
    private final RedditSettingsRepository redditSettingsRepository;
    private final SlackSettingsRepository slackSettingsRepository;
    private final NotionSettingsRepository notionSettingsRepository;
    private final LinkedInSettingsRepository linkedinSettingsRepository;

    public boolean isTelegramConnected(UUID userId) {
        return telegramSettingsRepository.findByUserId(userId)
                .map(settings -> settings.isEnabled()
                        && settings.getBotToken() != null
                        && !settings.getBotToken().isBlank())
                .orElse(false);
    }

    public TelegramSettingsView getTelegramView(UUID userId) {
        return telegramSettingsRepository.findByUserId(userId)
                .map(this::toTelegramView)
                .orElse(new TelegramSettingsView("", "", false, false));
    }

    public boolean isDiscordConnected(UUID userId) {
        return discordSettingsRepository.findByUserId(userId)
                .map(settings -> settings.isEnabled()
                        && settings.getWebhookUrl() != null
                        && !settings.getWebhookUrl().isBlank())
                .orElse(false);
    }

    public DiscordSettingsView getDiscordView(UUID userId) {
        return discordSettingsRepository.findByUserId(userId)
                .map(this::toDiscordView)
                .orElse(new DiscordSettingsView(false, false, ""));
    }

    public boolean isRedditConnected(UUID userId) {
        return redditSettingsRepository.findByUserId(userId)
                .map(settings -> settings.isEnabled()
                        && settings.getRefreshToken() != null
                        && !settings.getRefreshToken().isBlank())
                .orElse(false);
    }

    public RedditSettingsView getRedditView(UUID userId) {
        return redditSettingsRepository.findByUserId(userId)
                .map(this::toRedditView)
                .orElse(new RedditSettingsView(false, false, ""));
    }

    public boolean isSlackConnected(UUID userId) {
        return slackSettingsRepository.findByUserId(userId)
                .map(settings -> settings.isEnabled()
                        && settings.getWebhookUrl() != null
                        && !settings.getWebhookUrl().isBlank())
                .orElse(false);
    }

    public SlackSettingsView getSlackView(UUID userId) {
        return slackSettingsRepository.findByUserId(userId)
                .map(this::toSlackView)
                .orElse(new SlackSettingsView(false, false, ""));
    }

    public boolean isNotionConnected(UUID userId) {
        return notionSettingsRepository.findByUserId(userId)
                .map(settings -> settings.isEnabled()
                        && settings.getApiToken() != null
                        && !settings.getApiToken().isBlank())
                .orElse(false);
    }

    public NotionSettingsView getNotionView(UUID userId) {
        return notionSettingsRepository.findByUserId(userId)
                .map(this::toNotionView)
                .orElse(NotionSettingsView.builder().configured(false).enabled(false).build());
    }

    public boolean isLinkedInConnected(UUID userId) {
        return linkedinSettingsRepository.findByUserId(userId)
                .map(settings -> settings.isEnabled()
                        && settings.getAccessToken() != null
                        && !settings.getAccessToken().isBlank())
                .orElse(false);
    }

    public LinkedInSettingsView getLinkedInView(UUID userId) {
        return linkedinSettingsRepository.findByUserId(userId)
                .map(this::toLinkedInView)
                .orElse(LinkedInSettingsView.builder().configured(false).enabled(false).build());
    }

    public UserIntegrationsView getAllStatuses(UUID userId) {
        return UserIntegrationsView.builder()
                .telegram(getTelegramView(userId))
                .discord(getDiscordView(userId))
                .reddit(getRedditView(userId))
                .slack(getSlackView(userId))
                .notion(getNotionView(userId))
                .linkedin(getLinkedInView(userId))
                .build();
    }

    private TelegramSettingsView toTelegramView(TelegramSettingsEntity entity) {
        return new TelegramSettingsView(
                maskToken(entity.getBotToken()),
                maskToken(entity.getChatId()),
                entity.isEnabled(),
                true
        );
    }

    private DiscordSettingsView toDiscordView(DiscordSettingsEntity entity) {
        return new DiscordSettingsView(true, entity.isEnabled(), maskWebhook(entity.getWebhookUrl()));
    }

    private RedditSettingsView toRedditView(RedditSettingsEntity entity) {
        boolean configured = entity.getRefreshToken() != null && !entity.getRefreshToken().isBlank();
        String subreddit = entity.getDefaultSubreddit() == null ? "" : entity.getDefaultSubreddit();
        return new RedditSettingsView(configured, entity.isEnabled(), subreddit);
    }

    private SlackSettingsView toSlackView(SlackSettingsEntity entity) {
        return new SlackSettingsView(true, entity.isEnabled(), maskWebhook(entity.getWebhookUrl()));
    }

    private NotionSettingsView toNotionView(NotionSettingsEntity entity) {
        return NotionSettingsView.builder()
                .id(entity.getId())
                .apiToken(maskToken(entity.getApiToken()))
                .databaseId(entity.getDatabaseId())
                .enabled(entity.isEnabled())
                .configured(true)
                .build();
    }

    private LinkedInSettingsView toLinkedInView(LinkedInSettingsEntity entity) {
        return LinkedInSettingsView.builder()
                .id(entity.getId())
                .accessToken(maskLinkedInToken(entity.getAccessToken()))
                .authorUrn(entity.getAuthorUrn())
                .expiresAt(entity.getExpiresAt())
                .enabled(entity.isEnabled())
                .configured(true)
                .build();
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return token == null ? "" : token;
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }

    private String maskWebhook(String url) {
        if (url == null || url.length() < 20) {
            return url == null ? "" : url;
        }
        return url.substring(0, 15) + "..." + url.substring(url.length() - 5);
    }

    private String maskLinkedInToken(String token) {
        if (token == null || token.length() < 10) {
            return "***";
        }
        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }
}
