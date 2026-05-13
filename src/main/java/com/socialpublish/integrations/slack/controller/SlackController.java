package com.socialpublish.integrations.slack.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.integrations.slack.dto.SlackSettingsListRequest;
import com.socialpublish.integrations.slack.service.SlackService;
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
public class SlackController {

    private final SlackService slackService;
    private final HtmxSupport htmxSupport;

    @PostMapping("/accounts/slack")
    public String saveSlack(
            @CurrentUser CurrentUserView currentUser,
            @Valid @ModelAttribute("slackSettingsRequest") SlackSettingsListRequest requestList,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtils.firstFieldError(bindingResult));
            if (isHtmx) return "fragments/integrations/slack-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Validation failed")
                    .build().toUriString();
        }

        try {
            slackService.saveSettings(currentUser.id(), requestList);

            String successUrl = UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("message", "Slack connected successfully")
                    .build().toUriString();

            if (isHtmx) {
                htmxSupport.redirectTo(httpResponse, successUrl);
                return "fragments/integrations/slack-status";
            }
            return "redirect:" + successUrl;
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to save settings");
            if (isHtmx) return "fragments/integrations/slack-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Save failed")
                    .build().toUriString();
        }
    }

    @PostMapping("/accounts/slack/test")
    public String testSlack(
            @RequestParam(name = "targetAccountId") UUID targetAccountId,
            @RequestParam(name = "testMessage", defaultValue = "Hello from Social Publish!") String testMessage,
            HttpServletRequest httpRequest,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        try {
            slackService.testMessage(targetAccountId, testMessage);
            model.addAttribute("successMessage", "Test message sent successfully!");
            if (isHtmx) return "fragments/integrations/slack-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("message", "Test successful")
                    .build().toUriString();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed: " + ex.getMessage());
            if (isHtmx) return "fragments/integrations/slack-status";
            return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                    .queryParam("error", "Test failed")
                    .build().toUriString();
        }
    }

    @PostMapping("/accounts/slack/disconnect")
    public String disconnectSlack(@CurrentUser CurrentUserView currentUser) {
        slackService.disconnect(currentUser.id());
        return "redirect:" + UriComponentsBuilder.fromPath("/accounts")
                .queryParam("message", "Slack disconnected")
                .build().toUriString();
    }
}
