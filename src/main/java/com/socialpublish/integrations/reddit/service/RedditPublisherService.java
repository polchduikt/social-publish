package com.socialpublish.integrations.reddit.service;

import com.socialpublish.integrations.reddit.config.RedditOAuthProperties;
import com.socialpublish.integrations.reddit.entity.RedditSettingsEntity;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import com.socialpublish.posts.entity.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedditPublisherService {

    private final RedditSettingsRepository settingsRepository;
    private final RedditOAuthProperties properties;
    private final RestClient restClient = RestClient.create();

    public void publish(Post post) {
        UUID userId = post.getOwner().getId();
        RedditSettingsEntity settings = settingsRepository.findByUserId(userId)
                .filter(RedditSettingsEntity::isEnabled)
                .orElseThrow(() -> new RuntimeException("Reddit integration not configured or disabled"));

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
            Map<String, Object> response = restClient.post()
                    .uri("https://oauth.reddit.com/api/submit")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.containsKey("success") && response.get("success").equals(Boolean.FALSE)) {
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

        Map<String, Object> response = restClient.post()
                .uri("https://www.reddit.com/api/v1/access_token")
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response != null && response.containsKey("access_token")) {
            String newAccessToken = (String) response.get("access_token");
            settings.setAccessToken(newAccessToken);
            settingsRepository.save(settings);
            return newAccessToken;
        } else {
            throw new RuntimeException("Could not refresh Reddit token");
        }
    }
}
