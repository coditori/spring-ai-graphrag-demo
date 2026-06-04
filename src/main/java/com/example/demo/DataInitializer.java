package com.example.demo;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final Neo4jClient neo4jClient;
    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(VectorStore vectorStore, Neo4jClient neo4jClient, EmbeddingModel embeddingModel, JdbcTemplate jdbcTemplate) {
        this.vectorStore = vectorStore;
        this.neo4jClient = neo4jClient;
        this.embeddingModel = embeddingModel;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Initializing Database with Demo Data...");

        // 1. Clean Postgres pgvector documents
        try {
            jdbcTemplate.execute("TRUNCATE TABLE vector_store");
        } catch (Exception e) {
            System.out.println("Vector store table might not exist yet, skipping TRUNCATE: " + e.getMessage());
        }

        // 2. Clean Neo4j
        neo4jClient.query("MATCH (n) DETACH DELETE n").run();
        
        // Create Neo4j Vector Index for GraphRAG
        neo4jClient.query("CREATE VECTOR INDEX ticket_embeddings IF NOT EXISTS " +
                "FOR (n:Ticket) ON (n.embedding) " +
                "OPTIONS {indexConfig: {`vector.dimensions`: 768, `vector.similarity_function`: 'cosine'}}").run();


        // 3. Insert Documents to standard VectorStore (Postgres pgvector)
        String ticket1Text = "Ticket-101: Payment Gateway is timing out for user checkout. Returning 504 Gateway Timeout.";
        String ticket2Text = "Ticket-102: User Database cluster reporting high latency and frequent connection drops.";
        String ticket3Text = "Ticket-103: Frontend load balancer returning 502 Bad Gateway intermittently.";
        String ticket999Text = "Ticket-999: NullPointerException occurring exactly on line 428 in PaymentEngine.java. Handled and closed 3 years ago.";

        List<Document> documents = List.of(
            new Document(ticket1Text, Map.of("ticketId", "101")),
            new Document(ticket2Text, Map.of("ticketId", "102")),
            new Document(ticket3Text, Map.of("ticketId", "103")),
            new Document(ticket999Text, Map.of("ticketId", "999"))
        );
        vectorStore.add(documents);

        // 4. Compute embeddings for Neo4j Graph queries manually
        float[] emb1 = embeddingModel.embed(ticket1Text);
        float[] emb2 = embeddingModel.embed(ticket2Text);
        float[] emb3 = embeddingModel.embed(ticket3Text);
        float[] emb999 = embeddingModel.embed(ticket999Text);

        // 5. Build Graph in Neo4j (Teams, Services, Tickets)
        String cypher = """
            CREATE (t1:Team {name: 'Checkout Team'})
            CREATE (t2:Team {name: 'Backend Platform Team'})
            CREATE (t3:Team {name: 'Frontend Team'})

            CREATE (s1:Service {name: 'Payment Gateway'})-[:OWNED_BY]->(t1)
            CREATE (s2:Service {name: 'User Database'})-[:OWNED_BY]->(t2)
            CREATE (s3:Service {name: 'Web UI'})-[:OWNED_BY]->(t3)

            CREATE (s3)-[:DEPENDS_ON]->(s1)
            CREATE (s1)-[:DEPENDS_ON]->(s2)

            CREATE (tk1:Ticket {id: '101', description: $desc1, embedding: $emb1})-[:AFFECTS]->(s1)
            CREATE (tk2:Ticket {id: '102', description: $desc2, embedding: $emb2})-[:AFFECTS]->(s2)
            CREATE (tk3:Ticket {id: '103', description: $desc3, embedding: $emb3})-[:AFFECTS]->(s3)
            CREATE (tk999:Ticket {id: '999', description: $desc999, embedding: $emb999})-[:AFFECTS]->(s1)
        """;

        neo4jClient.query(cypher)
                .bind(ticket1Text).to("desc1")
                .bind(emb1).to("emb1")
                .bind(ticket2Text).to("desc2")
                .bind(emb2).to("emb2")
                .bind(ticket3Text).to("desc3")
                .bind(emb3).to("emb3")
                .bind(ticket999Text).to("desc999")
                .bind(emb999).to("emb999")
                .run();

        System.out.println("Data Initialization Complete!");
    }
}
