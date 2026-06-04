---
name: general-guidelines
description: Core coding and behavioral guidelines for AI agents working on this project. Must be followed for all codebase changes.
---

# General Guidelines

When working on this codebase, AI agents must strictly adhere to the following principles.

## 1. Karpathy-Inspired Coding Principles

1. **Think Before Coding**: Don't assume. State assumptions explicitly. Stop and seek clarification when confused. Surface tradeoffs.
2. **Simplicity First**: Write the minimum code necessary to solve the problem. Avoid speculative generalizations, overengineering, or bloated abstractions. If 200 lines could be 50, rewrite it.
3. **Surgical Changes**: Touch only what you must. Don't refactor adjacent code or "clean up" unrelated comments. Match the existing style perfectly.
4. **Goal-Driven Execution**: Rely on tests. Have a verifiable success criteria. Validate your changes.

## 2. Framework Guidelines

1. **Spring AI Version**: This project uses **Spring AI 1.1.7**. Do NOT rely on training data regarding Spring AI, as the APIs evolve rapidly. 
2. **Consult Documentation**: Always consult the `../spring_ai_1.1.7_docs.md` (relative to project root) to ensure usage of the correct classes, builders, and vector stores.
3. **Tech Stack**: 
   - **LLM**: Google Vertex AI Gemini
   - **Standard RAG**: PostgreSQL with `pgvector` (`VectorStore` abstraction)
   - **GraphRAG**: Neo4j (`Neo4jClient` for Cypher queries)
