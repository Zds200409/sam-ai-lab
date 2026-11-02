# Sam AI Assistant

> A membership management system upgraded with an LLM-powered service desk.
> Java + Spring Boot 3.4 + Spring AI, designed around retrieval-augmented generation,
> tool calling into real business APIs, streaming responses, graceful degradation and
> an evaluation harness that runs on every prompt or model change.

[![Status](https://img.shields.io/badge/status-RAG%20%2B%20tool%20calling%20working-green)](https://github.com/Zds200409/sam-ai-lab)
[![Java](https://img.shields.io/badge/java-17-blue)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/spring--boot-3.4.5-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/spring--ai-1.0.0-6DB33F)](https://docs.spring.io/spring-ai/reference/)

---

## Project status

| Layer | State | Notes |
| --- | --- | --- |
| Membership management system | Done | Full CRUD backend, RBAC, member portal, order and points logic |
| AI service desk | Working | Spring AI 1.0.0 wired to DeepSeek over the OpenAI-compatible protocol; multi-turn memory, SSE streaming and failure fallback all verified |
| Grounded Q&A (RAG) | Working | 65 chunks from two document families, retrieved through a modular RAG advisor with query compression |
| Tool calling | Working | Three read-only tools resolve tier, points and recent orders from MySQL at request time |
| Evaluation harness | Working | 55 graded questions across 7 categories, baseline 68% → 100% after grounding and tool access |
| Observability layer | Planned | `ai_chat_log` schema designed, implementation pending |

Verified behaviour of the current AI layer:

| Scenario | Result |
| --- | --- |
| Synchronous Q&A | HTTP 200, ~1.7 s round trip including model latency |
| Streaming (SSE) | Token-level output confirmed over `text/event-stream` |
| Multi-turn context | Third turn correctly recalled a member tier stated in the first turn |
| Session isolation | The same question under a different `conversationId` knew nothing about the first session |
| Empty input | HTTP 400 with a validation message, no model call issued |
| Invalid API key | HTTP 500 with a user-facing fallback message, service stays up, full exception stack logged |
| Model timeout / unreachable | HTTP 503 fallback, request tracing preserved |

This repository is a working portfolio project. The AI layer is built in
public, commit by commit, and the Roadmap section reflects actual progress
rather than intent.

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
| Grounded Q&A (RAG) | FAQ entries and charter clauses are chunked, embedded and retrieved; answers are generated from the retrieved chunks with a source label | Keeps responses traceable to a source, limits hallucination to a measurable surface |
| Tool calling | The model invokes read-only business lookups for tier, points balance and recent orders, keyed by phone number | Live data must come from the database, never from the model's imagination |
| Streaming (SSE) | Token-level streaming to the client | Perceived latency drops from seconds to sub-second, which changes whether the feature gets used at all |
| Session memory | Multi-turn context stored in MySQL, isolated per conversation | Real support questions are multi-step, single-shot memory feels broken |
| Graceful degradation | Three tiers: normal answer, low-confidence handoff, model-failure handoff | A probability system must fail predictably; an unanswered ticket is recoverable, a confidently wrong answer is not |
| Observability | Every request logged with retrieved document IDs, answer, handoff flag, token usage and latency | Without logs there is no way to measure hit rate or diagnose a bad answer |

---

## Architecture

```
                  ┌──────────────────┐
   Client  ──────▶│ AiAssistantCtrl  │◀─── SSE stream
                  └────────┬─────────┘
                           │
                  ┌────────▼─────────────┐
                  │  AiAssistantService  │
                  │  ChatClient +        │
                  │  advisor chain       │
                  └──┬────────┬───────┬──┘
                     │        │       │
        ┌────────────▼──┐  ┌──▼──────────────┐  ┌▼─────────────────┐
        │ Modular RAG   │  │  ToolCalling    │  │  Fallback / Log  │
        │ compress +    │  │ read-only APIs  │  │ handoff + tracing│
        │ retrieve + top-k│ └──────┬──────────┘  └──────┬──────────┘
        └────────┬──────┘         │                    │
                 │                │                    │
        ┌────────▼──────┐  ┌──────▼──────┐     ┌───────▼────────┐
        │SimpleVectorSt.│  │ MySQL 8.x   │     │  ai_chat_log   │
        │ + JSON snap.  │  │ (business + │     │  (observability)│
        │ (embeddings)  │  │  chat mem.) │     │                │
        └───────────────┘  └─────────────┘     └────────────────┘
                     │
              ┌──────▼───────┐
              │  LLM provider│
              │ DeepSeek API │
              │SiliconFlow   │
              │  (bge-m3)    │
              └──────────────┘
```

Two model providers are in play and they are not interchangeable. DeepSeek
serves chat completions and exposes no embedding endpoint, so embeddings come
from SiliconFlow running `BAAI/bge-m3`. Both speak the OpenAI-compatible
protocol, so they share one client class but need two configured instances,
because Spring AI's `base-url` is global rather than per-call.

The two halves are deliberately asymmetric. Retrieval and tool calling are
read-only paths that touch production-like data, so they are the parts that get
the most defensive engineering. The provider is abstracted behind Spring AI's
client interface so the model can be swapped without touching business code.

---

## Tech stack

| Layer | Choice |
| --- | --- |
| Backend | Spring Boot 3.4.5, JDK 17, Jakarta EE namespace |
| Persistence | MyBatis-Plus 3.5.7 (`mybatis-plus-spring-boot3-starter`), MySQL 8.x; chat memory and vector snapshot both persist to disk |
| Security | JWT (jjwt 0.12.6), BCrypt, HandlerInterceptor, separate member / admin token spaces |
| AI | Spring AI 1.0.0 (`spring-ai-starter-model-openai`), DeepSeek for chat, SiliconFlow `bge-m3` for embeddings, `SimpleVectorStore` with a JSON snapshot |
| Frontend | Vue 3, Vite, Element Plus, Pinia, Vue Router, ECharts |
| Observability | `ai_chat_log` table, Micrometer metrics, structured request tracing |
| API docs | springdoc-openapi 2.7.0 + `knife4j-openapi3-ui` (`/doc.html` when running) |
| Build | Maven Wrapper, npm |

> **Why Spring Boot 3.4?** Spring AI 1.0 requires Spring Boot 3.4+. Staying on
> 2.7 would have meant either an unsupported framework pairing or dropping to a
> raw HTTP client and hand-rolling RAG and tool calling. The upgrade touched
> 22 `javax.*` references across 16 files, of which only two namespaces
> (`servlet`, `validation`) needed migrating; `javax.crypto` and `javax.imageio`
> are JDK-owned and correctly stay as they are.

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

### AI layer configuration

The AI module runs through Spring AI's OpenAI-compatible client, so a hosted API
and a local model share the same code path. The API key is injected from the
environment and never committed.

```yaml
spring:
  ai:
    openai:
      # base-url must not include /v1, Spring AI appends the path itself
      base-url: https://api.deepseek.com
      api-key: ${DEEPSEEK_API_KEY}
      chat:
        options:
          model: deepseek-chat
          temperature: 0.7
          max-tokens: 2048
      client:
        connect-timeout: 10s
        read-timeout: 60s
        max-retries: 2

sam:
  ai:
    vector-store:
      # snapshot path, relative to the working directory
      path: data/vector-store.json
```

Embeddings are a separate provider. DeepSeek has no embedding endpoint, so the
retrieval side talks to SiliconFlow instead, and the key goes into the same
profile file:

```yaml
sam:
  ai:
    embedding:
      base-url: https://api.siliconflow.cn
      api-key: ${SILICONFLOW_API_KEY}
      model: BAAI/bge-m3
```

`bge-m3` outputs 1024 dimensions and rejects a `dimensions` parameter, so the
request must not send one. Sending it returns `20015 The parameter is invalid`,
which looks like an auth failure but is not.

Set the key before starting. Two ways, pick one.

**Option A, local config file (recommended).** Create
`src/main/resources/application-local.yml`:

```yaml
spring:
  ai:
    openai:
      api-key: sk-xxxx
```

Then confirm `application.yml` activates the profile:

```yaml
spring:
  profiles:
    active: dev,local
```

`application-local.yml` is listed in `.gitignore`, so the key never reaches the
repository. A profile file that does not exist is simply skipped, so other
developers can clone and run without it as long as they supply a key some
other way.

**Option B, environment variable.** Valid for the current shell only:

```bash
export DEEPSEEK_API_KEY=sk-xxxx        # Windows: $env:DEEPSEEK_API_KEY="sk-xxxx"
```

The application refuses to start when no key is present. `OpenAiChatModel` is a
singleton bean created during startup, and a blank key throws
`OpenAI API key must be set` before any endpoint becomes reachable.

Two endpoints are exposed. Both accept an optional `conversationId`. Calls
sharing the same value share conversation history, different values stay
isolated.

```bash
# synchronous, multi-turn
curl "http://localhost:8081/api/ai/chat?question=my+tier+is+premium&conversationId=t1"
curl "http://localhost:8081/api/ai/chat?question=what+benefits+does+that+give+me&conversationId=t1"

# streaming (SSE), token by token
curl -N "http://localhost:8081/api/ai/chat/stream?question=what+tiers+exist&conversationId=t1"
```

History is persisted in MySQL through `JdbcChatMemoryRepository`, so a restart
does not wipe the conversation and the table survives redeploys. Spring AI ships
no MySQL schema script for this repository type, so the table is created
manually and `initialize-schema` is set to `never`. Leaving it on `embedded`
makes startup fail with `No schema scripts found ... schema-mysql.sql`.

A minimal chat UI ships at `http://localhost:8081/ai-chat.html`. Plain HTML with
no build step, it renders the SSE stream token by token and generates a fresh
conversation ID per session.

### Knowledge base

Two document families feed the same vector store:

| File | Structure | Chunking | Chunks |
| --- | --- | --- | --- |
| `knowledge/sam-faq.md` | `## heading` per Q&A | one chunk per heading | 22 |
| `knowledge/sam-charter.md` | `## chapter` / `### clause` | one chunk per clause, chapter carried as metadata | 43 |

After editing either file, reindex through the admin endpoint. Ingestion clears
the store first and rewrites the snapshot, so running it twice leaves 65
documents, not 130.

```bash
curl -X POST "http://localhost:8081/api/ai/admin/reindex"
```

The vector store is `SimpleVectorStore`, held in memory and snapshotted to
`data/vector-store.json` on ingest. On startup the snapshot is loaded back, so
restarting does not cost an embedding round trip. Two consequences are worth
stating plainly: the snapshot is not shared between instances, and a stale
snapshot silently serves old policy until reindex runs.

---

## Business domain

The AI layer is only interesting if the rules are real, so the underlying
system carries genuine business logic that the assistant has to respect.

- Points: `P = floor(A x r)`, where `A` is the goods amount actually paid and
  `r` is the tier points rate. Delivery fees earn no points
- Discount: `payable = goods x (1 - d) + delivery`, where `d` is the tier
  discount rate. Gift cards, tobacco, alcohol and items flagged `discount_excluded`
  settle at list price
- Delivery: free above 180 on the goods amount, 15 otherwise. Not a tier benefit
- Tier: both tiers are bought outright, there is no cumulative-spend threshold.
  Upgrading from the entry tier costs the 420 annual-fee difference, prorated by
  remaining validity
- Expiry: points older than 90 days past their expiry date are swept to status
  expired; the sweep does not touch the lifetime-earned counter
- Transaction boundaries: settlement, refund and points adjustment are all
  `@Transactional(rollbackFor = Exception.class)`, atomically writing across
  orders, order items, stock and member balance

The tier rule above is worth flagging because it used to be wrong in this
repository. The table carried a `min_consumption` column that no entity mapped
and no code read, yet it had leaked into the prompt, the FAQ and the charter,
and the assistant dutifully told users they needed to spend 50,000 to join.
A dead column is invisible at runtime and extremely convincing in generated
prose, which is why the eval set pins this rule down explicitly.

An assistant that cannot reproduce these rules with citations is worse than no
assistant, because users trust it.

---

## Demo

Run it locally and open `http://localhost:8081/ai-chat.html`. Three questions
show the whole pipeline in under a minute:

| Ask | What you should see |
| --- | --- |
| What discount does a premium tier get? | Answer retrieved from the charter, ending with a source line, no tool call |
| What is my points balance? | The model asks for a phone number first |
| What is my points balance, phone 18827578719 | Live value from MySQL, not a guess |

The second question is the one worth watching. Before tool calling was wired,
the assistant answered that it could not see account data. It now asks for the
identifier instead of refusing, which is the behaviour a real service desk needs.

Screenshots and a recorded walkthrough go in `docs/demo/` once captured.

---

## Evaluation

A 55-question set lives in `eval/questions.jsonl`, split by what is being
tested rather than by topic. `eval/run_eval.py` drives the live endpoint and
grades each answer against expected and forbidden phrases.

| Category | What it probes | First run | Current |
| --- | --- | --- | --- |
| A Member tier | Fees, rates, discount per tier | 10/10 | 10/10 |
| B Points | Accrual, timing, redemption, expiry | 10/10 | 10/10 |
| C Returns | Windows, eligibility, refund path | 0/10 | 10/10 |
| D Delivery | Fees, thresholds, timing | 2/6 | 6/6 |
| E Out of scope | Must refuse rather than invent | 8/8 | 8/8 |
| F Multi-turn | Follow-ups that depend on earlier turns | 4/6 | 6/6 |
| G Tool calling | Live member data through the tools | not wired | 5/5 |
| **Total** | | **34/50 (68%)** | **55/55** |

The first run is the interesting one. All 16 failures were honest refusals, not
invented answers: the model did not know the return window and said so. That is
the right failure mode, and it moved the work from prompt tuning to knowledge
coverage. Ingesting the membership charter alone took the set from 68% to 100%
without touching a single prompt.

Three caveats worth stating, because a 100% headline invites the wrong reading:

- The grader matches keywords, so it measures "did the required rule appear"
  rather than "is the answer good". It has produced false positives on
  refusal sentences and false negatives on Chinese synonyms, both fixed by
  widening the alternatives, which is a cost not a solution
- One conversation answered correctly and then downgraded itself with
  "I do not have precise information on that", which keyword grading cannot see
- The set was written by the same person who wrote the knowledge base, so it
  shares blind spots with it. It catches regressions, it does not prove
  generalisation

Reproduce with:

```bash
python eval/run_eval.py --tag my-change
```

---

## Roadmap

- [x] Isolate the AI project environment (separate database, port, git history)
- [x] Baseline system running and verified on port 8081
- [x] Spring AI client wired to DeepSeek, streaming chat endpoint
- [x] RAG pipeline: chunking, embedding, vector storage, similarity retrieval
- [x] Modular RAG advisor with query compression for follow-up questions
- [x] Tool calling against member tier, points balance and recent orders
- [x] Evaluation set with regression checks
- [ ] Replace keyword grading with model-based or human grading
- [ ] Graceful degradation tiers and the `ai_chat_log` observability table
- [ ] Cost accounting per conversation and a monthly budget estimate
- [ ] Public deployment, demo video, live metrics
- [ ] Hybrid retrieval plus rerank, once the corpus outgrows simple similarity

---

## Notes

- The membership management base is an earlier personal project. The AI service
  desk layer, its architecture and all AI-related code are original work built
  for this repository.
- The `ai_chat_log` schema and the retrieval design were written before the
  implementation, so the design decisions and their reasoning are recorded in
  the commit history.
- The tools are read-only and keyed on a phone number with no second factor.
  That is acceptable for a demo whose data is synthetic, and unacceptable in
  production, where verifying the caller before returning tier, points or order
  history is the minimum bar.
- No licence file is attached on purpose. This is a portfolio repository.

---

Built with Spring Boot, Spring AI and a lot of iteration.
