package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleAiController {

    private final ChatClient chatClient;

    public SimpleAiController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // STAGE 1: Baseline AI (No RAG). Queries Gemini directly. 
    // It will miss internal company context and likely hallucinate or fail.
    @GetMapping("/api/simple")
    public String simpleAi(@RequestParam(defaultValue = "Why is the Payment Processing system failing, and who should I contact?") String query) {
        // Generate answer using Gemini without any extra context
        String prompt = """
            You are an IT helpdesk assistant. Answer the user's query directly.
            If you do not know the answer, say so.
            
            User Query:
            {query}
            """;

        return chatClient.prompt()
                .user(u -> u.text(prompt)
                        .param("query", query))
                .call()
                .content();
    }
}
