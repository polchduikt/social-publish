package com.socialpublish.integrations.telegram.service;

import com.socialpublish.integrations.exception.IntegrationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramClientService {

    private static final String API_BASE = "https://api.telegram.org/bot";
    private final RestClient restClient;

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
        assertOk(response);

        log.info("Message sent to Telegram chat {}", chatId);
    }

    public void sendPhoto(String botToken, String chatId, String photoUrl, String captionHtml) {
        String url = API_BASE + botToken + "/sendPhoto";
        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "photo", photoUrl,
                "caption", captionHtml == null ? "" : captionHtml,
                "parse_mode", "HTML"
        );
        TelegramApiResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TelegramApiResponse.class);
        assertOk(response);
        log.info("Photo sent to Telegram chat {}", chatId);
    }

    public void sendMediaGroup(String botToken, String chatId, List<String> photoUrls, String captionHtml) {
        if (photoUrls == null || photoUrls.isEmpty()) {
            return;
        }

        String url = API_BASE + botToken + "/sendMediaGroup";
        List<Map<String, Object>> media = new ArrayList<>();
        for (int index = 0; index < photoUrls.size(); index++) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", "photo");
            item.put("media", photoUrls.get(index));
            if (index == 0 && captionHtml != null && !captionHtml.isBlank()) {
                item.put("caption", captionHtml);
                item.put("parse_mode", "HTML");
            }
            media.add(item);
        }

        Map<String, Object> body = Map.of(
                "chat_id", chatId,
                "media", media
        );

        TelegramApiResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TelegramApiResponse.class);
        assertOk(response);
        log.info("Media group sent to Telegram chat {}", chatId);
    }

    private void assertOk(TelegramApiResponse response) {
        if (response == null || !response.ok()) {
            String description = response == null ? "No response" : response.description();
            log.error("Telegram API error: {}", description);
            throw new IntegrationException("Telegram API error: " + description);
        }
    }

    private record TelegramApiResponse(boolean ok, String description) {
    }
}
