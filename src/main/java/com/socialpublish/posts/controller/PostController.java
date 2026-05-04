package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.integrations.service.IntegrationStatusService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final HtmxSupport htmxSupport;
    private final IntegrationStatusService integrationStatusService;

    @GetMapping("/posts/new")
    public String createPostPage(@CurrentUser CurrentUserView currentUser, Model model) {
        PostUpsertRequest request = new PostUpsertRequest();
        populateFormModel(model, currentUser, "create", null, request);
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
            if (isHtmx) {
                htmxSupport.redirectTo(response, immediatePublish
                        ? "/dashboard?message=Post+queued+for+publishing"
                        : "/dashboard?message=Post+created");
                return "fragments/posts/form-status :: status";
            }
            return immediatePublish
                    ? "redirect:/dashboard?message=Post+queued+for+publishing"
                    : "redirect:/dashboard?message=Post+created";
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
            populateFormModel(model, currentUser, "edit", postId, request);
            model.addAttribute("existingMedia", postView.media());
            model.addAttribute("statuses", PostStatus.values());
            return "pages/posts/form";
        } catch (PostNotFoundException ex) {
            return "redirect:/dashboard?error=Post+not+found";
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
            if (isHtmx) {
                htmxSupport.redirectTo(response, immediatePublish
                        ? "/dashboard?message=Post+queued+for+publishing"
                        : "/dashboard?message=Post+updated");
                return "fragments/posts/form-status :: status";
            }
            return immediatePublish
                    ? "redirect:/dashboard?message=Post+queued+for+publishing"
                    : "redirect:/dashboard?message=Post+updated";
        } catch (PostValidationException ex) {
            return renderFormError(model, currentUser, "edit", postId, ex.getMessage(), isHtmx);
        } catch (PostNotFoundException ex) {
            return "redirect:/dashboard?error=Post+not+found";
        }
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@CurrentUser CurrentUserView currentUser, @PathVariable("id") UUID postId) {
        try {
            postService.deletePost(currentUser.id(), postId);
            return "redirect:/dashboard?message=Post+deleted";
        } catch (PostNotFoundException ex) {
            return "redirect:/dashboard?error=Post+not+found";
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
            model.addAttribute("user", currentUser);
            model.addAttribute("post", post);
            model.addAttribute("from", from == null || from.isBlank() ? "/" : from);
            return "pages/posts/preview";
        } catch (PostNotFoundException ex) {
            return "redirect:/dashboard?error=Post+not+found";
        }
    }

    private void populateFormModel(Model model, CurrentUserView user, String mode, UUID postId, PostUpsertRequest request) {
        model.addAttribute("user", user);
        model.addAttribute("mode", mode);
        if (request != null) {
            model.addAttribute("postRequest", request);
        }
        if (!model.containsAttribute("existingMedia")) {
            model.addAttribute("existingMedia", List.of());
        }
        model.addAttribute("statuses", PostStatus.userSettable());
        model.addAttribute("telegramConnected", integrationStatusService.isTelegramConnected(user.id()));
        model.addAttribute("discordConnected", integrationStatusService.isDiscordConnected(user.id()));
        model.addAttribute("redditConnected", integrationStatusService.isRedditConnected(user.id()));

        if (postId != null) {
            model.addAttribute("postId", postId);
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
        populateFormModel(model, currentUser, mode, postId, (PostUpsertRequest) model.asMap().get("postRequest"));
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
