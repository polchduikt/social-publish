package com.socialpublish.integrations.telegram.service;

import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.posts.entity.Post;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TelegramPublisherService {

    private static final Logger log = LoggerFactory.getLogger(TelegramPublisherService.class);

    private final TelegramSettingsRepository settingsRepository;
    private final TelegramClientService telegramClientService;

    public TelegramPublisherService(
            TelegramSettingsRepository settingsRepository,
            TelegramClientService telegramClientService
    ) {
        this.settingsRepository = settingsRepository;
        this.telegramClientService = telegramClientService;
    }

    public void publish(Post post) {
        UUID userId = post.getOwner().getId();
        TelegramSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Telegram not configured for user " + userId));

        if (!settings.isEnabled()) {
            throw new RuntimeException("Telegram integration is disabled");
        }

        String message = formatMessage(post);
        telegramClientService.sendMessage(settings.getBotToken(), settings.getChatId(), message);
        log.info("Published post {} to Telegram for user {}", post.getId(), userId);
    }

    private String formatMessage(Post post) {
        return "<b>" + escapeHtml(post.getTitle()) + "</b>\n\n" + escapeHtml(post.getContent());
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
