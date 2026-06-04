package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SimpleAiControllerTest {

    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private SimpleAiController controller;

    @BeforeEach
    void setUp() {
        chatClientBuilder = Mockito.mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
        chatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        
        when(chatClientBuilder.build()).thenReturn(chatClient);
        
        when(chatClient.prompt().user(any(java.util.function.Consumer.class)).call().content())
                .thenReturn("Mocked AI Response");

        controller = new SimpleAiController(chatClientBuilder);
    }

    @Test
    void testSimpleAi() {
        String response = controller.simpleAi("Why is payment failing?");
        assertEquals("Mocked AI Response", response);
    }
}
