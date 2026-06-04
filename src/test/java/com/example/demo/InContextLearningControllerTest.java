package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class InContextLearningControllerTest {

    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private InContextLearningController controller;

    @BeforeEach
    void setUp() {
        chatClientBuilder = Mockito.mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
        chatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        
        when(chatClientBuilder.build()).thenReturn(chatClient);
        
        when(chatClient.prompt().user(any(java.util.function.Consumer.class)).call().content())
                .thenReturn("Mocked AI Response");

        controller = new InContextLearningController(chatClientBuilder);
    }

    @Test
    void testInContextLearning() {
        InContextLearningController.InContextRequest request = 
            new InContextLearningController.InContextRequest("Why is payment failing?", "Ticket-101: System is down.");
        String response = controller.inContextLearning(request);
        assertEquals("Mocked AI Response", response);
    }
}
