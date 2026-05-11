package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.integrations.service.IntegrationStatusService;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.service.PostTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostFormSupport {

    private final IntegrationStatusService integrationStatusService;
    private final PostTemplateService postTemplateService;

    public void populateFormModel(Model model, CurrentUserView user, String mode, UUID postId, PostUpsertRequest request) {
        model.addAttribute("user", user);
        model.addAttribute("mode", mode);
        if (request != null) {
            model.addAttribute("postRequest", request);
        }
        if (!model.containsAttribute("existingMedia")) {
            model.addAttribute("existingMedia", List.of());
        }
        model.addAttribute("statuses", List.of("DRAFT", "SCHEDULED", "CANCELLED"));
        var integrations = integrationStatusService.getAllStatuses(user.id());
        model.addAttribute("integrations", integrations);
        model.addAttribute("telegramConnected", integrations.isTelegramConnected());
        model.addAttribute("discordConnected", integrations.isDiscordConnected());
        model.addAttribute("redditConnected", integrations.isRedditConnected());
        model.addAttribute("slackConnected", integrations.isSlackConnected());
        model.addAttribute("notionConnected", integrations.isNotionConnected());
        model.addAttribute("linkedinConnected", integrations.isLinkedInConnected());
        model.addAttribute("templates", postTemplateService.getUserTemplates(user.id()));

        if (postId != null) {
            model.addAttribute("postId", postId);
        }
    }
}
