package com.socialpublish.integrations.linkedin.service;

import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.integrations.linkedin.dto.LinkedInSettingsView;
import com.socialpublish.integrations.linkedin.dto.LinkedInTokenResponse;
import com.socialpublish.integrations.linkedin.entity.LinkedInSettingsEntity;
import com.socialpublish.integrations.linkedin.repository.LinkedInSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LinkedInService {

    private final LinkedInSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final LinkedInClientService linkedInClient;

    @Value("${linkedin.client-id:}")
    private String clientId;

    @Value("${linkedin.client-secret:}")
    private String clientSecret;

    @Value("${linkedin.redirect-uri:}")
    private String redirectUri;

    public String getAuthorizationUrl() {
        return "https://www.linkedin.com/oauth/v2/authorization" +
                "?response_type=code" +
                "&client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&scope=openid%20profile%20w_member_social%20email";
    }

    @Transactional
    public void connectAccount(UUID userId, String code) {
        LinkedInTokenResponse tokenResponse = linkedInClient.exchangeCodeForToken(code, clientId, clientSecret, redirectUri);
        String accessToken = tokenResponse.accessToken();
        String refreshToken = tokenResponse.refreshToken();
        Integer expiresIn = tokenResponse.expiresIn();

        String authorUrn = linkedInClient.getProfileUrn(accessToken);

        LinkedInSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> {
                    LinkedInSettingsEntity s = new LinkedInSettingsEntity();
                    User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
                    s.setUser(user);
                    return s;
                });

        settings.setAccessToken(accessToken);
        settings.setRefreshToken(refreshToken);
        settings.setAuthorUrn(authorUrn);
        settings.setEnabled(true);
        if (expiresIn != null) {
            settings.setExpiresAt(Instant.now().plusSeconds(expiresIn));
        }
        
        settingsRepository.save(settings);
    }

    @Transactional
    public void disconnectAccount(UUID userId) {
        settingsRepository.findByUserId(userId).ifPresent(settingsRepository::delete);
    }

    @Transactional(readOnly = true)
    public LinkedInSettingsView getSettings(UUID userId) {
        return settingsRepository.findByUserId(userId)
                .map(this::toSettingsView)
                .orElse(LinkedInSettingsView.builder().configured(false).enabled(false).build());
    }

    public void testPost(UUID userId, String testMessage) {
        LinkedInSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("LinkedIn is not configured or disabled"));
        if (!settings.isEnabled()) {
            throw new RuntimeException("LinkedIn is not configured or disabled");
        }
        linkedInClient.sharePost(settings.getAccessToken(), settings.getAuthorUrn(), testMessage, null);
    }

    private LinkedInSettingsView toSettingsView(LinkedInSettingsEntity entity) {
        return LinkedInSettingsView.builder()
                .id(entity.getId())
                .accessToken(entity.getAccessToken())
                .authorUrn(entity.getAuthorUrn())
                .expiresAt(entity.getExpiresAt())
                .enabled(entity.isEnabled())
                .configured(true)
                .build();
    }
}
