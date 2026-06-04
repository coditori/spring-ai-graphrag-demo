package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class StandardRagControllerTest {

    private VectorStore vectorStore;
    private EmbeddingModel embeddingModel;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private StandardRagController controller;

    @BeforeEach
    void setUp() {
        vectorStore = Mockito.mock(VectorStore.class);
        embeddingModel = Mockito.mock(EmbeddingModel.class);
        chatClientBuilder = Mockito.mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
        chatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        
        when(chatClientBuilder.build()).thenReturn(chatClient);
        
        when(chatClient.prompt().user(any(java.util.function.Consumer.class)).call().content())
                .thenReturn("Mocked AI Response");

        controller = new StandardRagController(chatClientBuilder, vectorStore, embeddingModel);
    }

    @Test
    void testGetErrorDetails() {
        Document mockDoc = new Document("Ticket-101: System is down.");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(mockDoc));

        String response = controller.getErrorDetails("What is failing?");
        assertEquals("Mocked AI Response", response);
    }
}
