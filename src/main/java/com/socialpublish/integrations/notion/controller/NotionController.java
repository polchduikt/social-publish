package com.socialpublish.integrations.notion.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.integrations.notion.dto.NotionSettingsListRequest;
import com.socialpublish.integrations.notion.service.NotionService;
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
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class NotionController {

    private final NotionService notionService;
    private final HtmxSupport htmxSupport;

    @PostMapping("/accounts/notion")
    public String saveNotion(
            @CurrentUser CurrentUserView currentUser,
            @Valid @ModelAttribute("notionSettingsRequest") NotionSettingsListRequest requestList,
            BindingResult bindingResult,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        if (bindingResult.hasErrors()) {
            model.addAttribute("errorMessage", ValidationUtils.firstFieldError(bindingResult));
            if (isHtmx) return "fragments/integrations/notion-status";
            return "redirect:/accounts?error=Validation+failed";
        }

        try {
            notionService.saveSettings(currentUser.id(), requestList);

            if (isHtmx) {
                htmxSupport.redirectTo(httpResponse, "/accounts?message=Notion+connected+successfully");
                return "fragments/integrations/notion-status";
            }
            return "redirect:/accounts?message=Notion+connected+successfully";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed to save settings");
            if (isHtmx) return "fragments/integrations/notion-status";
            return "redirect:/accounts?error=Save+failed";
        }
    }

    @PostMapping("/accounts/notion/test")
    public String testNotion(
            @RequestParam(name = "targetAccountId") UUID targetAccountId,
            @RequestParam(name = "testMessage", defaultValue = "Hello from Social Publish!") String testMessage,
            HttpServletRequest httpRequest,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(httpRequest);

        try {
            notionService.testEntry(targetAccountId, testMessage);
            model.addAttribute("successMessage", "Test page created in Notion!");
            if (isHtmx) return "fragments/integrations/notion-status";
            return "redirect:/accounts?message=Test+successful";
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Failed: " + ex.getMessage());
            if (isHtmx) return "fragments/integrations/notion-status";
            return "redirect:/accounts?error=Test+failed";
        }
    }

    @PostMapping("/accounts/notion/disconnect")
    public String disconnectNotion(@CurrentUser CurrentUserView currentUser) {
        notionService.disconnect(currentUser.id());
        return "redirect:/accounts?message=Notion+disconnected";
    }
}
