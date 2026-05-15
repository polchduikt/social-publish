package com.socialpublish.aiassistant.provider;

import com.socialpublish.aiassistant.config.AiAssistantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GroqAiAssistantProvider implements AiAssistantProvider {

    private final RestClient restClient;
    private final AiAssistantProperties properties;

    @Override
    public String name() {
        return "groq";
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
        String apiKey = properties.getGroq().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("AI provider key is not configured");
        }

        GroqChatResponse response;
        try {
            response = restClient.post()
                    .uri(properties.getGroq().getApiUrl())
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(new GroqChatRequest(
                            properties.getGroq().getModel(),
                            List.of(
                                    new GroqMessage("system", systemPrompt),
                                    new GroqMessage("user", userPrompt)
                            ),
                            properties.getGroq().getTemperature(),
                            properties.getGroq().getMaxTokens()
                    ))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new AiProviderException("AI provider request failed with status " + res.getStatusCode().value());
                    })
                    .body(GroqChatResponse.class);
        } catch (RestClientResponseException ex) {
            throw new AiProviderException("AI provider request failed: " + ex.getStatusCode().value(), ex);
        } catch (AiProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiProviderException("AI provider request failed", ex);
        }

        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new AiProviderException("AI provider returned empty response");
        }

        GroqMessage message = response.choices().getFirst().message();
        if (message == null || message.content() == null || message.content().isBlank()) {
            throw new AiProviderException("AI provider returned empty content");
        }

        return message.content();
    }

    private record GroqChatRequest(
            String model,
            List<GroqMessage> messages,
            double temperature,
            int max_tokens
    ) {
    }

    private record GroqChatResponse(List<GroqChoice> choices) {
    }

    private record GroqChoice(GroqMessage message) {
    }

    private record GroqMessage(String role, String content) {
    }
}

