package com.socialpublish.integrations.linkedin.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.integrations.linkedin.service.LinkedInService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class LinkedInController {

    private final LinkedInService linkedInService;
    private final HtmxSupport htmxSupport;

    @GetMapping("/accounts/linkedin/connect")
    public RedirectView connect() {
        return new RedirectView(linkedInService.getAuthorizationUrl());
    }

    @GetMapping("/integrations/linkedin/callback")
    public String callback(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(name = "code", required = false) String code,
            @RequestParam(name = "error", required = false) String error
    ) {
        if (error != null || code == null) {
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "LinkedIn connection cancelled")
                    .build().toUriString();
        }

        try {
            linkedInService.connectAccount(currentUser.id(), code);
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("message", "LinkedIn connected successfully")
                    .build().toUriString();
        } catch (Exception ex) {
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "LinkedIn connection failed")
                    .build().toUriString();
        }
    }

    @PostMapping("/accounts/linkedin/test")
    public String testLinkedIn(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(name = "testMessage", defaultValue = "Hello from Social Publish!") String testMessage,
            HttpServletRequest httpRequest,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        try {
            linkedInService.testPost(currentUser.id(), testMessage);
            model.addAttribute("successMessage", "Test post shared on LinkedIn!");
            if (isHtmx) return "fragments/integrations/linkedin-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("message", "Test successful")
                    .build().toUriString();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed: " + ex.getMessage());
            if (isHtmx) return "fragments/integrations/linkedin-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Test failed")
                    .build().toUriString();
        }
    }

    @PostMapping("/accounts/linkedin/disconnect")
    public String disconnectLinkedIn(@CurrentUser CurrentUserView currentUser) {
        linkedInService.disconnectAccount(currentUser.id());
        return "redirect:/accounts?message=LinkedIn+disconnected";
    }
}
