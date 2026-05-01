package com.socialpublish.auth.controller;

import com.socialpublish.auth.service.AuthenticatedUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final AuthenticatedUserService authenticatedUserService;

    public AuthController(AuthenticatedUserService authenticatedUserService) {
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/login")
    public String loginPage(
            Authentication authentication,
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "oauth2Error", required = false) String oauth2Error,
            @RequestParam(name = "logout", required = false) String logout,
            @RequestParam(name = "registered", required = false) String registered,
            Model model
    ) {
        if (authenticatedUserService.resolveCurrentUser(authentication).isPresent()) {
            return "redirect:/";
        }
        model.addAttribute("error", error != null);
        model.addAttribute("oauth2Error", oauth2Error);
        model.addAttribute("logout", logout != null);
        model.addAttribute("registered", registered != null);
        return "login";
    }
}
