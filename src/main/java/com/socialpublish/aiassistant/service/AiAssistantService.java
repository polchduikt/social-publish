package com.socialpublish.aiassistant.service;

import com.socialpublish.aiassistant.config.AiAssistantProperties;
import com.socialpublish.aiassistant.dto.AiAssistantChatRequest;
import com.socialpublish.aiassistant.dto.AiAssistantChatResponse;
import com.socialpublish.aiassistant.provider.AiAssistantProvider;
import com.socialpublish.aiassistant.provider.AiProviderException;
import com.socialpublish.dashboard.dto.DashboardView;
import com.socialpublish.dashboard.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AiAssistantService {

    private final DashboardService dashboardService;
    private final Map<String, AiAssistantProvider> providers;
    private final AiAssistantProperties properties;
    private final PromptBuilder promptBuilder;
    private final AiResponseParser responseParser;

    public AiAssistantService(
            DashboardService dashboardService,
            List<AiAssistantProvider> providers,
            AiAssistantProperties properties,
            PromptBuilder promptBuilder,
            AiResponseParser responseParser
    ) {
        this.dashboardService = dashboardService;
        this.providers = providers.stream().collect(Collectors.toMap(p -> p.name().toLowerCase(), p -> p));
        this.properties = properties;
        this.promptBuilder = promptBuilder;
        this.responseParser = responseParser;
    }

    public AiAssistantChatResponse chat(UUID ownerId, AiAssistantChatRequest request) {
        DashboardView dashboard = dashboardService.buildDashboard(ownerId);
        String userPrompt = promptBuilder.buildUserPrompt(request, dashboard);
        String systemPrompt = properties.getSystemPrompt();
        
        String providerName = properties.getProvider().toLowerCase();
        AiAssistantProvider provider = providers.get(providerName);

        if (provider == null) {
            log.error("Failed to execute AI chat: unsupported provider '{}'", providerName);
            throw new AiProviderException("AI provider is not supported: " + providerName);
        }

        log.info("Sending prompt to AI assistant provider: {}", providerName);
        log.debug("System prompt: {}\nUser prompt: {}", systemPrompt, userPrompt);

        String rawResponse = provider.complete(systemPrompt, userPrompt);
        log.debug("AI assistant provider raw response: {}", rawResponse);

        AiResponseParser.ParsedResponse parsed = responseParser.parseResponse(rawResponse);

        String generatedText = parsed.generatedText() == null ? "" : parsed.generatedText().trim();
        String reply = resolveReply(parsed, generatedText);
        boolean needsPlacementChoice = parsed.needsPlacementChoice() || !generatedText.isBlank();

        log.info("AI chat response successfully processed. reply length: {}, text generated: {}", 
                reply.length(), !generatedText.isBlank());

        return new AiAssistantChatResponse(reply, generatedText, needsPlacementChoice);
    }

    private String resolveReply(AiResponseParser.ParsedResponse parsed, String generatedText) {
        if (parsed.reply() == null || parsed.reply().isBlank()) {
            return generatedText.isBlank()
                    ? "Could not build a clear response. Please rephrase your request."
                    : "";
        }
        return parsed.reply().trim();
    }
}
