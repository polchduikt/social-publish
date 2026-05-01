package com.socialpublish.auth.controller;

import com.socialpublish.auth.service.AuthenticatedUserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final AuthenticatedUserService authenticatedUserService;

    public HomeController(AuthenticatedUserService authenticatedUserService) {
        this.authenticatedUserService = authenticatedUserService;
    }

    @GetMapping("/")
    public String root(Authentication authentication, Model model) {
        authenticatedUserService.resolveForView(authentication)
                .ifPresent(user -> model.addAttribute("user", user));
        return "home";
    }
}
