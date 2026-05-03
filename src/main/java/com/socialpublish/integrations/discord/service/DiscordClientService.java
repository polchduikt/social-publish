package com.socialpublish.integrations.discord.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DiscordClientService {

    private static final Logger log = LoggerFactory.getLogger(DiscordClientService.class);

    private final RestClient restClient;

    public DiscordClientService() {
        this.restClient = RestClient.create();
    }

    public void sendMessage(String webhookUrl, String content) {
        Map<String, Object> body = Map.of(
                "content", content == null ? "" : content
        );
        post(webhookUrl, body);
        log.info("Message sent to Discord webhook");
    }

    public void sendMessageWithImages(String webhookUrl, String content, List<String> imageUrls) {
        List<Map<String, Object>> embeds = imageUrls == null
                ? List.of()
                : imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .limit(10)
                .map(url -> {
                    Map<String, Object> image = Map.of("url", url);
                    return Map.<String, Object>of("image", image);
                })
                .toList();

        Map<String, Object> body = Map.of(
                "content", content == null ? "" : content,
                "embeds", embeds
        );
        post(webhookUrl, body);
        log.info("Message with {} image(s) sent to Discord webhook", embeds.size());
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