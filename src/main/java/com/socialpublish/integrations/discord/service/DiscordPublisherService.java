package com.socialpublish.integrations.discord.service;

import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.posts.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DiscordPublisherService {

    private static final Logger log = LoggerFactory.getLogger(DiscordPublisherService.class);

    private final DiscordSettingsRepository settingsRepository;
    private final DiscordClientService discordClientService;

    public DiscordPublisherService(
            DiscordSettingsRepository settingsRepository,
            DiscordClientService discordClientService
    ) {
        this.settingsRepository = settingsRepository;
        this.discordClientService = discordClientService;
    }

    public void publish(Post post) {
        UUID userId = post.getOwner().getId();
        DiscordSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Discord not configured for user " + userId));

        if (!settings.isEnabled()) {
            throw new RuntimeException("Discord integration is disabled");
        }

        String message = formatMessage(post);
        discordClientService.sendMessage(settings.getWebhookUrl(), message);
        log.info("Published post {} to Discord for user {}", post.getId(), userId);
    }

    private String formatMessage(Post post) {
        return "**" + post.getTitle() + "**\n\n" + post.getContent();
    }
}
