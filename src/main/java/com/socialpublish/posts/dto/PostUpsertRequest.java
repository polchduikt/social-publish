package com.socialpublish.posts.dto;

import com.socialpublish.common.validation.PlatformList;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PostUpsertRequest {

    @Size(max = 5000, message = "Post content must be at most 5000 characters")
    private String content;

    @NotNull(message = "Status is required")
    @Pattern(
            regexp = "DRAFT|SCHEDULED|PUBLISHING|PUBLISHED|RETRYING|FAILED|CANCELLED",
            message = "Status value is invalid"
    )
    private String status = "DRAFT";

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledAt;

    @Size(max = 500, message = "Failure reason must be at most 500 characters")
    private String failedReason;

    @PlatformList
    private List<String> platforms = new ArrayList<>();

    private boolean recurring;

    private List<String> recurringDays = new ArrayList<>();

    private String recurringTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime recurringEndDate;

    private boolean silentMode;

    private String inlineButtons;

    @Size(max = 250, message = "Poll question must be at most 250 characters")
    private String pollQuestion;

    private String pollOptions;

    private boolean pollMultipleAnswers;

    private boolean pollIsQuiz;

    private Integer pollCorrectOptionId;
}
