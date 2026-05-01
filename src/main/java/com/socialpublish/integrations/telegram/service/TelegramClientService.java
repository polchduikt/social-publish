package com.socialpublish.integrations.telegram.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class TelegramClientService {

    private static final Logger log = LoggerFactory.getLogger(TelegramClientService.class);
    private static final String API_BASE = "https://api.telegram.org/bot";

    private final RestClient restClient;

    public TelegramClientService() {
        this.restClient = RestClient.create();
    }

    public void sendMessage(String botToken, String chatId, String text) {
        String url = API_BASE + botToken + "/sendMessage";

        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "text", text,
                "parse_mode", "HTML"
        );

        TelegramApiResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TelegramApiResponse.class);

        if (response == null || !response.ok()) {
            String description = response == null ? "No response" : response.description();
            log.error("Telegram API error: {}", description);
            throw new RuntimeException("Telegram API error: " + description);
        }

        log.info("Message sent to Telegram chat {}", chatId);
    }

    private record TelegramApiResponse(boolean ok, String description) {}
}
