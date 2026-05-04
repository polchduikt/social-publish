package com.socialpublish.auth.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.entity.User;
import com.socialpublish.auth.repository.UserRepository;
import com.socialpublish.common.web.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/settings")
    public String settingsPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        return "pages/settings/settings";
    }

    @PostMapping("/settings/profile")
    public String updateProfile(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email
    ) {
        String trimmedName = fullName.trim();
        String trimmedEmail = email.trim().toLowerCase();

        if (trimmedName.isBlank()) {
            return "redirect:/settings?error=Name+cannot+be+empty";
        }
        if (trimmedEmail.isBlank()) {
            return "redirect:/settings?error=Email+cannot+be+empty";
        }

        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!trimmedEmail.equals(user.getEmail()) && userRepository.existsByEmailIgnoreCase(trimmedEmail)) {
            return "redirect:/settings?error=Email+already+in+use";
        }

        user.setFullName(trimmedName);
        user.setEmail(trimmedEmail);
        userRepository.save(user);

        return "redirect:/settings?message=Profile+updated+successfully";
    }

    @PostMapping("/settings/password")
    public String changePassword(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword
    ) {
        if (!currentUser.passwordLoginEnabled()) {
            return "redirect:/settings?error=Password+change+not+available+for+OAuth+accounts";
        }

        if (newPassword.length() < 6) {
            return "redirect:/settings?error=Password+must+be+at+least+6+characters";
        }

        if (!newPassword.equals(confirmPassword)) {
            return "redirect:/settings?error=Passwords+do+not+match";
        }

        User user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return "redirect:/settings?error=Current+password+is+incorrect";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return "redirect:/settings?message=Password+changed+successfully";
    }

    @PostMapping("/settings/delete-account")
    public String deleteAccount(@CurrentUser CurrentUserView currentUser) {
        userRepository.deleteById(currentUser.id());
        return "redirect:/login?message=Account+deleted";
    }
}
