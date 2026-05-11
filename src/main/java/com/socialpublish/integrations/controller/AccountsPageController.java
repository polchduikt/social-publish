package com.socialpublish.integrations.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.integrations.discord.dto.DiscordSettingsRequest;
import com.socialpublish.integrations.linkedin.dto.LinkedInSettingsRequest;
import com.socialpublish.integrations.notion.dto.NotionSettingsRequest;
import com.socialpublish.integrations.reddit.dto.RedditSettingsRequest;
import com.socialpublish.integrations.service.IntegrationStatusService;
import com.socialpublish.integrations.slack.dto.SlackSettingsRequest;
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
        var integrations = integrationStatusService.getAllStatuses(currentUser.id());
        model.addAttribute("integrations", integrations);
        model.addAttribute("telegramSettings", integrations.telegram());
        model.addAttribute("discordSettings", integrations.discord());
        model.addAttribute("redditSettings", integrations.reddit());
        model.addAttribute("slackSettings", integrations.slack());
        model.addAttribute("notionSettings", integrations.notion());
        model.addAttribute("linkedinSettings", integrations.linkedin());
        return "pages/accounts/accounts";
    }

    @GetMapping("/accounts/telegram")
    public String telegramSetupPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("telegramSettings", integrationStatusService.getTelegramView(currentUser.id()));
        model.addAttribute("settingsRequest", new TelegramSettingsRequest());
        return "pages/accounts/telegram";
    }

    @GetMapping("/accounts/discord")
    public String discordSetupPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("discordSettings", integrationStatusService.getDiscordView(currentUser.id()));
        model.addAttribute("discordSettingsRequest", new DiscordSettingsRequest());
        return "pages/accounts/discord";
    }

    @GetMapping("/accounts/reddit")
    public String redditSetupPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("redditSettings", integrationStatusService.getRedditView(currentUser.id()));
        model.addAttribute("redditSettingsRequest", new RedditSettingsRequest());
        return "pages/accounts/reddit";
    }

    @GetMapping("/accounts/slack")
    public String slackSetupPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("slackSettings", integrationStatusService.getSlackView(currentUser.id()));
        model.addAttribute("slackSettingsRequest", new SlackSettingsRequest());
        return "pages/accounts/slack";
    }

    @GetMapping("/accounts/notion")
    public String notionSetupPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("notionSettings", integrationStatusService.getNotionView(currentUser.id()));
        model.addAttribute("notionSettingsRequest", new NotionSettingsRequest());
        return "pages/accounts/notion";
    }

    @GetMapping("/accounts/linkedin")
    public String linkedinSetupPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        model.addAttribute("linkedinSettings", integrationStatusService.getLinkedInView(currentUser.id()));
        model.addAttribute("linkedinSettingsRequest", new LinkedInSettingsRequest());
        return "pages/accounts/linkedin";
    }
}
