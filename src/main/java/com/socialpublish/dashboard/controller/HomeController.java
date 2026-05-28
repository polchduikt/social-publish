package com.socialpublish.dashboard.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.auth.service.AuthenticatedUserService;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/")
    public String landing(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            authenticatedUserService.resolveCurrentUser(authentication).ifPresent(user -> {
                model.addAttribute("user", user);
            });
        }
        return "pages/public/landing";
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(name = "message", required = false) String message,
            @RequestParam(name = "error", required = false) String error,
            Model model
    ) {
        DashboardView dashboard = dashboardService.buildDashboard(currentUser.id());
        model.addAttribute("user", currentUser);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        return "pages/dashboard/home";
    }
}
