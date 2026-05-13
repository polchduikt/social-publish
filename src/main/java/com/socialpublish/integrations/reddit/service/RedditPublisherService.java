package com.socialpublish.integrations.reddit.service;

import com.socialpublish.integrations.reddit.config.RedditOAuthProperties;
import com.socialpublish.integrations.reddit.entity.RedditSettingsEntity;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import com.socialpublish.posts.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import com.socialpublish.integrations.reddit.dto.RedditSubmitResponse;
import com.socialpublish.integrations.reddit.dto.RedditTokenResponse;
import java.util.Base64;
import java.util.UUID;
import com.socialpublish.publishing.entity.Platform;
import com.socialpublish.publishing.service.PlatformPublisher;

@Service
@RequiredArgsConstructor
public class RedditPublisherService implements PlatformPublisher {

    private final RedditSettingsRepository settingsRepository;
    private final RedditOAuthProperties properties;
    private final RestClient restClient = RestClient.create();

    @Override
    public Platform getPlatform() {
        return Platform.REDDIT;
    }

    @Override
    public void publish(Post post, UUID targetId) {
        UUID userId = post.getOwner().getId();
        RedditSettingsEntity settings;
        if (targetId != null) {
            settings = settingsRepository.findById(targetId)
                    .filter(s -> s.getUser().getId().equals(userId))
                    .filter(RedditSettingsEntity::isEnabled)
                    .orElseThrow(() -> new RuntimeException("Reddit integration not configured or disabled for account " + targetId));
        } else {
            settings = settingsRepository.findByUserId(userId)
                    .filter(RedditSettingsEntity::isEnabled)
                    .orElseThrow(() -> new RuntimeException("Reddit integration not configured or disabled"));
        }

        String accessToken = getValidAccessToken(settings);

        String subreddit = settings.getDefaultSubreddit();
        if (subreddit == null || subreddit.isBlank()) {
            throw new RuntimeException("Default subreddit is not configured for Reddit");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("sr", subreddit);
        body.add("title", post.getTitle() != null && !post.getTitle().isBlank() ? post.getTitle() : "Post from Social Publish");
        body.add("kind", "self"); // Text post
        body.add("text", post.getContent());

        try {
            RedditSubmitResponse response = restClient.post()
                    .uri("https://oauth.reddit.com/api/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(RedditSubmitResponse.class);

            if (response != null && Boolean.FALSE.equals(response.success())) {
                 throw new RuntimeException("Reddit API returned failure: " + response);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to post to Reddit", e);
        }
    }

    private String getValidAccessToken(RedditSettingsEntity settings) {
        if (settings.getRefreshToken() == null) {
            throw new RuntimeException("No Reddit refresh token available");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", settings.getRefreshToken());

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (properties.getClientId() + ":" + properties.getClientSecret()).getBytes()
        );

        RedditTokenResponse response = restClient.post()
                .uri("https://www.reddit.com/api/v1/access_token")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(RedditTokenResponse.class);

        if (response != null && response.accessToken() != null) {
            String newAccessToken = response.accessToken();
            settings.setAccessToken(newAccessToken);
            settingsRepository.save(settings);
            return newAccessToken;
        } else {
            throw new RuntimeException("Could not refresh Reddit token");
        }
    }
}
