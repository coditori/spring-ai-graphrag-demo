---
name: data-generation
description: Instructions for generating mock IT Helpdesk data to demonstrate the difference between Standard RAG and GraphRAG.
---

# Data Generation Guidelines

When tasked with generating or updating the mock IT Helpdesk data for the presentation, follow these instructions to ensure both databases are correctly populated.

## Core Philosophy: The Single Source of Truth

We use **a single initialization logic** for both PostgreSQL (pgvector) and Neo4j. This is crucial for the presentation. 
If Standard RAG fails and GraphRAG succeeds, the audience must know it is due to the *technology capabilities* (text retrieval vs graph traversal), not because one database has better or different data than the other.

## The IT Dependency Topology

To prove GraphRAG's superiority in complex reasoning, the data must contain multi-hop relationships.
The standard schema should look like this:

`Ticket` --(AFFECTS)--> `Service` --(DEPENDS_ON)--> `Downstream Service` --(OWNED_BY)--> `Team`

### Example Scenario
- **Ticket**: "Ticket-102: System is experiencing high latency."
- **Service**: `PaymentGateway`
- **Downstream Service**: `UserDatabase`
- **Team**: `BackendPlatformTeam`

*Why this works*: If a user asks "Why is the Payment Gateway failing, and who should I contact?", a standard RAG system might retrieve Ticket-102 and say "It has high latency, contact the Payment team." It completely misses the downstream dependency. GraphRAG will traverse the graph, find that the UserDatabase is the root cause, and correctly advise the user to contact the BackendPlatformTeam.

## Implementation Details

When generating data (e.g. in `DataInitializer.java`):

1. **For Neo4j**: 
   - Use Cypher `MERGE` statements to create Nodes (`Ticket`, `Service`, `Team`) and their relationships (`AFFECTS`, `DEPENDS_ON`, `OWNED_BY`).
   - Embed the Ticket text using the `EmbeddingModel` and save it to the Ticket node so it can be queried via Vector Index.

2. **For pgvector**:
   - Save the exact same Ticket text as a `Document` via the `VectorStore`. 
   - Do not explicitly define the relational graph here; pgvector is just receiving flat text documents, representing what a standard RAG pipeline would ingest.
