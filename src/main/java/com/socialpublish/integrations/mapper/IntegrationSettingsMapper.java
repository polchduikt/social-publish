package com.socialpublish.integrations.mapper;

import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsRequest;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsView;
import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import com.socialpublish.integrations.discord.dto.DiscordSettingsRequest;
import com.socialpublish.integrations.discord.dto.DiscordSettingsView;
import com.socialpublish.integrations.slack.entity.SlackSettingsEntity;
import com.socialpublish.integrations.slack.dto.SlackSettingsRequest;
import com.socialpublish.integrations.slack.dto.SlackSettingsView;
import com.socialpublish.integrations.notion.entity.NotionSettingsEntity;
import com.socialpublish.integrations.notion.dto.NotionSettingsRequest;
import com.socialpublish.integrations.notion.dto.NotionSettingsView;
import com.socialpublish.integrations.reddit.entity.RedditSettingsEntity;
import com.socialpublish.integrations.reddit.dto.RedditSettingsView;
import com.socialpublish.integrations.linkedin.entity.LinkedInSettingsEntity;
import com.socialpublish.integrations.linkedin.dto.LinkedInSettingsView;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface IntegrationSettingsMapper {

    TelegramSettingsRequest toRequest(TelegramSettingsEntity entity);
    List<TelegramSettingsRequest> toTelegramRequests(List<TelegramSettingsEntity> entities);

    DiscordSettingsRequest toRequest(DiscordSettingsEntity entity);
    List<DiscordSettingsRequest> toDiscordRequests(List<DiscordSettingsEntity> entities);

    SlackSettingsRequest toRequest(SlackSettingsEntity entity);
    List<SlackSettingsRequest> toSlackRequests(List<SlackSettingsEntity> entities);

    NotionSettingsRequest toRequest(NotionSettingsEntity entity);
    List<NotionSettingsRequest> toNotionRequests(List<NotionSettingsEntity> entities);

    @Mapping(target = "label", expression = "java(entity.getLabel() == null ? \"\" : entity.getLabel())")
    TelegramSettingsView.TelegramAccountView toTelegramAccountView(TelegramSettingsEntity entity);

    default TelegramSettingsView toTelegramView(List<TelegramSettingsEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new TelegramSettingsView(Collections.emptyList(), false, false);
        }
        List<TelegramSettingsView.TelegramAccountView> accounts = entities.stream()
                .map(this::toTelegramAccountView).toList();
        boolean enabled = accounts.stream().anyMatch(TelegramSettingsView.TelegramAccountView::enabled);
        return new TelegramSettingsView(accounts, true, enabled);
    }

    @Mapping(target = "label", expression = "java(entity.getLabel() == null ? \"\" : entity.getLabel())")
    DiscordSettingsView.DiscordAccountView toDiscordAccountView(DiscordSettingsEntity entity);

    default DiscordSettingsView toDiscordView(List<DiscordSettingsEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new DiscordSettingsView(Collections.emptyList(), false, false);
        }
        List<DiscordSettingsView.DiscordAccountView> accounts = entities.stream()
                .map(this::toDiscordAccountView).toList();
        boolean enabled = accounts.stream().anyMatch(DiscordSettingsView.DiscordAccountView::enabled);
        return new DiscordSettingsView(accounts, true, enabled);
    }

    @Mapping(target = "label", expression = "java(entity.getLabel() == null ? \"\" : entity.getLabel())")
    SlackSettingsView.SlackAccountView toSlackAccountView(SlackSettingsEntity entity);

    default SlackSettingsView toSlackView(List<SlackSettingsEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new SlackSettingsView(Collections.emptyList(), false, false);
        }
        List<SlackSettingsView.SlackAccountView> accounts = entities.stream()
                .map(this::toSlackAccountView).toList();
        boolean enabled = accounts.stream().anyMatch(SlackSettingsView.SlackAccountView::enabled);
        return new SlackSettingsView(accounts, true, enabled);
    }

    @Mapping(target = "label", expression = "java(entity.getLabel() == null ? \"\" : entity.getLabel())")
    NotionSettingsView.NotionAccountView toNotionAccountView(NotionSettingsEntity entity);

    default NotionSettingsView toNotionView(List<NotionSettingsEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new NotionSettingsView(Collections.emptyList(), false, false);
        }
        List<NotionSettingsView.NotionAccountView> accounts = entities.stream()
                .map(this::toNotionAccountView).toList();
        boolean enabled = accounts.stream().anyMatch(NotionSettingsView.NotionAccountView::enabled);
        return new NotionSettingsView(accounts, true, enabled);
    }

    @Named("toRedditView")
    default RedditSettingsView toRedditView(RedditSettingsEntity entity) {
        boolean configured = entity.getRefreshToken() != null && !entity.getRefreshToken().isBlank();
        String subreddit = entity.getDefaultSubreddit() == null ? "" : entity.getDefaultSubreddit();
        String label = entity.getLabel() == null ? "" : entity.getLabel();
        return new RedditSettingsView(configured, entity.isEnabled(), subreddit, label);
    }

    @Mapping(target = "configured", constant = "true")
    LinkedInSettingsView toLinkedInView(LinkedInSettingsEntity entity);
}
