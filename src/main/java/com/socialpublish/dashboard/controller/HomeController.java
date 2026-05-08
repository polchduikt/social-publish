package com.socialpublish.dashboard.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final DashboardService dashboardService;

    @GetMapping("/")
    public String landing(@CurrentUser CurrentUserView currentUser, Model model) {
        if (currentUser != null) {
            model.addAttribute("user", currentUser);
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
