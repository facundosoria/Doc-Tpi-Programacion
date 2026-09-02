package com.example.demo.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GroqChatClientConfig {

    @Value("${spring.ai.openai.chat.model}")
    private String model;

    @Bean
    public ChatClient groqChatClient(ChatClient.Builder builder) {
        return builder
                .build();
    }
}
