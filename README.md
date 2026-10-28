# Sam AI Assistant

> A membership management system upgraded with an LLM-powered service desk.
> Java + Spring Boot 2.7 + Spring AI, designed around retrieval-augmented generation,
> tool calling into real business APIs, streaming responses, graceful degradation and
> end-to-end request tracing.

[![Status](https://img.shields.io/badge/status-AI%20layer%20in%20development-orange)](https://github.com/Zds200409/sam-ai-lab)
[![Java](https://img.shields.io/badge/java-17-blue)](https://adoptium.net/)
[![Spring Boot](https://img.io/badge/spring--boot-2.7.18-brightgreen)](https://spring.io/projects/spring-boot)

---

## Project status

| Layer | State | Notes |
| --- | --- | --- |
| Membership management system | Done | Full CRUD backend, RBAC, member portal, order and points logic |
| AI service desk | In development | Spring AI integration under active build, see Roadmap |
| Observability layer | Planned | `ai_chat_log` schema designed, implementation pending |

This repository is a working portfolio project. The AI layer is being built in
public, commit by commit, and the Roadmap section is kept honest with the actual
progress. Numbers marked `TBD` are placeholders waiting on real measurements,
never invented values.

---

## Why this project

A membership system generates a large volume of repetitive, rule-bound questions:
points calculation, tier upgrade thresholds, discount rates, refund windows,
delivery status. Two problems show up in practice.

**Keyword matching breaks on natural phrasing.** A user asking "how much do I
get back if I spend 800 this month" never matches the literal FAQ entry
"points accrual formula". The mapping from intent to rule is not a string
comparison, it is a semantic problem.

**Hard-coded answers drift out of date.** Every business rule lives in code and
in scattered documents. When the discount table changes, every hand-written
answer becomes wrong, and nobody notices until a customer complains.

The direction this project takes is to keep the LLM grounded in the business
rules themselves. The model does not remember policy, it retrieves the policy
and cites it. And for anything that depends on live data, the model does not
guess, it calls the real business API and reports what it actually returned.

---

## What the AI layer is designed to do

| Capability | Behaviour | Engineering reason |
| --- | --- | --- |
| Grounded Q&A (RAG) | FAQ and policy documents are chunked, embedded and stored in `pgvector`; answers are generated from retrieved chunks with citations | Keeps responses traceable to a source, limits hallucination to a measurable surface |
| Tool calling | The model can invoke real business APIs, e.g. read a member tier, points balance or order state | Live data must come from the database, never from the model's imagination |
| Streaming (SSE) | Token-level streaming to the client | Perceived latency drops from seconds to sub-second, which changes whether the feature gets used at all |
| Session memory | Multi-turn context kept in Redis, isolated per conversation | Real support questions are multi-step, single-shot memory feels broken |
| Graceful degradation | Three tiers: normal answer, low-confidence handoff, model-failure handoff | A probability system must fail predictably; an unanswered ticket is recoverable, a confidently wrong answer is not |
| Observability | Every request logged with retrieved document IDs, answer, handoff flag, token usage and latency | Without logs there is no way to measure hit rate or diagnose a bad answer |

---

## Architecture

```
                  ┌──────────────┐
   Client  ──────▶│ ChatController│◀─── SSE stream
                  └──────┬───────┘
                         │
                  ┌──────▼───────────────┐
                  │      ChatService     │
                  │  prompt assembly +   │
                  │  conversation memory │
                  └──┬────────┬───────┬──┘
                     │        │       │
        ┌────────────▼──┐  ┌──▼──────────────┐  ┌▼─────────────────┐
        │   Retriever   │  │  ToolCalling    │  │  Fallback / Log  │
        │ chunk + embed │  │ read-only APIs  │  │ handoff + tracing│
        │ + top-k + rank│  └──────┬──────────┘  └──────┬──────────┘
        └────────┬──────┘         │                    │
                 │                │                    │
        ┌────────▼──────┐  ┌──────▼──────┐     ┌───────▼────────┐
        │   pgvector    │  │ MySQL 8.x   │     │  ai_chat_log   │
        │  (embeddings) │  │ (business)  │     │  (observability)│
        └───────────────┘  └─────────────┘     └────────────────┘
                     │
              ┌──────▼───────┐
              │  LLM provider│
              │ DeepSeek API │
              │ Ollama local │
              └──────────────┘
```

The two halves are deliberately asymmetric. Retrieval and tool calling are
read-only paths that touch production-like data, so they are the parts that get
the most defensive engineering. The provider is abstracted behind Spring AI's
client interface so the model can be swapped without touching business code.

---

## Tech stack

| Layer | Choice |
| --- | --- |
| Backend | Spring Boot 2.7.18, JDK 17, Spring AI (in progress) |
| Persistence | MyBatis-Plus 3.5.3.1, MySQL 8.x, pgvector for embeddings |
| Security | JWT (jjwt 0.11.5), BCrypt, HandlerInterceptor, separate member / admin token spaces |
| AI | Spring AI, DeepSeek API, Ollama for local models, RAG over pgvector |
| Frontend | Vue 3, Vite, Element Plus, Pinia, Vue Router, ECharts |
| Observability | `ai_chat_log` table, Micrometer metrics, structured request tracing |
| API docs | Knife4j 4.1.0 (`/doc.html` when running) |
| Build | Maven Wrapper, npm |

---

## Getting started

### Prerequisites

1. JDK 17
2. MySQL 8.x with a local `root` account, database config in `src/main/resources/application.yml`
3. Node.js 20.19+ or 22.12+ (Vite 8 requirement)

### Database

```bash
mysql -u root -p
CREATE DATABASE sam_ai_lab DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
source sql/schema.sql
source sql/data.sql
```

Optional demo data with roughly 30 members and 200 recent orders:

```bash
python sql/seed_demo_data.py
```

### Run

```bash
# backend, port 8081
./mvnw spring-boot:run

# frontend, port 5173
cd frontend && npm install && npm run dev
```

Open `http://localhost:5173`. Demo accounts use password `123456`:
`admin`, `manager01`, `cashier01`, `delivery01`.

### AI layer configuration (planned)

The AI module is wired through Spring AI's OpenAI-compatible client so both a
hosted API and a local model work with the same code path.

```yaml
spring:
  ai:
    openai:
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat
    vectorstore:
      pgvector:
        initialize-schema: true
```

For a fully local setup, point the client at Ollama instead. No external
network dependency, no per-token cost.

---

## Business domain

The AI layer is only interesting if the rules are real, so the underlying
system carries genuine business logic that the assistant has to respect.

- Points: `P = A x r`, where `A` is the settled amount and `r` is the tier
  points rate, floored
- Discount: `D = T x (1 - d)`, where `T` is the item total and `d` is the tier
  discount rate
- Tier upgrade: triggered when cumulative spend crosses the next tier's
  `min_consumption`, recorded in a change log
- Transaction boundaries: settlement, refund and points adjustment are all
  `@Transactional(rollbackFor = Exception.class)`, atomically writing across
  orders, order items, stock and member balance

An assistant that cannot reproduce these rules with citations is worse than no
assistant, because users trust it.

---

## Demo

| Screenshot | What it shows |
| --- | --- |
| `docs/demo/chat-streaming.png` | Streaming answer with source citations (TBD) |
| `docs/demo/tool-calling.png` | Tool call resolving a real member tier (TBD) |
| `docs/demo/fallback-handoff.png` | Low-confidence handoff path (TBD) |

A 3-minute demo script is available in `docs/demo-script.md` (TBD).

---

## Metrics

Measurements are taken from `ai_chat_log` and are not yet populated. Placeholders
are kept explicit so no number here is decorative.

| Metric | Value | How it is measured |
| --- | --- | --- |
| Grounded answer hit rate | TBD | Share of questions answered with a citation above the similarity threshold |
| Human handoff rate | TBD | Share of questions routed to a human, from `ai_chat_log.handoff` |
| Average response latency | TBD | Mean end-to-end latency from `ai_chat_log.latency_ms` |
| Retrieval precision at k | TBD | Share of top-k chunks that contain the answering rule |

---

## Roadmap

- [x] Isolate the AI project environment (separate database, port, git history)
- [x] Baseline system running and verified on port 8081
- [ ] Spring AI client wired to DeepSeek and Ollama, first streaming chat endpoint
- [ ] RAG pipeline: chunking, embedding, `pgvector` storage, top-k retrieval, rerank
- [ ] Tool calling against member tier, points balance and order status
- [ ] Graceful degradation tiers and the `ai_chat_log` observability table
- [ ] Evaluation set with regression checks on every prompt or model change
- [ ] Deployment, demo video, public metrics

---

## Notes

- The membership management base is a personal graduation project. The AI
  service desk layer, its architecture and all AI-related code are original
  work built for this portfolio.
- The `ai_chat_log` schema and the retrieval design were written before the
  implementation, so the design decisions and their reasoning are recorded in
  the commit history.
- No licence file is attached on purpose. This is a learning portfolio
  repository and the underlying system originates from a graduation project.

---

Built with Spring Boot, Spring AI and a lot of iteration.
