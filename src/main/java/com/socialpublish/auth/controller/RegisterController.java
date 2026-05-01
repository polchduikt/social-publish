package com.socialpublish.auth.controller;

import com.socialpublish.auth.dto.RegisterRequest;
import com.socialpublish.auth.service.AuthService;
import com.socialpublish.auth.service.AuthenticatedUserService;
import com.socialpublish.common.web.HtmxSupport;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class RegisterController {

    private final AuthService authService;
    private final AuthenticatedUserService authenticatedUserService;
    private final HtmxSupport htmxSupport;

    public RegisterController(
            AuthService authService,
            AuthenticatedUserService authenticatedUserService,
            HtmxSupport htmxSupport
    ) {
        this.authService = authService;
        this.authenticatedUserService = authenticatedUserService;
        this.htmxSupport = htmxSupport;
    }

    @GetMapping("/register")
    public String registerPage(
            Authentication authentication,
            @RequestParam(name = "error", required = false) String error,
            Model model
    ) {
        if (authenticatedUserService.resolveForView(authentication).isPresent()) {
            return "redirect:/";
        }
        model.addAttribute("error", error != null);
        model.addAttribute("registerRequest", new RegisterRequest());
        return "register";
    }

    @PostMapping("/register")
    public String registerSubmit(
            @Valid @ModelAttribute("registerRequest") RegisterRequest registerRequest,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {
        boolean isHtmxRequest = htmxSupport.isHtmxRequest(request);

        if (bindingResult.hasErrors()) {
            return registerValidationError(isHtmxRequest, model, firstValidationMessage(bindingResult));
        }

        try {
            authService.register(registerRequest);
            if (isHtmxRequest) {
                htmxSupport.redirectTo(response, "/login?registered=true");
                return "fragments/auth/register-status";
            }
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            if (!isHtmxRequest) {
                return "redirect:/register?error=true";
            }
            model.addAttribute("errorMessage", ex.getMessage());
            return "fragments/auth/register-status";
        }
    }

    private String firstValidationMessage(BindingResult bindingResult) {
        if (bindingResult.getFieldError() != null) {
            return bindingResult.getFieldError().getDefaultMessage();
        }
        return "Validation failed";
    }

    private String registerValidationError(boolean isHtmxRequest, Model model, String message) {
        if (!isHtmxRequest) {
            return "redirect:/register?error=true";
        }
        model.addAttribute("errorMessage", message);
        return "fragments/auth/register-status";
    }
}
