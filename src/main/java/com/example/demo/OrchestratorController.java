package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrchestratorController {

    private final ChatClient chatClient;
    private final StandardRagController standardRagController;
    private final GraphRagController graphRagController;

    public OrchestratorController(ChatClient.Builder chatClientBuilder,
                                  StandardRagController standardRagController,
                                  GraphRagController graphRagController) {
        this.chatClient = chatClientBuilder.build();
        this.standardRagController = standardRagController;
        this.graphRagController = graphRagController;
    }

    @GetMapping("/api/orchestrator")
    public String orchestrate(@RequestParam String query) {
        String systemPrompt = """
            You are an intelligent router for an IT helpdesk.
            Analyze the user's query and decide if it should be routed to a standard Document Database (RAG)
            or a Knowledge Graph Database (GRAPHRAG).
            
            Rules:
            1. If the query asks for an exact error message, stack trace, or simple documentation lookup, respond strictly with 'RAG'.
            2. If the query asks about downstream impact or missing connections, respond strictly with 'GRAPHRAG_DOWNSTREAM'.
            3. If the query asks about upstream impact, reverse dependencies, or cascading failures up the chain, respond strictly with 'GRAPHRAG_UPSTREAM'.
            
            Return exactly ONE word: 'RAG', 'GRAPHRAG_DOWNSTREAM', or 'GRAPHRAG_UPSTREAM'. No other text.
            """;

        String classification = chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .call()
                .content()
                .trim()
                .toUpperCase();

        if ("GRAPHRAG_UPSTREAM".equals(classification)) {
            // Route to GraphRAG Upstream
            return "[Routed to: GRAPHRAG Upstream] \n\n" + graphRagController.executeGraphQueryForImpact(query, true);
        } else if ("GRAPHRAG_DOWNSTREAM".equals(classification)) {
            // Route to GraphRAG Downstream
            return "[Routed to: GRAPHRAG Downstream] \n\n" + graphRagController.executeGraphQueryForImpact(query, false);
        } else {
            // Route to Standard RAG
            return "[Routed to: RAG] \n\n" + standardRagController.executeRagQuery(query, 3);
        }
    }
}
