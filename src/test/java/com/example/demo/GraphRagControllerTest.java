package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.neo4j.core.Neo4jClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class GraphRagControllerTest {

    private Neo4jClient neo4jClient;
    private EmbeddingModel embeddingModel;
    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private GraphRagController controller;

    @BeforeEach
    void setUp() {
        neo4jClient = Mockito.mock(Neo4jClient.class, Answers.RETURNS_DEEP_STUBS);
        embeddingModel = Mockito.mock(EmbeddingModel.class);
        chatClientBuilder = Mockito.mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
        chatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        
        when(chatClientBuilder.build()).thenReturn(chatClient);
        
        when(chatClient.prompt().user(any(java.util.function.Consumer.class)).call().content())
                .thenReturn("Mocked AI Response");

        controller = new GraphRagController(chatClientBuilder, neo4jClient, embeddingModel);
    }

    @Test
    void testGetDownstreamImpact() {
        float[] mockEmbedding = {0.1f, 0.2f, 0.3f};
        when(embeddingModel.embed(anyString())).thenReturn(mockEmbedding);

        List<Map<String, Object>> mockGraphResults = List.of(
                Map.of(
                        "RawText", "Ticket-101: Payment Gateway is timing out",
                        "FailingService", "User Database",
                        "DownstreamService", "Payment Gateway",
                        "OwningTeam", "Checkout Team"
                )
        );

        when(neo4jClient.query(anyString()).bind(any(float[].class)).to("embedding").fetch().all())
                .thenReturn(mockGraphResults);

        String response = controller.getDownstreamImpact("Why is payment failing?");
        assertEquals("Mocked AI Response", response);
    }
}
