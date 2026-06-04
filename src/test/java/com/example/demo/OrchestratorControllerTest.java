package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

public class OrchestratorControllerTest {

    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private StandardRagController standardRagController;
    private GraphRagController graphRagController;
    private OrchestratorController controller;

    @BeforeEach
    void setUp() {
        chatClientBuilder = Mockito.mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
        chatClient = Mockito.mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
        standardRagController = Mockito.mock(StandardRagController.class);
        graphRagController = Mockito.mock(GraphRagController.class);
        
        when(chatClientBuilder.build()).thenReturn(chatClient);
        
        controller = new OrchestratorController(chatClientBuilder, standardRagController, graphRagController);
    }

    @Test
    void testRouteToRag() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("RAG");
                
        when(standardRagController.executeRagQuery(anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn("Mocked RAG Response");

        String response = controller.orchestrate("What is the exact error message?");
        assertEquals("[Routed to: RAG] \n\nMocked RAG Response", response);
    }

    @Test
    void testRouteToGraphRagDownstream() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("GRAPHRAG_DOWNSTREAM");
                
        when(graphRagController.executeGraphQueryForImpact(anyString(), anyBoolean()))
                .thenReturn("Mocked GraphRAG Downstream Response");

        String response = controller.orchestrate("Which downstream team is impacted?");
        assertEquals("[Routed to: GRAPHRAG Downstream] \n\nMocked GraphRAG Downstream Response", response);
    }

    @Test
    void testRouteToGraphRagUpstream() {
        when(chatClient.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("GRAPHRAG_UPSTREAM");
                
        when(graphRagController.executeGraphQueryForImpact(anyString(), anyBoolean()))
                .thenReturn("Mocked GraphRAG Upstream Response");

        String response = controller.orchestrate("What is the upstream impact?");
        assertEquals("[Routed to: GRAPHRAG Upstream] \n\nMocked GraphRAG Upstream Response", response);
    }
}
