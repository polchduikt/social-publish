package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.exception.PostNotFoundException;
import com.socialpublish.posts.exception.PostValidationException;
import com.socialpublish.posts.exception.UnauthorizedPostAccessException;
import com.socialpublish.posts.service.PostService;
import com.socialpublish.posts.service.PostTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final HtmxSupport htmxSupport;
    private final PostFormSupport postFormSupport;
    private final PostTemplateService postTemplateService;

    @GetMapping("/posts/new")
    public String createPostPage(@CurrentUser CurrentUserView currentUser,
                                 @RequestParam(name = "templateId", required = false) UUID templateId,
                                 Model model) {
        PostUpsertRequest request = (templateId != null)
                ? postTemplateService.createUpsertRequest(currentUser.id(), templateId)
                : new PostUpsertRequest();

        postFormSupport.populateFormModel(model, currentUser, "create", null, request);
        return "pages/posts/form";
    }

    @PostMapping("/posts")
    public String createPost(
            @CurrentUser CurrentUserView currentUser,
            @Valid @ModelAttribute("postRequest") PostUpsertRequest postRequest,
            BindingResult bindingResult,
            @RequestParam(name = "publishNow", required = false) Boolean publishNow,
            @RequestParam(name = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(request);
        if (bindingResult.hasErrors()) {
            return renderFormError(model, currentUser, "create", null, ValidationUtils.firstFieldError(bindingResult), isHtmx);
        }

        try {
            boolean immediatePublish = Boolean.TRUE.equals(publishNow);
            if (immediatePublish) {
                postService.createPostAndPublishNow(currentUser.id(), postRequest, mediaFiles);
            } else {
                postService.createPost(currentUser.id(), postRequest, mediaFiles);
            }

            String message = immediatePublish ? "Post queued for publishing" : "Post created";
            String redirectUrl = UriComponentsBuilder.fromPath("/dashboard")
                    .queryParam("message", message)
                    .build().toUriString();

            if (isHtmx) {
                htmxSupport.redirectTo(response, redirectUrl);
                return "fragments/posts/form-status :: status";
            }
            return "redirect:" + redirectUrl;
        } catch (PostValidationException ex) {
            return renderFormError(model, currentUser, "create", null, ex.getMessage(), isHtmx);
        } catch (UnauthorizedPostAccessException ex) {
            return "redirect:/login";
        }
    }

    @GetMapping("/posts/{id}/edit")
    public String editPostPage(@CurrentUser CurrentUserView currentUser, @PathVariable("id") UUID postId, Model model) {
        try {
            PostUpsertRequest request = postService.getEditRequest(currentUser.id(), postId);
            PostView postView = postService.getPostView(currentUser.id(), postId);
            postFormSupport.populateFormModel(model, currentUser, "edit", postId, request);
            model.addAttribute("existingMedia", postView.media());
            model.addAttribute("statuses", List.of("DRAFT", "SCHEDULED", "PUBLISHING", "PUBLISHED", "RETRYING", "FAILED", "CANCELLED"));
            return "pages/posts/form";
        } catch (PostNotFoundException ex) {
            return "redirect:" + UriComponentsBuilder.fromPath("/dashboard")
                    .queryParam("error", "Post not found")
                    .build().toUriString();
        }
    }

    @PostMapping("/posts/{id}")
    public String updatePost(
            @CurrentUser CurrentUserView currentUser,
            @PathVariable("id") UUID postId,
            @Valid @ModelAttribute("postRequest") PostUpsertRequest postRequest,
            BindingResult bindingResult,
            @RequestParam(name = "publishNow", required = false) Boolean publishNow,
            @RequestParam(name = "mediaFiles", required = false) List<MultipartFile> mediaFiles,
            @RequestParam(name = "removeMediaPublicIds", required = false) List<String> removeMediaPublicIds,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(request);
        if (bindingResult.hasErrors()) {
            return renderFormError(model, currentUser, "edit", postId, ValidationUtils.firstFieldError(bindingResult), isHtmx);
        }

        try {
            boolean immediatePublish = Boolean.TRUE.equals(publishNow);
            if (immediatePublish) {
                postService.updatePostAndPublishNow(currentUser.id(), postId, postRequest, mediaFiles, removeMediaPublicIds);
            } else {
                postService.updatePost(currentUser.id(), postId, postRequest, mediaFiles, removeMediaPublicIds);
            }

            String message = immediatePublish ? "Post queued for publishing" : "Post updated";
            String redirectUrl = UriComponentsBuilder.fromPath("/dashboard")
                    .queryParam("message", message)
                    .build().toUriString();

            if (isHtmx) {
                htmxSupport.redirectTo(response, redirectUrl);
                return "fragments/posts/form-status :: status";
            }
            return "redirect:" + redirectUrl;
        } catch (PostValidationException ex) {
            return renderFormError(model, currentUser, "edit", postId, ex.getMessage(), isHtmx);
        } catch (PostNotFoundException ex) {
            return "redirect:" + UriComponentsBuilder.fromPath("/dashboard")
                    .queryParam("error", "Post not found")
                    .build().toUriString();
        }
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@CurrentUser CurrentUserView currentUser, @PathVariable("id") UUID postId) {
        try {
            postService.deletePost(currentUser.id(), postId);
            return "redirect:" + UriComponentsBuilder.fromPath("/dashboard")
                    .queryParam("message", "Post deleted")
                    .build().toUriString();
        } catch (PostNotFoundException ex) {
            return "redirect:" + UriComponentsBuilder.fromPath("/dashboard")
                    .queryParam("error", "Post not found")
                    .build().toUriString();
        }
    }


    private String renderFormError(
            Model model,
            CurrentUserView currentUser,
            String mode,
            UUID postId,
            String errorMessage,
            boolean isHtmx
    ) {
        model.addAttribute("errorMessage", errorMessage);
        if (isHtmx) {
            return "fragments/posts/form-status :: status";
        }
        postFormSupport.populateFormModel(model, currentUser, mode, postId, (PostUpsertRequest) model.asMap().get("postRequest"));
        if (postId != null) {
            try {
                model.addAttribute("existingMedia", postService.getPostView(currentUser.id(), postId).media());
            } catch (PostNotFoundException ignored) {
                model.addAttribute("existingMedia", List.of());
            }
        }
        return "pages/posts/form";
    }
}
