package com.socialpublish.integrations.reddit.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.integrations.reddit.service.RedditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Controller
@RequestMapping("/integrations/reddit")
@RequiredArgsConstructor
public class RedditOAuthController {

    private final RedditService redditService;

    @GetMapping("/authorize")
    public String authorize() {
        return "redirect:" + redditService.getAuthorizationUrl();
    }

    @GetMapping("/callback")
    public String callback(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error
    ) {
        if (error != null) {
            log.error("Reddit OAuth error: {}", error);
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Reddit authorization failed")
                    .build().toUriString();
        }

        if (code == null) {
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Missing authorization code")
                    .build().toUriString();
        }

        try {
            redditService.connectAccount(currentUser.id(), code);
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("message", "Reddit connected successfully")
                    .build().toUriString();
        } catch (Exception e) {
            log.error("Error during Reddit token exchange", e);
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Failed to connect Reddit")
                    .build().toUriString();
        }
    }
}
