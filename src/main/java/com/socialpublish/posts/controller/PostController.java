package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.posts.dto.PostUpsertRequest;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.exception.PostNotFoundException;
import com.socialpublish.posts.exception.PostValidationException;
import com.socialpublish.posts.exception.UnauthorizedPostAccessException;
import com.socialpublish.posts.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Controller
public class PostController {

    private final PostService postService;
    private final HtmxSupport htmxSupport;

    public PostController(PostService postService, HtmxSupport htmxSupport) {
        this.postService = postService;
        this.htmxSupport = htmxSupport;
    }

    @GetMapping("/posts/new")
    public String createPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("mode", "create");
        model.addAttribute("postRequest", new PostUpsertRequest());
        model.addAttribute("statuses", PostStatus.values());
        return "posts/form";
    }

    @PostMapping("/posts")
    public String createPost(
            @CurrentUser CurrentUserView currentUser,
            @Valid @ModelAttribute("postRequest") PostUpsertRequest postRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        boolean isHtmxRequest = htmxSupport.isHtmxRequest(request);
        if (bindingResult.hasErrors()) {
            return renderFormError(model, "create", null, ValidationUtils.firstFieldError(bindingResult), isHtmxRequest);
        }

        try {
            postService.createPost(currentUser.id(), postRequest);
            if (isHtmxRequest) {
                htmxSupport.redirectTo(response, "/?message=Post+created");
                return "fragments/posts/form-status :: status";
            }
            return "redirect:/?message=Post+created";
        } catch (PostValidationException ex) {
            return renderFormError(model, "create", null, ex.getMessage(), isHtmxRequest);
        } catch (UnauthorizedPostAccessException ex) {
            return "redirect:/login";
        }
    }

    @GetMapping("/posts/{id}/edit")
    public String editPage(@CurrentUser CurrentUserView currentUser, @PathVariable("id") UUID postId, Model model) {
        try {
            PostUpsertRequest request = postService.getEditRequest(currentUser.id(), postId);
            model.addAttribute("mode", "edit");
            model.addAttribute("postId", postId);
            model.addAttribute("postRequest", request);
            model.addAttribute("statuses", PostStatus.values());
            return "posts/form";
        } catch (PostNotFoundException ex) {
            return "redirect:/?error=Post+not+found";
        }
    }

    @PostMapping("/posts/{id}")
    public String updatePost(
            @CurrentUser CurrentUserView currentUser,
            @PathVariable("id") UUID postId,
            @Valid @ModelAttribute("postRequest") PostUpsertRequest postRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        boolean isHtmxRequest = htmxSupport.isHtmxRequest(request);
        if (bindingResult.hasErrors()) {
            return renderFormError(model, "edit", postId, ValidationUtils.firstFieldError(bindingResult), isHtmxRequest);
        }

        try {
            postService.updatePost(currentUser.id(), postId, postRequest);
            if (isHtmxRequest) {
                htmxSupport.redirectTo(response, "/?message=Post+updated");
                return "fragments/posts/form-status :: status";
            }
            return "redirect:/?message=Post+updated";
        } catch (PostValidationException ex) {
            return renderFormError(model, "edit", postId, ex.getMessage(), isHtmxRequest);
        } catch (PostNotFoundException ex) {
            return "redirect:/?error=Post+not+found";
        }
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@CurrentUser CurrentUserView currentUser, @PathVariable("id") UUID postId) {
        try {
            postService.deletePost(currentUser.id(), postId);
            return "redirect:/?message=Post+deleted";
        } catch (PostNotFoundException ex) {
            return "redirect:/?error=Post+not+found";
        }
    }

    @GetMapping("/posts/{id}/preview")
    public String previewPost(
            @CurrentUser CurrentUserView currentUser,
            @PathVariable("id") UUID postId,
            @RequestParam(name = "from", required = false) String from,
            Model model
    ) {
        try {
            PostView post = postService.getPostView(currentUser.id(), postId);
            model.addAttribute("post", post);
            model.addAttribute("from", from == null || from.isBlank() ? "/" : from);
            return "posts/preview";
        } catch (PostNotFoundException ex) {
            return "redirect:/?error=Post+not+found";
        }
    }

    private String renderFormError(
            Model model,
            String mode,
            UUID postId,
            String errorMessage,
            boolean isHtmxRequest
    ) {
        model.addAttribute("errorMessage", errorMessage);
        if (isHtmxRequest) {
            return "fragments/posts/form-status :: status";
        }

        model.addAttribute("mode", mode);
        if (postId != null) {
            model.addAttribute("postId", postId);
        }
        model.addAttribute("statuses", PostStatus.values());
        return "posts/form";
    }
}
