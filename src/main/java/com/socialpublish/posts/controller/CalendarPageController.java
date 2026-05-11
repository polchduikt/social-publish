package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CalendarPageController {

    @GetMapping("/calendar")
    public String calendarPage(@CurrentUser CurrentUserView currentUser, Model model) {
        model.addAttribute("user", currentUser);
        return "pages/dashboard/calendar";
    }
}
