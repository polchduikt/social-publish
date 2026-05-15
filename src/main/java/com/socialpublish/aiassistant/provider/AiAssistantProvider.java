package com.socialpublish.aiassistant.provider;

public interface AiAssistantProvider {

    String name();

    String complete(String systemPrompt, String userPrompt);
}

