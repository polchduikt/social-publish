package com.socialpublish.integrations.reddit.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.integrations.reddit.dto.RedditSettingsRequest;
import com.socialpublish.integrations.reddit.repository.RedditSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class RedditAccountsController {

    private final RedditSettingsRepository settingsRepository;

    @PostMapping("/accounts/reddit/disconnect")
    public String disconnectReddit(@CurrentUser CurrentUserView currentUser) {
        settingsRepository.findByUserId(currentUser.id())
                .ifPresent(settingsRepository::delete);
        return "redirect:/accounts?message=Reddit+disconnected";
    }

    @PostMapping("/accounts/reddit/settings")
    public String updateRedditSettings(
            @CurrentUser CurrentUserView currentUser,
            RedditSettingsRequest request) {
        settingsRepository.findByUserId(currentUser.id()).ifPresent(settings -> {
            settings.setDefaultSubreddit(request.getDefaultSubreddit());
            settings.setEnabled(request.isEnabled());
            settingsRepository.save(settings);
        });
        return "redirect:/accounts?message=Reddit+settings+updated";
    }
}
