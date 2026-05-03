package com.socialpublish.integrations.discord.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
                "content", content
        );

        restClient.post()
                .uri(webhookUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        log.info("Message sent to Discord webhook");
    }
}
