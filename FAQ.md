# FAQ: Understanding GraphRAG

## Why do we call it GraphRAG if we don't embed unstructured data in the graph?

Great question. The term **GraphRAG** doesn't mean "do RAG inside a graph database." It means:

> **Use a graph's structured relationships to _augment_ the retrieval step of RAG.**

Standard RAG has one retrieval mechanism: vector similarity search over flat text. **GraphRAG adds a second retrieval mechanism: graph traversal.** The "Graph" in GraphRAG refers to _how_ you retrieve context, not _where_ you store embeddings.

| Approach | Retrieval Method | Context Returned |
|---|---|---|
| **Standard RAG** | Vector similarity only | Flat text chunks |
| **GraphRAG** | Vector similarity + graph traversal | Text + relationships + connected entities |

In our demo, Neo4j uses a vector index to find the **starting node** (the ticket), then **traverses edges** (`DEPENDS_ON`, `OWNED_BY`) to pull in structurally connected context that no amount of text similarity could ever find. That traversal is what makes it "Graph" RAG.

You could even have the vector index live in pgvector and the graph in Neo4j — it's still GraphRAG as long as the graph traversal augments the retrieval.

---

## Can I store team names and relationships as unstructured text in the Postgres table instead?

You _could_ put "This ticket affects Payment Gateway, which depends on Auth Service, which is owned by the Identity Team" into the `description` field and embed it. But here's why it breaks down:

1. **Relationships are implicit, not explicit.** If you add a new dependency between services, you'd have to rewrite and re-embed _every_ ticket description that should reference it. In a graph, you add one edge.

2. **Multi-hop reasoning fails with flat text.** If Service A → Service B → Service C, and tickets only mention Service A, no amount of embedding similarity will surface Service C. A graph traversal finds it in milliseconds.

3. **Data duplication and staleness.** You'd be duplicating structural data (team names, ownership, dependencies) across hundreds of ticket descriptions. When a team changes ownership, which tickets do you update?

4. **The graph is the single source of truth** for structure, and the vector store is the single source of truth for unstructured text. Mixing them creates a maintenance nightmare.

> **Think of it this way:** You wouldn't store your org chart as a paragraph in every employee's profile. You'd store it as a _structure_ and reference it.

---

## What is a multi-hop query?

A "hop" is one step along a relationship in a graph. The number of hops tells you how many relationships you need to traverse to get your answer.

Imagine this simple graph of an IT landscape:

```mermaid
flowchart LR
    Ticket["Ticket #42"]:::ticket -- AFFECTS --> PG["Payment Gateway"]:::service
    PG -- DEPENDS_ON --> Auth["Auth Service"]:::service
    Auth -- OWNED_BY --> IDTeam["Identity Team"]:::team

    classDef ticket fill:#F9B248,stroke:#E5A030,color:#333,stroke-width:2px
    classDef service fill:#4A90D9,stroke:#2C3696,color:#fff,stroke-width:2px
    classDef team fill:#1ABC9C,stroke:#16A085,color:#fff,stroke-width:2px
```

Now compare the queries:

### 0-hop: "What went wrong?"

You land directly on the ticket. No traversal needed.

> **Question:** "What is the error message in Ticket #42?"
>
> **Answer:** Read the ticket text → `"Connection timeout at checkout"`
>
> **Path:** `Ticket #42` → _(read its own text)_

Standard RAG can do this. Just search for similar text.

### 1-hop: "What does it affect?"

One relationship away from the starting point.

> **Question:** "Which service is affected by Ticket #42?"
>
> **Answer:** Follow one edge → `Payment Gateway`
>
> **Path:** `Ticket #42` → _AFFECTS_ → `Payment Gateway`

Standard RAG _might_ get this if the ticket text explicitly mentions the service name.

### 2-hop: "Who should I call?"

Two relationships away. This is where graphs shine and flat text fails.

> **Question:** "Which team owns the root cause of Ticket #42?"
>
> **Answer:** Follow two edges → `Identity Team`
>
> **Path:** `Ticket #42` → _AFFECTS_ → `Payment Gateway` → _DEPENDS_ON_ → `Auth Service` → _OWNED_BY_ → `Identity Team`

Standard RAG **cannot** answer this. The ticket text says "Payment Gateway timeout" — it never mentions Auth Service or Identity Team. Only the graph knows those connections.

### The pattern

| Hops | Question Type | Example | RAG? | GraphRAG? |
|---|---|---|---|---|
| **0** | Direct lookup | "What's the error?" | | |
| **1** | One relationship | "Which service is affected?" | Maybe | |
| **2** | Chained relationships | "Who owns the root cause?" | No | |
| **3+** | Deep traversal | "What else breaks if this goes down?" | No | |

> **Rule of thumb:** The more hops your question requires, the more you need a graph. Standard RAG tops out at ~0-1 hops. GraphRAG handles any depth.
