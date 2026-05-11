package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.posts.dto.CreatePostTemplateRequest;
import com.socialpublish.posts.service.PostTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PostTemplateWebController {

    private final PostTemplateService postTemplateService;

    @PostMapping("/posts/templates")
    @ResponseBody
    public ResponseEntity<Void> saveTemplate(
            @CurrentUser CurrentUserView user,
            @RequestParam("templateName") String templateName,
            @RequestParam(name = "platforms", required = false) List<String> platforms,
            @RequestParam("content") String content
    ) {
        CreatePostTemplateRequest req = new CreatePostTemplateRequest(
                templateName,
                content,
                platforms != null ? String.join(",", platforms) : ""
        );
        postTemplateService.createTemplate(user.id(), req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/posts/templates/{id}")
    public String deleteTemplate(
            @CurrentUser CurrentUserView user,
            @PathVariable("id") UUID id,
            Model model
    ) {
        postTemplateService.deleteTemplate(user.id(), id);
        model.addAttribute("templates", postTemplateService.getUserTemplates(user.id()));
        model.addAttribute("openTemplates", true);
        return "fragments/posts/templates :: templates-list";
    }
}
