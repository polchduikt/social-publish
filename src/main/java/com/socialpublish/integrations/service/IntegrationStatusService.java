package com.socialpublish.integrations.service;

import com.socialpublish.integrations.discord.dto.DiscordSettingsView;
import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.integrations.linkedin.dto.LinkedInSettingsView;
import com.socialpublish.integrations.linkedin.repository.LinkedInSettingsRepository;
import com.socialpublish.integrations.notion.dto.NotionSettingsView;
import com.socialpublish.integrations.notion.entity.NotionSettingsEntity;
import com.socialpublish.integrations.notion.repository.NotionSettingsRepository;
import com.socialpublish.integrations.reddit.dto.RedditSettingsView;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import com.socialpublish.integrations.slack.dto.SlackSettingsView;
import com.socialpublish.integrations.slack.entity.SlackSettingsEntity;
import com.socialpublish.integrations.slack.repository.SlackSettingsRepository;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsView;
import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.integrations.dto.UserIntegrationsView;
import com.socialpublish.integrations.mapper.IntegrationSettingsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final IntegrationSettingsMapper integrationSettingsMapper;

    public boolean isTelegramConnected(UUID userId) {
        List<TelegramSettingsEntity> accounts = telegramSettingsRepository.findAllByUserId(userId);
        return accounts.stream().anyMatch(settings -> settings.isEnabled()
                && settings.getBotToken() != null
                && !settings.getBotToken().isBlank());
    }

    public TelegramSettingsView getTelegramView(UUID userId) {
        return integrationSettingsMapper.toTelegramView(telegramSettingsRepository.findAllByUserId(userId));
    }

    public boolean isDiscordConnected(UUID userId) {
        List<DiscordSettingsEntity> accounts = discordSettingsRepository.findAllByUserId(userId);
        return accounts.stream().anyMatch(settings -> settings.isEnabled()
                && settings.getWebhookUrl() != null
                && !settings.getWebhookUrl().isBlank());
    }

    public DiscordSettingsView getDiscordView(UUID userId) {
        return integrationSettingsMapper.toDiscordView(discordSettingsRepository.findAllByUserId(userId));
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
                .map(integrationSettingsMapper::toRedditView)
                .orElse(new RedditSettingsView(false, false, "", ""));
    }

    public boolean isSlackConnected(UUID userId) {
        List<SlackSettingsEntity> accounts = slackSettingsRepository.findAllByUserId(userId);
        return accounts.stream().anyMatch(settings -> settings.isEnabled()
                && settings.getWebhookUrl() != null
                && !settings.getWebhookUrl().isBlank());
    }

    public SlackSettingsView getSlackView(UUID userId) {
        return integrationSettingsMapper.toSlackView(slackSettingsRepository.findAllByUserId(userId));
    }

    public boolean isNotionConnected(UUID userId) {
        List<NotionSettingsEntity> accounts = notionSettingsRepository.findAllByUserId(userId);
        return accounts.stream().anyMatch(settings -> settings.isEnabled()
                && settings.getApiToken() != null
                && !settings.getApiToken().isBlank());
    }

    public NotionSettingsView getNotionView(UUID userId) {
        return integrationSettingsMapper.toNotionView(notionSettingsRepository.findAllByUserId(userId));
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
                .map(integrationSettingsMapper::toLinkedInView)
                .orElse(LinkedInSettingsView.builder().configured(false).enabled(false).build());
    }

    @Cacheable(value = "account-labels", key = "#userId")
    public Map<String, String> getAccountLabels(UUID userId) {
        Map<String, String> labels = new HashMap<>();
 
        telegramSettingsRepository.findAllByUserId(userId).forEach(s ->
                labels.put("TELEGRAM:" + s.getId(), s.getLabel() == null || s.getLabel().isBlank() ? "Telegram" : "Telegram: " + s.getLabel()));
 
        discordSettingsRepository.findAllByUserId(userId).forEach(s ->
                labels.put("DISCORD:" + s.getId(), s.getLabel() == null || s.getLabel().isBlank() ? "Discord" : "Discord: " + s.getLabel()));
 
        slackSettingsRepository.findAllByUserId(userId).forEach(s ->
                labels.put("SLACK:" + s.getId(), s.getLabel() == null || s.getLabel().isBlank() ? "Slack" : "Slack: " + s.getLabel()));
 
        notionSettingsRepository.findAllByUserId(userId).forEach(s ->
                labels.put("NOTION:" + s.getId(), s.getLabel() == null || s.getLabel().isBlank() ? "Notion" : "Notion: " + s.getLabel()));
 
        redditSettingsRepository.findByUserId(userId).ifPresent(s -> {
            String label = s.getLabel() == null || s.getLabel().isBlank() ? "Reddit" : "Reddit: " + s.getLabel();
            labels.put("REDDIT:" + s.getId(), label);
            labels.put("REDDIT", label);
        });
 
        linkedinSettingsRepository.findByUserId(userId).ifPresent(s -> {
            labels.put("LINKEDIN:" + s.getId(), "LinkedIn");
            labels.put("LINKEDIN", "LinkedIn");
        });
 
        return labels;
    }
 
    @Cacheable(value = "integrations", key = "#userId")
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
}
