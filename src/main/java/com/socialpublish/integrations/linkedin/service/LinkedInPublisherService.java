package com.socialpublish.integrations.linkedin.service;

import com.socialpublish.integrations.linkedin.entity.LinkedInSettingsEntity;
import com.socialpublish.integrations.linkedin.repository.LinkedInSettingsRepository;
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
public class LinkedInPublisherService implements PlatformPublisher {

    private final LinkedInSettingsRepository settingsRepository;
    private final LinkedInClientService linkedInClientService;

    @Override
    public Platform getPlatform() {
        return Platform.LINKEDIN;
    }

    @Override
    public void publish(Post post, UUID targetId) {
        UUID userId = post.getOwner().getId();
        LinkedInSettingsEntity settings;
        if (targetId != null) {
            settings = settingsRepository.findById(targetId)
                    .filter(s -> s.getUser().getId().equals(userId))
                    .orElseThrow(() -> new IntegrationException("LinkedIn not configured for user " + userId + " or account " + targetId));
        } else {
            settings = settingsRepository.findByUserId(userId)
                    .orElseThrow(() -> new IntegrationException("LinkedIn not configured for user " + userId));
        }

        if (!settings.isEnabled()) {
            throw new IntegrationException("LinkedIn integration is disabled");
        }

        String text = post.getContent() == null ? "" : post.getContent();
        List<String> mediaUrls = post.getMedia().stream()
                .map(PostMedia::getSecureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        linkedInClientService.sharePost(settings.getAccessToken(), settings.getAuthorUrn(), text, mediaUrls);
        log.info("Published post {} to LinkedIn for user {}", post.getId(), userId);
    }
}
