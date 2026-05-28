package com.socialpublish.integrations.telegram.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final ObjectMapper objectMapper;

    public void sendMessage(String botToken, String chatId, String text, boolean silent, String inlineButtonsJson) {
        String url = API_BASE + botToken + "/sendMessage";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);
        body.put("parse_mode", "HTML");
        if (silent) body.put("disable_notification", true);
        Map<String, Object> replyMarkup = buildReplyMarkup(inlineButtonsJson);
        if (replyMarkup != null) body.put("reply_markup", replyMarkup);

        execute(url, body);
        log.info("Message sent to Telegram chat {}", chatId);
    }

    public void sendPhoto(String botToken, String chatId, String photoUrl, String captionHtml, boolean silent, String inlineButtonsJson) {
        String url = API_BASE + botToken + "/sendPhoto";
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("photo", photoUrl);
        body.put("caption", captionHtml == null ? "" : captionHtml);
        body.put("parse_mode", "HTML");
        if (silent) body.put("disable_notification", true);
        Map<String, Object> replyMarkup = buildReplyMarkup(inlineButtonsJson);
        if (replyMarkup != null) body.put("reply_markup", replyMarkup);

        execute(url, body);
        log.info("Photo sent to Telegram chat {}", chatId);
    }

    public void sendMediaGroup(String botToken, String chatId, List<String> photoUrls, String captionHtml, boolean silent) {
        if (photoUrls == null || photoUrls.isEmpty()) return;

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

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("media", media);
        if (silent) body.put("disable_notification", true);

        execute(url, body);
        log.info("Media group sent to Telegram chat {}", chatId);
    }

    public void sendPoll(String botToken, String chatId, String question, List<String> options, boolean silent, boolean multiple, boolean quiz, Integer correctOptionId) {
        String url = API_BASE + botToken + "/sendPoll";
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("question", question);
        body.put("options", options);
        if (silent) body.put("disable_notification", true);
        if (quiz) {
            body.put("type", "quiz");
            if (correctOptionId != null) {
                body.put("correct_option_id", correctOptionId);
            } else {
                body.put("correct_option_id", 0);
            }
        } else {
            body.put("type", "regular");
            body.put("allows_multiple_answers", multiple);
        }

        execute(url, body);
        log.info("Poll sent to Telegram chat {}", chatId);
    }

    private Map<String, Object> buildReplyMarkup(String inlineButtonsJson) {
        if (inlineButtonsJson == null || inlineButtonsJson.isBlank()) return null;
        try {
            List<Map<String, String>> buttons = objectMapper.readValue(inlineButtonsJson, new TypeReference<List<Map<String, String>>>(){});
            if (buttons.isEmpty()) return null;


            List<List<Map<String, String>>> keyboard = new ArrayList<>();
            for (Map<String, String> btn : buttons) {
                keyboard.add(List.of(btn));
            }
            return Map.of("inline_keyboard", keyboard);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse inline buttons JSON", e);
            return null;
        }
    }

    private void execute(String url, Map<String, Object> body) {
        TelegramApiResponse response = restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TelegramApiResponse.class);
        assertOk(response);
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
