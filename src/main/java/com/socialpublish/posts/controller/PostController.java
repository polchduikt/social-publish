package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.common.web.HtmxSupport;
import com.socialpublish.common.web.ValidationUtils;
import com.socialpublish.integrations.telegram.dto.TelegramSettingsView;
import com.socialpublish.integrations.telegram.repository.TelegramSettingsRepository;
import com.socialpublish.integrations.discord.dto.DiscordSettingsView;
import com.socialpublish.integrations.discord.repository.DiscordSettingsRepository;
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
    private final TelegramSettingsRepository telegramSettingsRepository;
    private final DiscordSettingsRepository discordSettingsRepository;

    public PostController(
            PostService postService,
            HtmxSupport htmxSupport,
            TelegramSettingsRepository telegramSettingsRepository,
            DiscordSettingsRepository discordSettingsRepository
    ) {
        this.postService = postService;
        this.htmxSupport = htmxSupport;
        this.telegramSettingsRepository = telegramSettingsRepository;
        this.discordSettingsRepository = discordSettingsRepository;
    }

    @GetMapping("/posts/new")
    public String createPage(@CurrentUser CurrentUserView currentUser, Model model) {
        populateFormModel(model, currentUser, "create", null, new PostUpsertRequest());
        return "pages/posts/form";
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
        boolean isHtmx = htmxSupport.isHtmxRequest(request);
        if (bindingResult.hasErrors()) {
            return renderFormError(model, currentUser, "create", null, ValidationUtils.firstFieldError(bindingResult), isHtmx);
        }

        try {
            postService.createPost(currentUser.id(), postRequest);
            if (isHtmx) {
                htmxSupport.redirectTo(response, "/dashboard?message=Post+created");
                return "fragments/posts/form-status :: status";
            }
            return "redirect:/dashboard?message=Post+created";
        } catch (PostValidationException ex) {
            return renderFormError(model, currentUser, "create", null, ex.getMessage(), isHtmx);
        } catch (UnauthorizedPostAccessException ex) {
            return "redirect:/login";
        }
    }

    @GetMapping("/posts/{id}/edit")
    public String editPage(@CurrentUser CurrentUserView currentUser, @PathVariable("id") UUID postId, Model model) {
        try {
            PostUpsertRequest request = postService.getEditRequest(currentUser.id(), postId);
            populateFormModel(model, currentUser, "edit", postId, request);
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
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        boolean isHtmx = htmxSupport.isHtmxRequest(request);
        if (bindingResult.hasErrors()) {
            return renderFormError(model, currentUser, "edit", postId, ValidationUtils.firstFieldError(bindingResult), isHtmx);
        }

        try {
            postService.updatePost(currentUser.id(), postId, postRequest);
            if (isHtmx) {
                htmxSupport.redirectTo(response, "/dashboard?message=Post+updated");
                return "fragments/posts/form-status :: status";
            }
            return "redirect:/dashboard?message=Post+updated";
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
        TelegramSettingsView telegram = telegramSettingsRepository.findByUserId(user.id())
                .map(TelegramSettingsView::from)
                .orElse(TelegramSettingsView.empty());

        DiscordSettingsView discord = discordSettingsRepository.findByUserId(user.id())
                .map(DiscordSettingsView::from)
                .orElse(DiscordSettingsView.empty());

        model.addAttribute("user", user);
        model.addAttribute("mode", mode);
        if (request != null) {
            model.addAttribute("postRequest", request);
        }
        model.addAttribute("statuses", PostStatus.userSettable());
        model.addAttribute("telegramConnected", telegram.configured() && telegram.enabled());
        model.addAttribute("discordConnected", discord.configured() && discord.enabled());
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
        return "pages/posts/form";
    }
}
