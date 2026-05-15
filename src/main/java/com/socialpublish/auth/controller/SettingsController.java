package com.socialpublish.auth.controller;

import com.socialpublish.auth.dto.ChangePasswordRequest;
import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.dto.UpdateProfileRequest;
import com.socialpublish.auth.exception.SettingsOperationException;
import com.socialpublish.auth.service.SettingsService;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.ValidationUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/settings")
    public String settingsPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        return "pages/settings/settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(
            @CurrentUser CurrentUserView currentUser,
            @Valid UpdateProfileRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "redirect:" + UriComponentsBuilder.fromPath("/settings")
                    .queryParam("error", ValidationUtils.firstFieldError(bindingResult))
                    .build().toUriString();
        }

        try {
            settingsService.updateProfile(currentUser.id(), request);
            return "redirect:" + UriComponentsBuilder.fromPath("/settings")
                    .queryParam("message", "Profile updated successfully")
                    .build().toUriString();
        } catch (SettingsOperationException ex) {
            return "redirect:" + UriComponentsBuilder.fromPath("/settings")
                    .queryParam("error", ex.getMessage())
                    .build().toUriString();
        }
    }

    @PostMapping("/settings/password")
    public String changePassword(
            @CurrentUser CurrentUserView currentUser,
            @Valid ChangePasswordRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return "redirect:" + UriComponentsBuilder.fromPath("/settings")
                    .queryParam("error", ValidationUtils.firstFieldError(bindingResult))
                    .build().toUriString();
        }

        try {
            settingsService.changePassword(currentUser.id(), request);
            return "redirect:" + UriComponentsBuilder.fromPath("/settings")
                    .queryParam("message", "Password changed successfully")
                    .build().toUriString();
        } catch (SettingsOperationException ex) {
            return "redirect:" + UriComponentsBuilder.fromPath("/settings")
                    .queryParam("error", ex.getMessage())
                    .build().toUriString();
        }
    }

    @PostMapping("/settings/ai")
    public String updateAiAssistant(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(name = "aiAssistantEnabled", defaultValue = "false") boolean aiAssistantEnabled
    ) {
        settingsService.updateAiAssistantEnabled(currentUser.id(), aiAssistantEnabled);
        return "redirect:" + UriComponentsBuilder.fromPath("/settings")
                .queryParam("message", "AI assistant settings updated")
                .build().toUriString();
    }

    @PostMapping("/settings/delete-account")
    public String deleteAccount(@CurrentUser CurrentUserView currentUser) {
        settingsService.deleteAccount(currentUser.id());
        return "redirect:/login?message=Account+deleted";
    }
}
