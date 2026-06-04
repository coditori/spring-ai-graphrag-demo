package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class StandardRagController {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;

    public StandardRagController(ChatClient.Builder chatClientBuilder, VectorStore vectorStore, EmbeddingModel embeddingModel) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel;
    }

    // STAGE 3: Standard RAG (Success)
    // Working example: A common RAG question
    // This perfectly matches unstructured text (exact error messages) across isolated documents.
    @GetMapping("/api/rag/error-details")
    public String getErrorDetails(@RequestParam(defaultValue = "What is the exact error message reported when the frontend load balancer fails?") String query) {
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(2).build()
        );
        
        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        String prompt = """
            You are an IT helpdesk assistant. Use the following context to answer the user's query.
            If you do not know the answer based on the context, say so.
            
            Context:
            {context}
            
            User Query:
            {query}
            """;

        return chatClient.prompt()
                .user(u -> u.text(prompt)
                        .param("query", query)
                        .param("context", context))
                .call()
                .content();
    }

    // STAGE 3: Standard RAG (Failure)
    // Not working example: A situation which is the expertise of GraphRAG
    // This fails because it requires multi-hop traversal to find implicit downstream dependencies.
    @GetMapping("/api/rag/downstream-impact")
    public String getDownstreamImpact(@RequestParam(defaultValue = "If the Payment Gateway is timing out, which downstream team do I need to contact?") String query) {
        return executeRagQuery(query, 3);
    }

    // -------------------------------------------------------------------------
    // NEW ADVANCED EXAMPLES
    // -------------------------------------------------------------------------

    // 1. Reverse Dependency (Failure)
    @GetMapping("/api/rag/upstream-impact")
    public String getUpstreamImpact(@RequestParam(defaultValue = "If we take the User Database offline for maintenance this weekend, which customer-facing services will break?") String query) {
        return executeRagQuery(query, 3);
    }

    // 2. Cross-Silo Aggregation (Failure)
    @GetMapping("/api/rag/team-bottleneck")
    public String getTeamBottleneck(@RequestParam(defaultValue = "Which engineering team is currently responsible for the highest number of failing services?") String query) {
        return executeRagQuery(query, 3);
    }

    // 3. The Obscure Stack Trace (Success)
    // Standard RAG is an absolute beast at finding semantically similar stack traces hidden in unstructured text.
    @GetMapping("/api/rag/obscure-trace")
    public String getObscureTrace(@RequestParam(defaultValue = "Have we ever seen a NullPointerException occurring exactly on line 428 in the old payment monolith?") String query) {
        return executeRagQuery(query, 2);
    }

    public String executeRagQuery(String query, int topK) {
        List<Document> similarDocuments = vectorStore.similaritySearch(
                SearchRequest.builder().query(query).topK(topK).build()
        );
        
        String context = similarDocuments.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        String prompt = """
            You are an IT helpdesk assistant. Use the following context to answer the user's query.
            If you do not know the answer based on the context, say so.
            
            Context:
            {context}
            
            User Query:
            {query}
            """;

        return chatClient.prompt()
                .user(u -> u.text(prompt)
                        .param("query", query)
                        .param("context", context))
                .call()
                .content();
    }
}
