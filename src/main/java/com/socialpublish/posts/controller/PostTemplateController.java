package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.posts.dto.CreatePostTemplateRequest;
import com.socialpublish.posts.dto.PostTemplateDto;
import com.socialpublish.posts.service.PostTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class PostTemplateController {

    private final PostTemplateService postTemplateService;

    @GetMapping
    public ResponseEntity<List<PostTemplateDto>> getTemplates(@CurrentUser CurrentUserView user) {
        return ResponseEntity.ok(postTemplateService.getUserTemplates(user.id()));
    }

    @PostMapping
    public ResponseEntity<PostTemplateDto> createTemplate(@CurrentUser CurrentUserView user, 
                                                          @Valid @RequestBody CreatePostTemplateRequest request) {
        return ResponseEntity.ok(postTemplateService.createTemplate(user.id(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@CurrentUser CurrentUserView user, 
                                               @PathVariable UUID id) {
        postTemplateService.deleteTemplate(user.id(), id);
        return ResponseEntity.ok().build();
    }
}
