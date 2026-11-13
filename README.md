# Fraud Case Assistant (Capstone)

An explainable, agentic fraud-investigation assistant for the Banking & Financial
Service capstone. It collects deterministic evidence through allow-listed tools,
retrieves applicable policy passages with exact citation metadata, recommends a
case route, and always leaves the final disposition to a human reviewer.

**Safety boundary:** the application may recommend a route, priority or evidence
request. It never blocks a card, issues a refund, moves funds, files a report or
contacts a customer. Every recommendation has `humanReviewRequired=true`.

## Stack

- Java 21, Spring Boot 3.4 (Web, Thymeleaf, Data JPA, Validation, Security)
- **Spring AI 1.0.9** — `ChatClient`, prompt templates, typed structured output, `VectorStore`
- **LangChain4j 1.0.1** — declarative `AiServices`, `ChatModel`, `EmbeddingStore`
- MySQL 8 (H2 for tests)

## Architecture

```
Experience    REST API (/api/cases/**) + Thymeleaf UI (/ui/cases/**)
Application   FraudCaseService, InvestigationOrchestrator, CaseMemoryService, ProcessorSelector
Domain        enums, CaseIntakeRequest, FraudCaseContext, FraudCaseRecommendation, PolicyPassage
Processors    FraudCaseProcessor contract:
                rules        -> RulesFraudCaseAgent          (no model dependency)
                spring-ai    -> SpringAiFraudCaseAgent        (ChatClient + entity output)
                langchain4j  -> LangChain4jFraudCaseAgent     (AiServices typed output)
Tools         ToolExecutor (timeout + trace) -> DeterministicEvidence fixtures
              EvidenceToolset (@Tool methods for both frameworks -> same fixtures)
Knowledge     PolicyIngestionService -> PolicyRetriever (local | Spring AI VectorStore | LC4J EmbeddingStore)
Persistence   JPA entities + append-only AuditWriter (payload hashes, no secrets)
```

Evidence collection is deterministic and separated from model-assisted
interpretation. State transitions live in application services — never in model
output. Citations, evidence IDs and trace IDs are re-attached by the
`ModelOutputNormalizer` so a model can never invent them.

## Modes

Select with `capstone.processor-mode` (env `PROCESSOR_MODE`):

| Mode          | Description |
|---------------|-------------|
| `rules`       | Deterministic fallback, works with no model. Default. |
| `spring-ai`   | Spring AI ChatClient path (demo offline `ChatModel` included). |
| `langchain4j` | LangChain4j AI Service path (demo offline `ChatModel` included). |

`capstone.llm.provider=demo` ships an offline deterministic `ChatModel` for each
framework so every mode runs without credentials. For a real provider, replace
the `ChatModel` beans in `SpringAiConfig` / `LangChain4jConfig` (and the
`EmbeddingModel` beans for embeddings) — nothing else changes.

### Framework-native tools and memory

Each AI mode also exercises its framework's own tool-calling and chat-memory
APIs on top of the shared contract:

- **Tools**: `tools/EvidenceToolset` exposes the allow-listed read-only evidence
  tools as `@Tool`-annotated methods (Spring AI and LangChain4j annotations on
  the same methods). Spring AI registers them via `ChatClient.tools(...)` and
  executes model `ToolCall`s through `ToolCallingManager`; LangChain4j registers
  them via `AiServices.tools()` and executes `ToolExecutionRequest`s. The demo
  models emit a real tool request on the first turn so the loop is exercised
  offline.
- **Memory**: Spring AI uses `MessageChatMemoryAdvisor` +
  `MessageWindowChatMemory` keyed by `ChatMemory.CONVERSATION_ID = caseId`;
  LangChain4j uses `MessageWindowChatMemory` via `chatMemoryProvider` +
  `@MemoryId`. JPA `CaseMemoryService` remains the durable, audited record.

## Build, run, test

```bash
# Prereqs: Java 21, Maven 3.8+, MySQL 8
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS fasystem;"

export DB_URL='jdbc:mysql://localhost:3306/fasystem?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC'
export DB_USER=fauser
export DB_PASSWORD=***

mvn clean verify          # unit + integration tests (H2, no MySQL needed)
mvn spring-boot:run       # starts on http://localhost:8080
```

UI: `http://localhost:8080/ui/cases` — users: `agent/agent123`,
`investigator/investigator123`, `reviewer/reviewer123`.

Switch modes without rebuilding:

```bash
PROCESSOR_MODE=spring-ai mvn spring-boot:run
PROCESSOR_MODE=langchain4j mvn spring-boot:run
```

## REST API

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/cases` | Create + validate a fraud case |
| GET  | `/api/cases/{id}` | Case summary + state |
| POST | `/api/cases/{id}/investigations` | Start/restart bounded analysis |
| GET  | `/api/cases/{id}/evidence` | Evidence items + provenance |
| GET  | `/api/cases/{id}/tool-traces` | Redacted tool execution trace |
| GET  | `/api/cases/{id}/recommendation` | Latest validated recommendation |
| GET  | `/api/cases/{id}/citations` | Policy citation metadata |
| GET  | `/api/cases/{id}/audit` | Append-only audit events |
| POST | `/api/cases/{id}/messages` | Case-scoped follow-up |
| POST | `/api/cases/{id}/reviews` | Reviewer disposition + rationale (REVIEWER role) |

Sample:

```bash
curl -u agent:agent123 -X POST localhost:8080/api/cases \
  -H 'Content-Type: application/json' -d '{
    "maskedCustomerRef":"CUST-NEWDEV-4821","homeRegion":"IN-MH",
    "transactionRef":"TXN-2026-784102","amount":48750.00,"currency":"INR",
    "occurredAt":"2026-11-06T08:42:00+05:30","status":"POSTED",
    "channel":"CARD_NOT_PRESENT","maskedMerchant":"NORTHSTAR DIGITAL ••••73",
    "issueType":"MERCHANT_NOT_RECOGNISED",
    "customerStatement":"I do not recognise this purchase and still have my card.",
    "cardPossession":true,"merchantRecognised":false,
    "syntheticDataConfirmed":true,"reviewRequired":true}'

curl -u investigator:investigator123 -X POST localhost:8080/api/cases/CASE-xxx/investigations
curl -u reviewer:reviewer123   localhost:8080/api/cases/CASE-xxx/recommendation
curl -u reviewer:reviewer123 -X POST localhost:8080/api/cases/CASE-xxx/reviews \
  -H 'Content-Type: application/json' \
  -d '{"disposition":"ACCEPT","reason":"Evidence supports the route."}'
```

Useful demo inputs: `TXN-DUP-*` → duplicate fingerprint; `CUST-NEWDEV-*` →
unknown device + unavailable auth outcome; `*BLOCKLIST*` merchant → blocklist hit.

## Tests

`mvn test` runs: intake validation, deterministic-fixture reproducibility,
rules-mode scenarios (unauthorised / merchant dispute / duplicate / abstention /
unavailable tool), schema validation, retrieval + abstention, case-memory
isolation, framework parity across all three processors, and an end-to-end
REST flow including mandatory reviewer rationale.

## Limitations

- Demo `ChatModel`/embedding implementations are deterministic stand-ins; wire a
  real provider before any production-like evaluation.
- Policy vectors are in-memory and re-ingested at startup (fine for training).
- `REQUEST_MORE_INFO` keeps the case `UNDER_REVIEW` until a final disposition.
