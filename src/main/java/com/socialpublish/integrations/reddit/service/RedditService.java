package com.socialpublish.integrations.reddit.service;

import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.reddit.config.RedditOAuthProperties;
import com.socialpublish.integrations.reddit.dto.RedditSettingsRequest;
import com.socialpublish.integrations.reddit.dto.RedditTokenResponse;
import com.socialpublish.integrations.reddit.entity.RedditSettingsEntity;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import com.socialpublish.integrations.service.BaseIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class RedditService extends BaseIntegrationService<RedditSettingsEntity, RedditSettingsRepository> {

    private static final String REDDIT_AUTH_URL = "https://www.reddit.com/api/v1/authorize";
    private static final String REDDIT_TOKEN_URL = "https://www.reddit.com/api/v1/access_token";

    private final RedditOAuthProperties properties;
    private final RestClient restClient;

    public RedditService(RedditSettingsRepository settingsRepository, UserRepository userRepository, 
                         RedditOAuthProperties properties, RestClient restClient) {
        super(settingsRepository, userRepository);
        this.properties = properties;
        this.restClient = restClient;
    }

    public String getAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        return String.format("%s?client_id=%s&response_type=code&state=%s&redirect_uri=%s&duration=permanent&scope=submit,identity",
                REDDIT_AUTH_URL,
                properties.getClientId(),
                state,
                URLEncoder.encode(properties.getRedirectUri(), StandardCharsets.UTF_8)
        );
    }

    @Transactional
    public void connectAccount(UUID userId, String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", properties.getRedirectUri());

        String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                (properties.getClientId() + ":" + properties.getClientSecret()).getBytes()
        );

        RedditTokenResponse response = restClient.post()
                .uri(REDDIT_TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, authHeader)
                .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(RedditTokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new RuntimeException("Failed to obtain Reddit token");
        }

        RedditSettingsEntity settings = findOrCreate(userId, RedditSettingsEntity::new);

        settings.setAccessToken(response.accessToken());
        if (response.refreshToken() != null) {
            settings.setRefreshToken(response.refreshToken());
        }
        settings.setEnabled(true);
        settingsRepository.save(settings);
    }

    @Transactional
    public void updateSettings(UUID userId, RedditSettingsRequest request) {
        settingsRepository.findByUserId(userId).ifPresent(settings -> {
            settings.setDefaultSubreddit(request.getDefaultSubreddit());
            settings.setEnabled(request.isEnabled());
            settingsRepository.save(settings);
        });
    }

    @Override
    @Transactional
    public void disconnect(UUID userId) {
        super.disconnect(userId);
    }
}
