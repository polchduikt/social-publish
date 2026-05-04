package com.socialpublish.integrations.telegram.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsRequest;
import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.integrations.telegram.service.TelegramClientService;
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

@Controller
@RequiredArgsConstructor
public class AccountsController {

    private final TelegramSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final TelegramClientService telegramClient;
    private final HtmxSupport htmxSupport;

    @PostMapping("/accounts/telegram")
    public String saveTelegram(
            @CurrentUser CurrentUserView currentUser,
            @Valid @ModelAttribute("settingsRequest") TelegramSettingsRequest request,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtils.firstFieldError(bindingResult));
            if (isHtmx) return "fragments/integrations/telegram-status";
            return "redirect:/accounts?error=Validation+failed";
        }

        try {
            TelegramSettingsEntity settings = settingsRepository.findByUserId(currentUser.id())
                    .orElseGet(() -> {
                        TelegramSettingsEntity s = new TelegramSettingsEntity();
                        User user = userRepository.findById(currentUser.id()).orElseThrow();
                        s.setUser(user);
                        return s;
                    });

            settings.setBotToken(request.getBotToken().trim());
            settings.setChatId(request.getChatId().trim());
            settings.setEnabled(request.isEnabled());
            settingsRepository.save(settings);

            if (isHtmx) {
                htmxSupport.redirectTo(httpResponse, "/accounts?message=Telegram+connected+successfully");
                return "fragments/integrations/telegram-status";
            }
            return "redirect:/accounts?message=Telegram+connected+successfully";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to save settings");
            if (isHtmx) return "fragments/integrations/telegram-status";
            return "redirect:/accounts?error=Save+failed";
        }
    }

    @PostMapping("/accounts/telegram/test")
    public String testTelegram(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(name = "testMessage", defaultValue = "Hello from Social Publish!") String testMessage,
            HttpServletRequest httpRequest,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        TelegramSettingsEntity settings = settingsRepository.findByUserId(currentUser.id()).orElse(null);
        if (settings == null || !settings.isEnabled()) {
            model.addAttribute("errorMessage", "Telegram is not configured or disabled");
            if (isHtmx) return "fragments/integrations/telegram-status";
            return "redirect:/accounts?error=Not+configured";
        }

        try {
            telegramClient.sendMessage(settings.getBotToken(), settings.getChatId(), testMessage);
            model.addAttribute("successMessage", "Test message sent successfully!");
            if (isHtmx) return "fragments/integrations/telegram-status";
            return "redirect:/accounts?message=Test+message+sent";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed: " + ex.getMessage());
            if (isHtmx) return "fragments/integrations/telegram-status";
            return "redirect:/accounts?error=Test+failed";
        }
    }

    @PostMapping("/accounts/telegram/disconnect")
    public String disconnectTelegram(@CurrentUser CurrentUserView currentUser) {
        settingsRepository.findByUserId(currentUser.id())
                .ifPresent(settingsRepository::delete);
        return "redirect:/accounts?message=Telegram+disconnected";
    }
}
