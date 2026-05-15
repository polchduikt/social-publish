package com.socialpublish.aiassistant.controller;

import com.socialpublish.aiassistant.dto.AiAssistantChatRequest;
import com.socialpublish.aiassistant.dto.AiAssistantChatResponse;
import com.socialpublish.aiassistant.provider.AiProviderException;
import com.socialpublish.aiassistant.service.AiAssistantService;
import com.socialpublish.auth.dto.CurrentUserView;
import com.socialpublish.common.web.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai-assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @PostMapping("/chat")
    public AiAssistantChatResponse chat(
            @CurrentUser CurrentUserView currentUser,
            @Valid @RequestBody AiAssistantChatRequest request
    ) {
        return aiAssistantService.chat(currentUser.id(), request);
    }

    @ExceptionHandler(AiProviderException.class)
    public ResponseEntity<Map<String, String>> handleProviderError(AiProviderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", ex.getMessage()));
    }
}

