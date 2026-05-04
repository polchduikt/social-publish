package com.socialpublish.integrations.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.integrations.discord.dto.DiscordSettingsRequest;
import com.socialpublish.integrations.reddit.dto.RedditSettingsRequest;
import com.socialpublish.integrations.service.IntegrationStatusService;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsRequest;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AccountsPageController {

    private final IntegrationStatusService integrationStatusService;

    @GetMapping("/accounts")
    public String accountsPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("telegramSettings", integrationStatusService.getTelegramView(currentUser.id()));
        model.addAttribute("settingsRequest", new TelegramSettingsRequest());
        model.addAttribute("discordSettings", integrationStatusService.getDiscordView(currentUser.id()));
        model.addAttribute("discordSettingsRequest", new DiscordSettingsRequest());
        model.addAttribute("redditSettings", integrationStatusService.getRedditView(currentUser.id()));
        model.addAttribute("redditSettingsRequest", new RedditSettingsRequest());

        return "pages/accounts/accounts";
    }
}
