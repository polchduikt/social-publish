package com.socialpublish.integrations.discord.service;

import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.integrations.exception.IntegrationException;
import com.socialpublish.media.entity.PostMedia;
import com.socialpublish.posts.entity.Post;
import com.socialpublish.publishing.entity.Platform;
import com.socialpublish.publishing.service.PlatformPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordPublisherService implements PlatformPublisher {

    private final DiscordSettingsRepository settingsRepository;
    private final DiscordClientService discordClientService;

    @Override
    public Platform getPlatform() {
        return Platform.DISCORD;
    }

    @Override
    public void publish(Post post) {
        UUID userId = post.getOwner().getId();
        DiscordSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IntegrationException("Discord not configured for user " + userId));

        if (!settings.isEnabled()) {
            throw new IntegrationException("Discord integration is disabled");
        }

        String message = formatMessage(post);
        List<String> mediaUrls = post.getMedia().stream()
                .map(PostMedia::getSecureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
        if (mediaUrls.isEmpty()) {
            discordClientService.sendMessage(settings.getWebhookUrl(), message);
        } else {
            discordClientService.sendMessageWithImages(settings.getWebhookUrl(), message, mediaUrls);
        }
        log.info("Published post {} to Discord for user {}", post.getId(), userId);
    }

    private String formatMessage(Post post) {
        return post.getContent() == null ? "" : post.getContent();
    }
}
