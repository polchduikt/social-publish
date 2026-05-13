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
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
        List<TelegramSettingsEntity> accounts = telegramSettingsRepository.findAllByUserId(userId);
        return accounts.stream().anyMatch(settings -> settings.isEnabled()
                && settings.getBotToken() != null
                && !settings.getBotToken().isBlank());
    }

    public TelegramSettingsView getTelegramView(UUID userId) {
        List<TelegramSettingsEntity> accounts = telegramSettingsRepository.findAllByUserId(userId);
        return toTelegramView(accounts);
    }

    public boolean isDiscordConnected(UUID userId) {
        List<DiscordSettingsEntity> accounts = discordSettingsRepository.findAllByUserId(userId);
        return accounts.stream().anyMatch(settings -> settings.isEnabled()
                && settings.getWebhookUrl() != null
                && !settings.getWebhookUrl().isBlank());
    }

    public DiscordSettingsView getDiscordView(UUID userId) {
        List<DiscordSettingsEntity> accounts = discordSettingsRepository.findAllByUserId(userId);
        return toDiscordView(accounts);
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
                .orElse(new RedditSettingsView(false, false, "", ""));
    }

    public boolean isSlackConnected(UUID userId) {
        List<SlackSettingsEntity> accounts = slackSettingsRepository.findAllByUserId(userId);
        return accounts.stream().anyMatch(settings -> settings.isEnabled()
                && settings.getWebhookUrl() != null
                && !settings.getWebhookUrl().isBlank());
    }

    public SlackSettingsView getSlackView(UUID userId) {
        List<SlackSettingsEntity> accounts = slackSettingsRepository.findAllByUserId(userId);
        return toSlackView(accounts);
    }

    public boolean isNotionConnected(UUID userId) {
        List<NotionSettingsEntity> accounts = notionSettingsRepository.findAllByUserId(userId);
        return accounts.stream().anyMatch(settings -> settings.isEnabled()
                && settings.getApiToken() != null
                && !settings.getApiToken().isBlank());
    }

    public NotionSettingsView getNotionView(UUID userId) {
        List<NotionSettingsEntity> accounts = notionSettingsRepository.findAllByUserId(userId);
        return toNotionView(accounts);
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

    private TelegramSettingsView toTelegramView(List<TelegramSettingsEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new TelegramSettingsView(Collections.emptyList(), false, false);
        }
        
        List<TelegramSettingsView.TelegramAccountView> accounts = entities.stream()
                .map(entity -> new TelegramSettingsView.TelegramAccountView(
                        entity.getId(),
                        maskToken(entity.getBotToken()),
                        maskToken(entity.getChatId()),
                        entity.getLabel() == null ? "" : entity.getLabel(),
                        entity.isEnabled()
                )).collect(Collectors.toList());
                
        boolean enabled = accounts.stream().anyMatch(TelegramSettingsView.TelegramAccountView::enabled);
        return new TelegramSettingsView(accounts, true, enabled);
    }

    private DiscordSettingsView toDiscordView(List<DiscordSettingsEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new DiscordSettingsView(Collections.emptyList(), false, false);
        }
        
        List<DiscordSettingsView.DiscordAccountView> accounts = entities.stream()
                .map(entity -> new DiscordSettingsView.DiscordAccountView(
                        entity.getId(),
                        maskWebhook(entity.getWebhookUrl()),
                        entity.getLabel() == null ? "" : entity.getLabel(),
                        entity.isEnabled()
                )).collect(Collectors.toList());
                
        boolean enabled = accounts.stream().anyMatch(DiscordSettingsView.DiscordAccountView::enabled);
        return new DiscordSettingsView(accounts, true, enabled);
    }

    private RedditSettingsView toRedditView(RedditSettingsEntity entity) {
        boolean configured = entity.getRefreshToken() != null && !entity.getRefreshToken().isBlank();
        String subreddit = entity.getDefaultSubreddit() == null ? "" : entity.getDefaultSubreddit();
        return new RedditSettingsView(
                configured,
                entity.isEnabled(),
                subreddit,
                entity.getLabel() == null ? "" : entity.getLabel()
        );
    }

    private SlackSettingsView toSlackView(List<SlackSettingsEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new SlackSettingsView(Collections.emptyList(), false, false);
        }
        
        List<SlackSettingsView.SlackAccountView> accounts = entities.stream()
                .map(entity -> new SlackSettingsView.SlackAccountView(
                        entity.getId(),
                        maskWebhook(entity.getWebhookUrl()),
                        entity.getLabel() == null ? "" : entity.getLabel(),
                        entity.isEnabled()
                )).collect(Collectors.toList());
                
        boolean enabled = accounts.stream().anyMatch(SlackSettingsView.SlackAccountView::enabled);
        return new SlackSettingsView(accounts, true, enabled);
    }

    private NotionSettingsView toNotionView(List<NotionSettingsEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new NotionSettingsView(Collections.emptyList(), false, false);
        }
        
        List<NotionSettingsView.NotionAccountView> accounts = entities.stream()
                .map(entity -> new NotionSettingsView.NotionAccountView(
                        entity.getId(),
                        maskToken(entity.getApiToken()),
                        entity.getDatabaseId(),
                        entity.getLabel() == null ? "" : entity.getLabel(),
                        entity.isEnabled()
                )).collect(Collectors.toList());
                
        boolean enabled = accounts.stream().anyMatch(NotionSettingsView.NotionAccountView::enabled);
        return new NotionSettingsView(accounts, true, enabled);
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
