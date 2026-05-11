package com.socialpublish.integrations.slack.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlackClientService {

    private final RestClient restClient;

    public void sendMessage(String webhookUrl, String content) {
        Map<String, Object> body = Map.of(
                "text", content == null ? "" : content
        );
        post(webhookUrl, body);
        log.info("Message sent to slack webhook");
    }

    public void sendMessageWithImages(String webhookUrl, String content, List<String> imageUrls) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        if (content != null && !content.isBlank()) {
            blocks.add(Map.of(
                "type", "section",
                "text", Map.of("type", "mrkdwn", "text", content)
            ));
        }
        
        if (imageUrls != null) {
            imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .limit(10)
                .forEach(url -> {
                    blocks.add(Map.of(
                        "type", "image",
                        "image_url", url,
                        "alt_text", "Image"
                    ));
                });
        }

        Map<String, Object> body = Map.of(
                "text", content == null ? "New post" : content,
                "blocks", blocks
        );
        post(webhookUrl, body);
        log.info("Message with image(s) sent to slack webhook");
    }

    private void post(String webhookUrl, Map<String, Object> body) {
        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}

