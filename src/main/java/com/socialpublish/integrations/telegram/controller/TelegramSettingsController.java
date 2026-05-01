package com.socialpublish.integrations.telegram.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsRequest;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsView;
import com.socialpublish.integrations.telegram.entity.TelegramSettingsEntity;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.integrations.telegram.service.TelegramClientService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/settings/telegram")
public class TelegramSettingsController {

    private final TelegramSettingsRepository settingsRepository;
    private final UserRepository userRepository;
    private final TelegramClientService telegramClient;
    private final HtmxSupport htmxSupport;

    public TelegramSettingsController(
            TelegramSettingsRepository settingsRepository,
            UserRepository userRepository,
            TelegramClientService telegramClient,
            HtmxSupport htmxSupport
    ) {
        this.settingsRepository = settingsRepository;
        this.userRepository = userRepository;
        this.telegramClient = telegramClient;
        this.htmxSupport = htmxSupport;
    }

    @GetMapping
    public String settingsPage(@CurrentUser CurrentUserView currentUser, Model model) {
        TelegramSettingsView view = settingsRepository.findByUserId(currentUser.id())
                .map(TelegramSettingsView::from)
                .orElse(TelegramSettingsView.empty());

        model.addAttribute("user", currentUser);
        model.addAttribute("telegramSettings", view);
        model.addAttribute("settingsRequest", new TelegramSettingsRequest());
        return "integrations/telegram";
    }

    @PostMapping
    public String saveSettings(
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
            if (isHtmx) {
                return "fragments/integrations/telegram-status";
            }
            return "redirect:/settings/telegram?error=Validation+failed";
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
                htmxSupport.redirectTo(httpResponse, "/settings/telegram?message=Settings+saved+successfully");
                return "fragments/integrations/telegram-status";
            }
            return "redirect:/settings/telegram?message=Settings+saved+successfully";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to save settings");
            if (isHtmx) {
                return "fragments/integrations/telegram-status";
            }
            return "redirect:/settings/telegram?error=Save+failed";
        }
    }

    @PostMapping("/test")
    public String sendTestMessage(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(name = "testMessage", defaultValue = "Hello from SocialAuto! 🚀") String testMessage,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        TelegramSettingsEntity settings = settingsRepository.findByUserId(currentUser.id()).orElse(null);
        if (settings == null || !settings.isEnabled()) {
            model.addAttribute("errorMessage", "Telegram is not configured or disabled");
            if (isHtmx) {
                return "fragments/integrations/telegram-status";
            }
            return "redirect:/settings/telegram?error=Not+configured";
        }

        try {
            telegramClient.sendMessage(settings.getBotToken(), settings.getChatId(), testMessage);
            model.addAttribute("successMessage", "✅ Test message sent successfully!");
            if (isHtmx) {
                return "fragments/integrations/telegram-status";
            }
            return "redirect:/settings/telegram?message=Test+message+sent";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "❌ Failed: " + ex.getMessage());
            if (isHtmx) {
                return "fragments/integrations/telegram-status";
            }
            return "redirect:/settings/telegram?error=Test+failed";
        }
    }
}
