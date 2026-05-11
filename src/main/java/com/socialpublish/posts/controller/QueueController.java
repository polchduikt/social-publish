package com.socialpublish.posts.controller;

import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import com.socialpublish.posts.queue.QueueBulkAction;
import com.socialpublish.posts.queue.QueueBulkResult;
import com.socialpublish.posts.queue.QueueDateRangeFilter;
import com.socialpublish.posts.queue.QueueFilterRequest;
import com.socialpublish.posts.queue.QueuePageView;
import com.socialpublish.posts.queue.QueuePlatformFilter;
import com.socialpublish.posts.queue.QueuePostTypeFilter;
import com.socialpublish.posts.queue.QueueService;
import com.socialpublish.posts.queue.QueueSortOption;
import com.socialpublish.posts.queue.QueueWebSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;
    private final QueueWebSupport queueWebSupport;

    @ModelAttribute("statusOptions")
    public List<String> statusOptions() {
        return List.of("DRAFT", "SCHEDULED", "PUBLISHING", "PUBLISHED", "RETRYING", "FAILED", "CANCELLED");
    }

    @ModelAttribute("platformOptions")
    public QueuePlatformFilter[] platformOptions() {
        return QueuePlatformFilter.values();
    }

    @ModelAttribute("typeOptions")
    public QueuePostTypeFilter[] typeOptions() {
        return QueuePostTypeFilter.values();
    }

    @ModelAttribute("dateRangeOptions")
    public QueueDateRangeFilter[] dateRangeOptions() {
        return QueueDateRangeFilter.values();
    }

    @ModelAttribute("sortOptions")
    public QueueSortOption[] sortOptions() {
        return QueueSortOption.values();
    }

    @ModelAttribute("bulkActions")
    public QueueBulkAction[] bulkActions() {
        return QueueBulkAction.values();
    }

    @GetMapping("/queue")
    public String queuePage(
            @CurrentUser CurrentUserView currentUser,
            @ModelAttribute("filters") QueueFilterRequest filters,
            @RequestParam(name = "message", required = false) String message,
            @RequestParam(name = "error", required = false) String error,
            Model model
    ) {
        QueuePageView queue = queueService.getQueue(currentUser.id(), filters);

        model.addAttribute("user", currentUser);
        model.addAttribute("queue", queue);
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        return "pages/dashboard/queue";
    }

    @PostMapping("/queue/bulk")
    public String runBulkAction(
            @CurrentUser CurrentUserView currentUser,
            @RequestParam("action") QueueBulkAction action,
            @RequestParam(name = "postIds", required = false) List<UUID> postIds,
            @RequestParam(name = "scheduledAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime scheduledAt,
            @ModelAttribute("filters") QueueFilterRequest filters,
            RedirectAttributes redirectAttributes
    ) {
        if (postIds == null || postIds.isEmpty()) {
            queueWebSupport.applySelectionError(redirectAttributes);
            return "redirect:" + queueWebSupport.buildRedirectUrl(filters);
        }

        QueueBulkResult result = queueService.runBulkAction(
                currentUser.id(),
                action,
                postIds,
                scheduledAt
        );

        queueWebSupport.applyBulkResultFeedback(result, redirectAttributes);

        return "redirect:" + queueWebSupport.buildRedirectUrl(filters);
    }
}
