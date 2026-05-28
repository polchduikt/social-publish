package com.socialpublish.integrations.discord.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.integrations.discord.dto.DiscordSettingsListRequest;
import com.socialpublish.integrations.discord.service.DiscordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class DiscordController {

    private final DiscordService discordService;
    private final HtmxSupport htmxSupport;

    @PostMapping("/accounts/discord")
    public String saveDiscord(
            @CurrentUser CurrentUserView currentUser,
            @Valid @ModelAttribute("discordSettingsRequest") DiscordSettingsListRequest requestList,
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
            discordService.saveSettings(currentUser.id(), requestList);

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
            @RequestParam(name = "targetAccountId") UUID targetAccountId,
            @RequestParam(name = "testMessage", defaultValue = "Hello from Social Publish!") String testMessage,
            HttpServletRequest httpRequest,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        try {
            discordService.testMessage(targetAccountId, testMessage);
            model.addAttribute("successMessage", "Test message sent successfully!");
            if (isHtmx) return "fragments/integrations/discord-status";
            return "redirect:/accounts?message=Test+message+sent";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed: " + ex.getMessage());
            if (isHtmx) return "fragments/integrations/discord-status";
            return "redirect:/accounts?error=Test+failed";
        }
    }

    @PostMapping("/accounts/discord/disconnect")
    public String disconnectDiscord(@CurrentUser CurrentUserView currentUser) {
        discordService.disconnect(currentUser.id());
        return "redirect:/accounts?message=Discord+disconnected";
    }
}
