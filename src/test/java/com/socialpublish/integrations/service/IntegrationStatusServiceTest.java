package com.socialpublish.integrations.service;

import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.integrations.dto.UserIntegrationsView;
import com.socialpublish.integrations.linkedin.repository.LinkedInSettingsRepository;
import com.socialpublish.integrations.notion.repository.NotionSettingsRepository;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import com.socialpublish.integrations.slack.repository.SlackSettingsRepository;
import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IntegrationStatusServiceTest {

    @Mock private TelegramSettingsRepository telegramRepository;
    @Mock private DiscordSettingsRepository discordRepository;
    @Mock private RedditSettingsRepository redditRepository;
    @Mock private SlackSettingsRepository slackRepository;
    @Mock private NotionSettingsRepository notionRepository;
    @Mock private LinkedInSettingsRepository linkedInRepository;

    @InjectMocks
    private IntegrationStatusService integrationStatusService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Test
    void getAllStatuses_WithNoIntegrations_ReturnsAllFalse() {
        when(telegramRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(discordRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(redditRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(slackRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(notionRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(linkedInRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UserIntegrationsView result = integrationStatusService.getAllStatuses(userId);

        assertNotNull(result);
        assertFalse(result.telegram().configured());
        assertFalse(result.discord().configured());
        assertFalse(result.reddit().configured());
        assertFalse(result.slack().configured());
        assertFalse(result.notion().configured());
        assertNotNull(result.linkedin());
        assertFalse(result.linkedin().configured());
    }

    @Test
    void getAllStatuses_WithConfiguredIntegrations_ReturnsTrueForConfigured() {
        TelegramSettingsEntity tg = new TelegramSettingsEntity();
        tg.setId(UUID.randomUUID());
        tg.setBotToken("token");
        tg.setChatId("chat");
        tg.setEnabled(true);

        DiscordSettingsEntity discord = new DiscordSettingsEntity();
        discord.setId(UUID.randomUUID());
        discord.setWebhookUrl("url");
        discord.setEnabled(false);

        when(telegramRepository.findAllByUserId(userId)).thenReturn(List.of(tg));
        when(discordRepository.findAllByUserId(userId)).thenReturn(List.of(discord));
        when(redditRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(slackRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(notionRepository.findAllByUserId(userId)).thenReturn(List.of());
        when(linkedInRepository.findByUserId(userId)).thenReturn(Optional.empty());

        UserIntegrationsView result = integrationStatusService.getAllStatuses(userId);

        assertNotNull(result);
        assertTrue(result.telegram().configured());
        assertTrue(result.telegram().enabled());
        assertEquals(1, result.telegram().accounts().size());

        assertTrue(result.discord().configured());
        assertFalse(result.discord().enabled());
        assertEquals(1, result.discord().accounts().size());
        
        assertFalse(result.reddit().configured());
    }
}
