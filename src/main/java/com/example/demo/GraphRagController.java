package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class GraphRagController {

    private final ChatClient chatClient;
    private final Neo4jClient neo4jClient;
    private final EmbeddingModel embeddingModel;

    public GraphRagController(ChatClient.Builder chatClientBuilder, Neo4jClient neo4jClient, EmbeddingModel embeddingModel) {
        this.chatClient = chatClientBuilder.build();
        this.neo4jClient = neo4jClient;
        this.embeddingModel = embeddingModel;
    }

    // STAGE 4: GraphRAG (Success)
    // Working example: A common GraphRAG question
    // This perfectly traverses the graph to find implicit dependencies and ownership teams.
    @GetMapping("/api/graphrag/downstream-impact")
    public String getDownstreamImpact(@RequestParam(defaultValue = "If the Payment Gateway is timing out, which downstream team do I need to contact?") String query) {
        float[] queryEmbedding = embeddingModel.embed(query);

        // Vector search to find the ticket, then traverse to find downstream dependencies and teams
        String cypher = """
            CALL db.index.vector.queryNodes('ticket_embeddings', 1, $embedding) YIELD node AS t, score
            MATCH (t)-[:AFFECTS]->(root:Service)
            MATCH (root)-[:DEPENDS_ON*1..3]->(downstream:Service)
            MATCH (downstream)-[:OWNED_BY]->(team:Team)
            RETURN t.description AS RawText, root.name AS FailingService, downstream.name AS DownstreamService, team.name AS OwningTeam, score
            ORDER BY score DESC
            """;

        Collection<Map<String, Object>> results = neo4jClient.query(cypher)
                .bind(queryEmbedding).to("embedding")
                .fetch().all();

        String context = results.stream()
                .map(row -> String.format("Raw Error Text: %s\nFailing Service: %s\nDownstream Service: %s\nOwning Team: %s",
                        row.get("RawText"), row.get("FailingService"), row.get("DownstreamService"), row.get("OwningTeam")))
                .collect(Collectors.joining("\n\n"));

        String prompt = """
            You are an IT architect. Use the following graph traversal context to answer the user's query.
            List the root failing service, the downstream service, and the team that needs to be contacted.
            If you do not know the answer based on the context, say so.
            
            rules: you should only answer to the user's query based on the context provided. Do not attempt to answer based on any outside knowledge. If the context does not contain the answer, say "I don't know based on the provided context."
            
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

    // STAGE 4: GraphRAG (Failure)
    // Not working example: A situation which is the expertise of RAG
    // This fails because the highly structured graph traversal strips out the unstructured raw text of the tickets.
    @GetMapping("/api/graphrag/error-details")
    public String getErrorDetails(@RequestParam(defaultValue = "What is the exact error message reported when the frontend load balancer fails?") String query) {
        return executeGraphQueryForImpact(query, false);
    }

    // -------------------------------------------------------------------------
    // NEW ADVANCED EXAMPLES
    // -------------------------------------------------------------------------

    // 1. Reverse Dependency (Success)
    @GetMapping("/api/graphrag/upstream-impact")
    public String getUpstreamImpact(@RequestParam(defaultValue = "If we take the User Database offline for maintenance this weekend, which customer-facing services will break?") String query) {
        return executeGraphQueryForImpact(query, true); // true = upstream
    }

    // 2. Cross-Silo Aggregation (Success)
    // Graph databases excel at aggregating structure across the entire dataset.
    @GetMapping("/api/graphrag/team-bottleneck")
    public String getTeamBottleneck(@RequestParam(defaultValue = "Which engineering team is currently responsible for the highest number of failing services?") String query) {
        String cypher = """
            MATCH (t:Ticket)-[:AFFECTS]->(s:Service)-[:OWNED_BY]->(team:Team)
            RETURN team.name AS OwningTeam, count(t) as incidentCount
            ORDER BY incidentCount DESC LIMIT 1
            """;

        Collection<Map<String, Object>> results = neo4jClient.query(cypher).fetch().all();

        String context = results.stream()
                .map(row -> String.format("Team: %s, Failing Services Count: %s",
                        row.get("OwningTeam"), row.get("incidentCount")))
                .collect(Collectors.joining("\n"));

        String prompt = """
            You are an IT architect. Use the following graph aggregation data to answer the user's query.
            
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

    // 3. The Obscure Stack Trace (Failure)
    // GraphRAG fails here because the unstructured stack trace is buried in the ticket, and the graph
    // query is structured to pull out Services and Teams, effectively ignoring the raw text.
    @GetMapping("/api/graphrag/obscure-trace")
    public String getObscureTrace(@RequestParam(defaultValue = "Have we ever seen a NullPointerException occurring exactly on line 428 in the old payment monolith?") String query) {
        return executeGraphQueryForImpact(query, false); // Just doing a standard structural traversal which ignores the stack trace
    }

    public String executeGraphQueryForImpact(String query, boolean upstream) {
        float[] queryEmbedding = embeddingModel.embed(query);

        String relationshipDir = upstream ? "<-[:DEPENDS_ON*1..3]-" : "-[:DEPENDS_ON*1..3]->";
        
        String cypher = String.format("""
            CALL db.index.vector.queryNodes('ticket_embeddings', 1, $embedding) YIELD node AS t, score
            MATCH (t)-[:AFFECTS]->(root:Service)
            MATCH (root)%s(impacted:Service)
            MATCH (impacted)-[:OWNED_BY]->(team:Team)
            RETURN t.description AS RawText, root.name AS RootService, impacted.name AS ImpactedService, team.name AS OwningTeam, score
            ORDER BY score DESC
            """, relationshipDir);

        Collection<Map<String, Object>> results = neo4jClient.query(cypher)
                .bind(queryEmbedding).to("embedding")
                .fetch().all();

        String context = results.stream()
                .map(row -> String.format("Raw Error Text: %s\nRoot Service: %s\nImpacted Service: %s\nOwning Team: %s",
                        row.get("RawText"), row.get("RootService"), row.get("ImpactedService"), row.get("OwningTeam")))
                .collect(Collectors.joining("\n\n"));

        String prompt = """
            You are an IT architect. Use the following graph traversal context to answer the user's query.
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
