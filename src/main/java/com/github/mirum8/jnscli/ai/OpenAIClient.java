package com.github.mirum8.jnscli.ai;

import io.github.stefanbratanov.jvm.openai.*;

public class OpenAIClient implements AiClient {
    private final ChatClient chatClient;
    private final String model;

    public OpenAIClient(LlmSettings.OpenAI settings) {
        OpenAI openAI = OpenAI.newBuilder(settings.apiKey()).build();
        this.chatClient = openAI.chatClient();
        this.model = settings.model();
    }

    @Override
    public String generate(String prompt) {
        CreateChatCompletionRequest createChatCompletionRequest = CreateChatCompletionRequest.newBuilder()
            .model(model)
            .message(ChatMessage.userMessage(prompt))
            .build();
        ChatCompletion chatCompletion = chatClient.createChatCompletion(createChatCompletionRequest);
        return chatCompletion.choices().getFirst().message().content();
    }
}
