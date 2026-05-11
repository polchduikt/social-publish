package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.posts.service.PostTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.UUID;

@Controller
@RequestMapping("/templates")
@RequiredArgsConstructor
public class TemplatesPageController {

    private final PostTemplateService postTemplateService;

    @GetMapping
    public String templatesPage(@CurrentUser CurrentUserView user, Model model) {
        model.addAttribute("user", user);
        model.addAttribute("templates", postTemplateService.getUserTemplates(user.id()));
        return "pages/templates/index";
    }

    @PostMapping("/{id}/delete")
    public String deleteTemplate(@CurrentUser CurrentUserView user, @PathVariable("id") UUID id) {
        postTemplateService.deleteTemplate(user.id(), id);
        return "redirect:/templates?toast=deleted";
    }
}
