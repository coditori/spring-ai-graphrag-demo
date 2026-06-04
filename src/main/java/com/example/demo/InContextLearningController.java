package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InContextLearningController {

    private final ChatClient chatClient;

    public InContextLearningController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public record InContextRequest(String query, String context) {}

    // STAGE 2: In-Context Learning (Prompt Stuffing). 
    // The user manually provides the context. There is no automated retrieval from a database.
    @PostMapping("/api/in-context")
    public String inContextLearning(@RequestBody InContextRequest request) {
        String prompt = """
            You are an IT helpdesk assistant. Use the following manually provided context to answer the user's query.
            If you do not know the answer based on the context, say so.
            
            Context:
            {context}
            
            User Query:
            {query}
            """;

        return chatClient.prompt()
                .user(u -> u.text(prompt)
                        .param("query", request.query())
                        .param("context", request.context()))
                .call()
                .content();
    }
}
