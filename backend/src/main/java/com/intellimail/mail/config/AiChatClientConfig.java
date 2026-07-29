package com.intellimail.mail.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The {@link ChatModel} bean (AzureOpenAiChatModel) is auto-configured by
 * spring-ai-starter-model-azure-openai directly from the
 * spring.ai.azure.openai.* properties in application.yml. This class only
 * wraps it in the higher-level fluent {@link ChatClient} the rest of the
 * app (see {@code client.AzureOpenAiClient}) is built against.
 */
@Configuration
public class AiChatClientConfig {

    // Named explicitly (rather than relying on the method-name-as-bean-name
    // default) since agent.config.AgentChatClientConfig introduces a second
    // ChatClient bean - every injection site now disambiguates via
    // @Qualifier("chatClient") to avoid Spring failing to autowire with two
    // ChatClient candidates present.
    @Bean("chatClient")
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}
