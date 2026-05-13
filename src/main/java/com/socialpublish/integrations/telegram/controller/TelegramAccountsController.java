package com.socialpublish.integrations.telegram.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsListRequest;
import com.socialpublish.integrations.telegram.service.TelegramService;
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
import org.springframework.web.util.UriComponentsBuilder;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class TelegramAccountsController {

    private final TelegramService telegramService;
    private final HtmxSupport htmxSupport;

    @PostMapping("/accounts/telegram")
    public String saveTelegram(
            @CurrentUser CurrentUserView currentUser,
            @Valid @ModelAttribute("settingsRequest") TelegramSettingsListRequest requestList,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtils.firstFieldError(bindingResult));
            if (isHtmx) return "fragments/integrations/telegram-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Validation failed")
                    .build().toUriString();
        }

        try {
            telegramService.saveSettings(currentUser.id(), requestList);

            String successUrl = UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("message", "Telegram connected successfully")
                    .build().toUriString();

            if (isHtmx) {
                htmxSupport.redirectTo(httpResponse, successUrl);
                return "fragments/integrations/telegram-status";
            }
            return "redirect:" + successUrl;
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to save settings");
            if (isHtmx) return "fragments/integrations/telegram-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Save failed")
                    .build().toUriString();
        }
    }

    @PostMapping("/accounts/telegram/test")
    public String testTelegram(
            @RequestParam(name = "targetAccountId") UUID targetAccountId,
            @RequestParam(name = "testMessage", defaultValue = "Hello from Social Publish!") String testMessage,
            HttpServletRequest httpRequest,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        try {
            telegramService.testMessage(targetAccountId, testMessage);
            model.addAttribute("successMessage", "Test message sent successfully!");
            if (isHtmx) return "fragments/integrations/telegram-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("message", "Test message sent")
                    .build().toUriString();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed: " + ex.getMessage());
            if (isHtmx) return "fragments/integrations/telegram-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Test failed")
                    .build().toUriString();
        }
    }

    @PostMapping("/accounts/telegram/disconnect")
    public String disconnectTelegram(@CurrentUser CurrentUserView currentUser) {
        telegramService.disconnect(currentUser.id());
        return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                .queryParam("message", "Telegram disconnected")
                .build().toUriString();
    }
}
