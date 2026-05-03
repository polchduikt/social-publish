package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.posts.dto.PostView;
import com.socialpublish.posts.entity.PostStatus;
import com.socialpublish.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class QueueController {

    private final PostService postService;

    @GetMapping("/queue")
    public String queuePage(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam(name = "status", required = false) PostStatus statusFilter,
            Model model
    ) {
        List<PostView> posts = postService.getQueuePosts(currentUser.id(), statusFilter);

        model.addAttribute("user", currentUser);
        model.addAttribute("posts", posts);
        model.addAttribute("statuses", PostStatus.values());
        model.addAttribute("activeFilter", statusFilter);
        return "pages/dashboard/queue";
    }
}
