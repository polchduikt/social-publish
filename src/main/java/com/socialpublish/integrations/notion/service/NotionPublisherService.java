package com.socialpublish.integrations.notion.service;

import com.socialpublish.integrations.notion.entity.NotionSettingsEntity;
import com.socialpublish.integrations.notion.repository.NotionSettingsRepository;
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
public class NotionPublisherService implements PlatformPublisher {

    private final NotionSettingsRepository settingsRepository;
    private final NotionClientService notionClientService;

    @Override
    public Platform getPlatform() {
        return Platform.NOTION;
    }

    @Override
    public void publish(Post post) {
        UUID userId = post.getOwner().getId();
        NotionSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IntegrationException("Notion not configured for user " + userId));

        if (!settings.isEnabled()) {
            throw new IntegrationException("Notion integration is disabled");
        }

        String content = post.getContent() == null ? "" : post.getContent();
        List<String> mediaUrls = post.getMedia().stream()
                .map(PostMedia::getSecureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        notionClientService.createDatabaseEntry(settings.getApiToken(), settings.getDatabaseId(), content, mediaUrls);
        log.info("Published post {} to Notion for user {}", post.getId(), userId);
    }
}
