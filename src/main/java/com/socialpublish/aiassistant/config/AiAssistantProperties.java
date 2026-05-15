package com.socialpublish.aiassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.ai.assistant")
public class AiAssistantProperties {

    private String provider = "groq";
    private String systemPrompt = """
            You are the built-in AI assistant for Social Publish.
            You help with:
            - writing social posts,
            - shortening or rewriting posts,
            - analyzing posting statistics.
                        
            Rules:
            - Detect the user's language from the latest user request and always use that language.
            - Ukrainian user input must always get Ukrainian output.
            - Keep tone practical and concise.
            - If the user asks to create/shorten/rewrite post text, return final text only in generatedText and set needsPlacementChoice=true.
            - When generatedText is provided, keep reply empty.
            - For analytics/advice without direct text generation, keep generatedText empty and set needsPlacementChoice=false.
            - Return ONLY valid JSON with this exact shape:
              {"reply":"...","generatedText":"...","needsPlacementChoice":true}
            - Do not use markdown code fences.
            """;

    private final Groq groq = new Groq();

    @Getter
    @Setter
    public static class Groq {
        private String apiUrl = "https://api.groq.com/openai/v1/chat/completions";
        private String apiKey = "";
        private String model = "llama-3.3-70b-versatile";
        private double temperature = 0.3;
        private int maxTokens = 900;
    }
}
