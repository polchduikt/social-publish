package com.socialpublish.integrations.dto;

import com.socialpublish.integrations.discord.dto.DiscordSettingsView;
import com.socialpublish.integrations.linkedin.dto.LinkedInSettingsView;
import com.socialpublish.integrations.notion.dto.NotionSettingsView;
import com.socialpublish.integrations.reddit.dto.RedditSettingsView;
import com.socialpublish.integrations.slack.dto.SlackSettingsView;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsView;
import lombok.Builder;

@Builder
public record UserIntegrationsView(
        TelegramSettingsView telegram,
        DiscordSettingsView discord,
        RedditSettingsView reddit,
        SlackSettingsView slack,
        NotionSettingsView notion,
        LinkedInSettingsView linkedin
) {
    public boolean isTelegramConnected() { return telegram.enabled() && telegram.configured(); }
    public boolean isDiscordConnected() { return discord.enabled() && discord.configured(); }
    public boolean isRedditConnected() { return reddit.enabled() && reddit.configured(); }
    public boolean isSlackConnected() { return slack.enabled() && slack.configured(); }
    public boolean isNotionConnected() { return notion.enabled() && notion.configured(); }
    public boolean isLinkedInConnected() { return linkedin.enabled() && linkedin.configured(); }
}
