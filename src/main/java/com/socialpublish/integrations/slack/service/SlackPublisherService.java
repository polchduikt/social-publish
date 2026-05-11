package com.socialpublish.integrations.slack.service;

import com.socialpublish.integrations.slack.entity.SlackSettingsEntity;
import com.socialpublish.integrations.slack.repository.SlackSettingsRepository;
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
public class SlackPublisherService implements PlatformPublisher {

    private final SlackSettingsRepository settingsRepository;
    private final SlackClientService slackClientService;

    @Override
    public Platform getPlatform() {
        return Platform.SLACK;
    }

    @Override
    public void publish(Post post) {
        UUID userId = post.getOwner().getId();
        SlackSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IntegrationException("Slack not configured for user " + userId));

        if (!settings.isEnabled()) {
            throw new IntegrationException("Slack integration is disabled");
        }

        String message = post.getContent() == null ? "" : post.getContent();
        List<String> mediaUrls = post.getMedia().stream()
                .map(PostMedia::getSecureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();
        
        if (mediaUrls.isEmpty()) {
            slackClientService.sendMessage(settings.getWebhookUrl(), message);
        } else {
            slackClientService.sendMessageWithImages(settings.getWebhookUrl(), message, mediaUrls);
        }
        log.info("Published post {} to Slack for user {}", post.getId(), userId);
    }
}
