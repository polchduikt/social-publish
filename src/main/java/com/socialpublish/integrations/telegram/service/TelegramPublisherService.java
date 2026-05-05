package com.socialpublish.integrations.telegram.service;

import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.integrations.exception.IntegrationException;
import com.socialpublish.media.entity.PostMedia;
import com.socialpublish.posts.entity.Post;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TelegramPublisherService {

    private static final Logger log = LoggerFactory.getLogger(TelegramPublisherService.class);

    private final TelegramSettingsRepository settingsRepository;
    private final TelegramClientService telegramClientService;

    public void publish(Post post) {
        UUID userId = post.getOwner().getId();
        TelegramSettingsEntity settings = settingsRepository.findByUserId(userId)
                .orElseThrow(() -> new IntegrationException("Telegram not configured for user " + userId));

        if (!settings.isEnabled()) {
            throw new IntegrationException("Telegram integration is disabled");
        }

        String message = formatMessage(post);
        List<String> mediaUrls = post.getMedia().stream()
                .map(PostMedia::getSecureUrl)
                .filter(url -> url != null && !url.isBlank())
                .toList();

        boolean longMessage = message.length() > 1024;
        String caption = longMessage ? "" : message;

        if (mediaUrls.isEmpty()) {
            telegramClientService.sendMessage(settings.getBotToken(), settings.getChatId(), message);
        } else if (mediaUrls.size() == 1) {
            telegramClientService.sendPhoto(settings.getBotToken(), settings.getChatId(), mediaUrls.get(0), caption);
            if (longMessage) {
                telegramClientService.sendMessage(settings.getBotToken(), settings.getChatId(), message);
            }
        } else {
            telegramClientService.sendMediaGroup(
                    settings.getBotToken(),
                    settings.getChatId(),
                    mediaUrls,
                    caption
            );
            if (longMessage) {
                telegramClientService.sendMessage(settings.getBotToken(), settings.getChatId(), message);
            }
        }
        log.info("Published post {} to Telegram for user {}", post.getId(), userId);
    }

    private String formatMessage(Post post) {
        return convertMarkdownToTelegramHtml(post.getContent());
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String convertMarkdownToTelegramHtml(String text) {
        String escaped = escapeHtml(text == null ? "" : text);
        String withLinks = escaped.replaceAll("\\[(.+?)]\\((https?://[^\\s)]+)\\)", "<a href=\"$2\">$1</a>");
        String withBold = withLinks.replaceAll("\\*\\*(.+?)\\*\\*", "<b>$1</b>");
        String withUnderline = withBold.replaceAll("__(.+?)__", "<u>$1</u>");
        String withItalic = withUnderline.replaceAll("\\*(.+?)\\*", "<i>$1</i>");
        String withStrike = withItalic.replaceAll("~~(.+?)~~", "<s>$1</s>");
        String withSpoiler = withStrike.replaceAll("\\|\\|(.+?)\\|\\|", "<tg-spoiler>$1</tg-spoiler>");
        String withCode = withSpoiler.replaceAll("`([^`]+)`", "<code>$1</code>");
        return withCode.replaceAll("(?m)^&gt;\\s?(.*)$", "<blockquote>$1</blockquote>");
    }

}
