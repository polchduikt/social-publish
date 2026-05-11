package com.socialpublish.integrations.reddit.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.integrations.reddit.dto.RedditSettingsRequest;
import com.socialpublish.integrations.reddit.service.RedditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class RedditAccountsController {

    private final RedditService redditService;

    @PostMapping("/accounts/reddit/disconnect")
    public String disconnectReddit(@CurrentUser CurrentUserView currentUser) {
        redditService.disconnect(currentUser.id());
        return "redirect:/accounts?message=Reddit+disconnected";
    }

    @PostMapping("/accounts/reddit/settings")
    public String updateRedditSettings(
            @CurrentUser CurrentUserView currentUser,
            @Valid RedditSettingsRequest request,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "redirect:/accounts/reddit?error=Validation+failed";
        }
        redditService.updateSettings(currentUser.id(), request);
        return "redirect:/accounts?message=Reddit+settings+updated";
    }
}
