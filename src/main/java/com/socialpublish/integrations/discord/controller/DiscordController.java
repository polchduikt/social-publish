package com.socialpublish.integrations.discord.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.integrations.discord.dto.DiscordSettingsRequest;
import com.socialpublish.integrations.discord.entity.DiscordSettingsEntity;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
import com.socialpublish.integrations.discord.service.DiscordClientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class DiscordController {

    private final DiscordSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final DiscordClientService discordClient;
    private final HtmxSupport htmxSupport;

    public DiscordController(
            DiscordSettingsRepository settingsRepository,
            UserRepository userRepository,
            DiscordClientService discordClient,
            HtmxSupport htmxSupport
    ) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.discordClient = discordClient;
        this.htmxSupport = htmxSupport;
    }

    @PostMapping("/accounts/discord")
    public String saveDiscord(
            @CurrentUser CurrentUserView currentUser,
            @Valid @ModelAttribute("discordSettingsRequest") DiscordSettingsRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtils.firstFieldError(bindingResult));
            if (isHtmx) return "fragments/integrations/discord-status";
            return "redirect:/accounts?error=Validation+failed";
        }

        try {
            DiscordSettingsEntity settings = settingsRepository.findByUserId(currentUser.id())
                    .orElseGet(() -> {
                        DiscordSettingsEntity s = new DiscordSettingsEntity();
                        User user = userRepository.findById(currentUser.id()).orElseThrow();
                        s.setUser(user);
                        return s;
                    });

            settings.setWebhookUrl(request.getWebhookUrl().trim());
            settings.setEnabled(request.isEnabled());
            settingsRepository.save(settings);

            if (isHtmx) {
                htmxSupport.redirectTo(httpResponse, "/accounts?message=Discord+connected+successfully");
                return "fragments/integrations/discord-status";
            }
            return "redirect:/accounts?message=Discord+connected+successfully";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to save settings");
            if (isHtmx) return "fragments/integrations/discord-status";
            return "redirect:/accounts?error=Save+failed";
        }
    }

    @PostMapping("/accounts/discord/test")
    public String testDiscord(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(name = "testMessage", defaultValue = "Hello from Social Publish!") String testMessage,
            HttpServletRequest httpRequest,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        DiscordSettingsEntity settings = settingsRepository.findByUserId(currentUser.id()).orElse(null);
        if (settings == null || !settings.isEnabled()) {
            model.addAttribute("errorMessage", "Discord is not configured or disabled");
            if (isHtmx) return "fragments/integrations/discord-status";
            return "redirect:/accounts?error=Not+configured";
        }

        try {
            discordClient.sendMessage(settings.getWebhookUrl(), testMessage);
            model.addAttribute("successMessage", "Test message sent successfully!");
            if (isHtmx) return "fragments/integrations/discord-status";
            return "redirect:/accounts?message=Test+message+sent";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed: " + ex.getMessage());
            if (isHtmx) return "fragments/integrations/discord-status";
            return "redirect:/accounts?error=Test+failed";
        }
    }
}
