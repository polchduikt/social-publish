package com.socialpublish.posts.dto;

import com.socialpublish.posts.entity.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostUpsertRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 150, message = "Title must be at most 150 characters")
    private String title;

    @NotBlank(message = "Post content is required")
    @Size(max = 5000, message = "Post content must be at most 5000 characters")
    private String content;

    @NotNull(message = "Status is required")
    private PostStatus status = PostStatus.DRAFT;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledAt;

    @Size(max = 500, message = "Failure reason must be at most 500 characters")
    private String failedReason;
}
