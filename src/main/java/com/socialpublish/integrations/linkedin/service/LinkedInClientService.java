package com.socialpublish.integrations.linkedin.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import com.socialpublish.integrations.linkedin.dto.LinkedInTokenResponse;
import com.socialpublish.integrations.linkedin.dto.LinkedInProfileResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkedInClientService {

    private final RestClient restClient;

    public LinkedInTokenResponse exchangeCodeForToken(String code, String clientId, String clientSecret, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);

        return restClient.post()
            .uri("https://www.linkedin.com/oauth/v2/accessToken")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(params)
            .retrieve()
            .body(LinkedInTokenResponse.class);
    }

    public String getProfileUrn(String accessToken) {
        LinkedInProfileResponse profile = restClient.get()
            .uri("https://api.linkedin.com/v2/userinfo")
            .header("Authorization", "Bearer " + accessToken)
            .retrieve()
            .body(LinkedInProfileResponse.class);

        if (profile != null && profile.sub() != null) {
            return "urn:li:person:" + profile.sub();
        }
        
        throw new RuntimeException("Failed to get profile URN");
    }

    public void sharePost(String accessToken, String authorUrn, String text, List<String> imageUrls) {
        Map<String, Object> body = Map.of(
            "author", authorUrn,
            "lifecycleState", "PUBLISHED",
            "specificContent", Map.of(
                "com.linkedin.ugc.ShareContent", Map.of(
                    "shareCommentary", Map.of("text", text),
                    "shareMediaCategory", "NONE"
                )
            ),
            "visibility", Map.of("com.linkedin.ugc.MemberNetworkVisibility", "PUBLIC")
        );

        restClient.post()
            .uri("https://api.linkedin.com/v2/ugcPosts")
            .header("Authorization", "Bearer " + accessToken)
            .header("X-Restli-Protocol-Version", "2.0.0")
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();

        log.info("Post shared on LinkedIn for author: {}", authorUrn);
    }
}
