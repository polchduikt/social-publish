package com.socialpublish.integrations.reddit.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.integrations.reddit.config.RedditOAuthProperties;
import com.socialpublish.integrations.reddit.entity.RedditSettingsEntity;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/integrations/reddit")
@RequiredArgsConstructor
public class RedditOAuthController {

    private static final Logger log = LoggerFactory.getLogger(RedditOAuthController.class);
    private static final String REDDIT_AUTH_URL = "https://www.reddit.com/api/v1/authorize";
    private static final String REDDIT_TOKEN_URL = "https://www.reddit.com/api/v1/access_token";

    private final RedditOAuthProperties properties;
    private final RedditSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final RestClient restClient = RestClient.create();

    @GetMapping("/authorize")
    public String authorize() {
        String state = UUID.randomUUID().toString();
        String authUrl = String.format("%s?client_id=%s&response_type=code&state=%s&redirect_uri=%s&duration=permanent&scope=submit,identity",
                REDDIT_AUTH_URL,
                properties.getClientId(),
                state,
                URLEncoder.encode(properties.getRedirectUri(), StandardCharsets.UTF_8)
        );
        return "redirect:" + authUrl;
    }

    @GetMapping("/callback")
    public String callback(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error
    ) {
        if (error != null) {
            log.error("Reddit OAuth error: {}", error);
            return "redirect:/accounts?error=Reddit+authorization+failed";
        }

        if (code == null) {
            return "redirect:/accounts?error=Missing+authorization+code";
        }

        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", properties.getRedirectUri());

            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (properties.getClientId() + ":" + properties.getClientSecret()).getBytes()
            );

            Map<String, Object> response = restClient.post()
                    .uri(REDDIT_TOKEN_URL)
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .header(HttpHeaders.USER_AGENT, properties.getUserAgent())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response != null && response.containsKey("access_token")) {
                String accessToken = (String) response.get("access_token");
                String refreshToken = (String) response.get("refresh_token");

                RedditSettingsEntity settings = settingsRepository.findByUserId(currentUser.id())
                        .orElseGet(() -> {
                            RedditSettingsEntity s = new RedditSettingsEntity();
                            User user = userRepository.findById(currentUser.id()).orElseThrow();
                            s.setUser(user);
                            return s;
                        });

                settings.setAccessToken(accessToken);
                if (refreshToken != null) {
                    settings.setRefreshToken(refreshToken);
                }
                settings.setEnabled(true);
                settingsRepository.save(settings);

                return "redirect:/accounts?message=Reddit+connected+successfully";
            } else {
                return "redirect:/accounts?error=Failed+to+obtain+Reddit+token";
            }
        } catch (Exception e) {
            log.error("Error during Reddit token exchange", e);
            return "redirect:/accounts?error=Failed+to+connect+Reddit";
        }
    }
}
