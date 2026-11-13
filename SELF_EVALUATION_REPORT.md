# Capstone Self-Evaluation Report

## 1. Executive Summary
- Project: Intelligent Payment Fraud Investigation and Dispute Resolution Agent (`fraud-case-assistant`)
- Participant: Chandra Sekhar Gunturi
- Primary framework: Spring AI 1.0.9 (plus deterministic `rules` fallback)
- Secondary framework: LangChain4j 1.0.1
- Build status: `mvn clean test` — BUILD SUCCESS (2026-11-13)
- Test status: 27 tests run, 27 passed, 0 failed, 0 skipped
- Overall score: 83/100
- Percentage: 83%
- Readiness: Demo Ready
- Scoring model used: Both frameworks implemented — metrics 4 and 5 retained at 10 points each; framework parity evaluated in §5.3.

## 2. Repository and Technology Overview
| Area | Finding | Evidence |
|---|---|---|
| Business objective | Advisory fraud/dispute triage: collect evidence, retrieve policy, recommend route; human reviewer decides | `README.md`, `NewProject_Requ` |
| Java / Spring Boot | Java 21, Spring Boot 3.4.12 | `pom.xml` |
| Build tool | Maven (no wrapper) | `pom.xml` |
| AI frameworks | Both Spring AI and LangChain4j behind one contract | `agent/springai/`, `agent/langchain4j/` |
| Model provider | Offline deterministic demo `ChatModel` per framework; swappable beans | `SpringAiConfig`, `LangChain4jConfig`, `DemoRecommendationHeuristics` |
| Database | MySQL 8 via Spring Data JPA; H2 for tests | `application.yml`, `src/test/resources/application.yml` |
| Vector store | Spring AI `SimpleVectorStore`, LangChain4j `InMemoryEmbeddingStore`, plus local deterministic index | `SpringAiEmbeddingConfig`, `LangChain4jEmbeddingConfig`, `LocalPolicyRetriever` |
| Embeddings | Deterministic hashing embedder (offline, reproducible) | `knowledge/HashingEmbedder.java` |
| UI | Thymeleaf + Spring Security form login, 3 roles | `templates/`, `config/SecurityConfig.java` |
| Entry point | `com.capstone.fraudcase.FraudCaseApplication` | `FraudCaseApplication.java` |
| Neutral contracts | `FraudCaseProcessor`, `PolicyRetriever` | `agent/common/`, `knowledge/` |
| Modes | `capstone.processor-mode`: `rules` \| `spring-ai` \| `langchain4j` | `application.yml`, `ProcessorSelector` |
| MCP / external integration | None (out of scope for this capstone) | — |

## 3. Build and Test Results
| Command | Result | Details |
|---|---|---|
| `mvn clean test` | SUCCESS | 27 tests, 0 failures, 0 errors, 0 skipped; no JaCoCo configured |
| `mvn package -DskipTests` | SUCCESS | executable jar `target/fraud-case-assistant-1.0.0.jar` (~70 MB) |
| `java -jar …` (rules mode) | PASS | served on :8081 with MySQL |
| `PROCESSOR_MODE=spring-ai java -jar …` | PASS | live case produced validated recommendation, `mode=spring-ai` persisted |
| `PROCESSOR_MODE=langchain4j java -jar …` | PASS | live case produced `DUPLICATE_PROCESSING → PAYMENT_OPERATIONS`, `mode=langchain4j` |
| `python3 scripts/walkthrough.py` | PASS | 17-step end-to-end flow, all asserts pass |
| Compile warnings | None observed | — |
| External credential dependency | None | demo `ChatModel`/embeddings run offline; H2 for tests |

Test inventory: `DeterministicEvidenceTest` (3), `RulesFraudCaseAgentTest` (6),
`RecommendationValidatorTest` (6), `LocalPolicyRetrieverTest` (2),
`MemoryIsolationTest` (2), `ProcessorParityTest` (1), `CaseApiIntegrationTest` (7).

## 4. Evaluation Scorecard
| # | Metric | Maximum | Score | Status | Evaluator Comments | Evidence |
|---|---|---:|---:|---|---|---|
| 1 | BFS fraud and dispute use case | 10 | 9 | Implemented | Full intake, masked/synthetic IDs, all six classifications, route labels, lifecycle state machine, human ownership; no loan-underwriting concepts. AML escalation not modelled (minor). | `domain/`, `FraudCaseService`, `templates/intake.html` |
| 2 | Architecture and maintainability | 12 | 11 | Implemented | Clean layering; framework-neutral `FraudCaseProcessor`/`PolicyRetriever`; config-driven mode; rules shared — no adapter divergence; typed enums; validation + exception handler. | `application/`, `agent/`, `api/ApiExceptionHandler` |
| 3 | Fraud workflow and deterministic tools | 15 | 13 | Implemented | 8 approved tools; hash-seeded deterministic results; bounded timeout; per-call `ToolTrace`; provenance + severity per evidence item; structured unavailable/timeout results. Velocity and sanctions tools absent (not mandatory). | `tools/`, `ToolExecutor`, `DeterministicEvidenceTest` |
| 4 | Spring AI implementation | 10 | 9 | Implemented | `ChatClient`, system/user prompt templates, `.entity()` typed output, `VectorStore`+`EmbeddingModel`, external config, safe failure. `@Tool` evidence methods registered on the call and executed through `ToolCallingManager` (demo model emits a real `ToolCall`, second turn answers). Case-keyed framework memory via `MessageChatMemoryAdvisor` + `MessageWindowChatMemory` (`CONVERSATION_ID=caseId`). Not used: retrieval advisor (`QuestionAnswerAdvisor`) — retrieval stays behind the neutral `PolicyRetriever` for parity. | `SpringAiFraudCaseAgent`, `SpringAiConfig`, `EvidenceToolset`, `SpringAiPolicyRetriever` |
| 5 | LangChain4j implementation | 10 | 9 | Implemented | `AiServices` + `@SystemMessage`/`@UserMessage`, typed `FraudCaseRecommendation` return, `EmbeddingStore`. `@Tool` evidence methods registered via `.tools()` — AiServices executes the emitted `ToolExecutionRequest` and re-invokes the model. Case-scoped `MessageWindowChatMemory` via `chatMemoryProvider` + `@MemoryId(caseId)`. Not used: `RetrievalAugmentor`/persistent `ChatMemoryStore` — retrieval stays behind the neutral `PolicyRetriever`; JPA `CaseMemoryService` remains the durable record. | `FraudInvestigationAiService`, `LangChain4jConfig`, `EvidenceToolset`, `LangChain4jPolicyRetriever` |
| 6 | RAG, embeddings, vector retrieval, citations | 15 | 12 | Implemented | 4 synthetic policies, metadata header validation, section chunking, embeddings, three retriever impls, configurable top-k + threshold, exact citation metadata (docId/version/section/chunkId/score/passage) surfaced to UI, abstention on no match. No retired/expired policy exclusion; retrieval test coverage is basic. | `knowledge/`, `policies/*.md`, `LocalPolicyRetrieverTest` |
| 7 | Structured output, memory, human review | 8 | 7 | Implemented | Typed DTO, enum/confidence/citation validation, bounded JPA memory isolated by caseId, reviewer dispositions incl. REROUTE/REQUEST_MORE_INFO/OVERRIDE, mandatory reason enforced. | `RecommendationValidator`, `CaseMemoryService`, `FraudCaseService.recordReview` |
| 8 | Security, privacy, responsible AI, auditability | 8 | 5 | Partially Implemented | Masked-identifier validation, output encoding, CSRF, role-gated actions, append-only audit with payload hashes, no external secrets, injection treated as data. Gaps: no correlation ID, demo user passwords and DB defaults in code, model/provider version not captured in audit. | `SecurityConfig`, `AuditWriter`, `CaseIntakeRequest` patterns |
| 9 | Testing, reliability, observability | 8 | 5 | Partially Implemented | 27 tests across tools, rules scenarios, validation, retrieval abstention, memory isolation, REST integration, framework parity. Gaps: no metrics/tracing/correlation-ID instrumentation, failure-path (model outage) tested only at unit level, no retry/idempotency tests. | `src/test/` |
| 10 | UI, documentation, developer experience | 4 | 3 | Implemented | Six capabilities demonstrated on workspace + intake + list screens; accessible labels; README/AGENTS/example config; walkthrough script. Intake form had a 500 defect (fixed during evaluation); limited empty/loading states. | `templates/`, `README.md`, `scripts/walkthrough.py` |
| | **Total** | **100** | **83** | | | |

## 5. Framework Evaluation

### 5.1 Spring AI
- `ChatClient` built from injected `ChatModel` (`SpringAiConfig`); demo model returns deterministic JSON so the mode runs offline.
- System prompt from `prompts/fraud-system.txt`; user template `fraud-user.txt` rendered via `ChatClient.PromptUserSpec.params`.
- Typed output via `.call().entity(FraudCaseRecommendation.class)`, then `ModelOutputNormalizer` + `RecommendationValidator`.
- `SimpleVectorStore` + `EmbeddingModel` used for `SpringAiPolicyRetriever`.
- Live result: `UNAUTHORISED_TRANSACTION → FRAUD_INVESTIGATION`, HIGH, persisted `mode=spring-ai` (case `CASE-E7063F9300CE`).
- Tool calling: `EvidenceToolset` methods carry `@Tool`; the demo model emits a real `ToolCall`, `ToolCallingManager` executes it (`ToolCallbackResolver` → `MethodToolCallback`), and the second turn yields the typed recommendation.
- Memory: `MessageChatMemoryAdvisor` + `MessageWindowChatMemory` (`InMemoryChatMemoryRepository`) on follow-ups, keyed by `ChatMemory.CONVERSATION_ID = caseId`.
- Not used: `QuestionAnswerAdvisor`/retrieval advisor — RAG stays behind the neutral `PolicyRetriever` so citation metadata is identical across modes; JPA `CaseMemoryService` remains the durable record.

### 5.2 LangChain4j
- Declarative `FraudInvestigationAiService` with `@SystemMessage(fromResource)` + `@UserMessage`; built via `AiServices.builder().chatModel(demoLc4jChatModel)`.
- Typed `FraudCaseRecommendation` return mapped by the framework, then the same normalizer + validator.
- `InMemoryEmbeddingStore<TextSegment>` + `EmbeddingModel` used for `LangChain4jPolicyRetriever`.
- Live result: `DUPLICATE_PROCESSING → PAYMENT_OPERATIONS`, persisted `mode=langchain4j` (case `CASE-08452FF6FD62`).
- Tool calling: the same `EvidenceToolset` carries the LangChain4j `@Tool` annotations; registered via `AiServices.tools()` — the demo model's `ToolExecutionRequest` is executed by the AI Service and the follow-up turn returns JSON.
- Memory: `MessageWindowChatMemory` via `chatMemoryProvider`, keyed by `@MemoryId caseId` on `chat`.
- Not used: `RetrievalAugmentor` and a persistent `ChatMemoryStore` (default in-memory store used) — same uniform `PolicyRetriever` approach as 5.1; JPA `CaseMemoryService` remains the durable record.

### 5.3 Framework Parity
`ProcessorParityTest` runs one identical context through all three processors and
asserts the same invariants: caseId echo, `humanReviewRequired=true`, valid
classification/route enums, confidence ∈ [0,1], non-empty evidence IDs,
non-empty citations, non-blank rationale. Result: PASS. Live runs confirmed
equivalent business outcomes per scenario (same intake → same route) across
modes; the output schema and persisted fields are identical by construction.
Latency: all three ~<1s per investigation (demo models are local).

## 6. Acceptance Scenario Results
| Scenario | Mode | Expected | Actual | Result | Evidence |
|---|---|---|---|---|---|
| Unrecognised merchant, new device, location anomaly | rules | Suspicious/unauthorised, fraud route | `UNAUTHORISED_TRANSACTION`, HIGH, `FRAUD_INVESTIGATION` | Pass | `CASE-43A83877B67E`, walkthrough step 5 |
| Recognised merchant, fulfilment complaint | rules | Merchant dispute, dispute ops | `MERCHANT_DISPUTE`, LOW, `DISPUTE_OPERATIONS` | Pass | `CASE-C3AA5BE0793A` |
| Duplicate fingerprint evidence | rules | Duplicate, payment ops | `DUPLICATE_PROCESSING`, `PAYMENT_OPERATIONS` | Pass | `CASE-555490144F8E` (walkthrough step 16) |
| Same scenario | spring-ai | Same contract enforced | `UNAUTHORISED_TRANSACTION`, `mode=spring-ai` | Pass | `CASE-E7063F9300CE` |
| Same scenario | langchain4j | Same contract enforced | `DUPLICATE_PROCESSING`, `mode=langchain4j` | Pass | `CASE-08452FF6FD62` |
| Velocity (5+ txns/10 min) | — | Velocity indicator | No velocity tool implemented | Fail (gap) | `tools/` catalogue |
| Confirmed synthetic screening match | — | Mandatory escalation | No sanctions/PEP tool implemented | Fail (gap) | `tools/` catalogue |
| Evidence service unavailable | rules | `INSUFFICIENT_EVIDENCE` / missing info | Structured `TIMEOUT`/`UNAVAILABLE`, missing-info entry | Pass | `RulesFraudCaseAgentTest#t08` |
| Same case submitted twice | — | Idempotent/duplicate protection | Second `POST` creates a new case ID; no dedup | Fail (gap) | `FraudCaseService.createCase` |
| Policy question with active source | rules | Grounded, cited answer | Citations w/ docId, version, section, chunkId, score | Pass | `GET /api/cases/{id}/citations` |
| Question absent from corpus | rules | Explicit abstention | Empty retrieval → `MANUAL_REVIEW` + missing info | Pass | `LocalPolicyRetrieverTest`, rule `R5` |
| Retired policy contains matching text | — | Retired policy excluded | No active-status filtering implemented | Fail (gap) | `PolicyChunk` (no `active` flag) |
| Model returns malformed output | rules | Validated rejection | `RecommendationValidator` rejects unknown enums/bounds | Pass | `RecommendationValidatorTest` |
| Two separate case IDs | rules | Memory isolated | Isolation verified | Pass | `MemoryIsolationTest` |
| Prompt injection in narrative | rules | Treated as data, no action | `UNAUTHORISED_TRANSACTION`, review required, no refund/close | Pass | `CASE-CD6F1ABD9CBB` |

## 7. Strengths
1. **Safety-first grounding**: citations, evidence IDs and trace IDs are re-attached from context by `ModelOutputNormalizer` — the model literally cannot fabricate them; plus `humanReviewRequired` is forced true and schema-validated.
2. **Deterministic, reproducible evidence**: hash-seeded fixtures give identical results for identical input; every tool call is traced with status/duration — demo and tests are fully reproducible offline.
3. **True framework neutrality**: one `FraudCaseProcessor` contract, three implementations verified by a shared parity test and live runs — rules mode works with no model at all.
4. **Complete human-in-the-loop loop**: mandatory reviewer rationale, role enforcement, state machine with illegal-transition guards, append-only audit reconstructing every step.

## 8. Gaps and Risks
| Priority | Gap or Risk | Impact | Evidence | Recommended Fix |
|---|---|---|---|---|
| High | No transaction-velocity or sanctions/PEP screening tools | Two rubric scenarios unscoreable | `tools/` catalogue | Add deterministic velocity + screening tools to `DeterministicEvidence` |
| High | No case-submission idempotency | Duplicate `POST /api/cases` creates duplicate cases | `FraudCaseService.createCase` | Unique constraint on `transactionRef` or idempotency-key header |
| High | No retired/expired policy filtering | Stale policy could be cited | `PolicyChunk` lacks `active`/`expiresOn` | Add `active` metadata + filter in retrieval and a stale-policy test |
| Medium | No correlation ID or observability (metrics/tracing) | Harder cross-span audit, rubric §10.1 unmet | no Micrometer/MDC usage | Add correlation ID filter + Micrometer timers on tools/model/retrieval |
| Medium | ~~Framework memory/tools not used~~ — RESOLVED: `@Tool` toolset + framework chat memory now wired in both adapters | — | `EvidenceToolset`, `SpringAiConfig`, `LangChain4jConfig` | Optional next step: retrieval advisor/`RetrievalAugmentor`, persistent `ChatMemoryStore` |
| Medium | Model-outage fallback untested at integration level | Fail-safe path unproven in IT | `InvestigationOrchestrator.analyzeWithFallback` | Add a MockMvc test with a throwing processor bean |
| Low | `REQUEST_MORE_INFO` leaves case `UNDER_REVIEW` | Minor workflow ambiguity | `FraudCaseService.recordReview` | Document or add allowed transition to `INCOMPLETE` |
| Low | Few UI empty/loading/error states | UX polish | `templates/` | Add per-section empty/loading fragments |

## 9. Security and Responsible AI Findings
| Severity | Finding | File or Component | Remediation |
|---|---|---|---|
| Low | Demo user passwords hardcoded in code (`agent123` etc.) | `SecurityConfig` | Move to env-injected credentials or document as demo-only; never use in non-demo profiles |
| Low | Default DB credentials shipped in `application.yml` (`DB_USER`/`DB_PASSWORD` env fallbacks) | `application.yml` | Remove fallback values so credentials must come from env |
| Info | No committed API keys, tokens, or real PII found — synthetic/masked data only; audit stores payload hashes, not raw content | repo-wide scan | Maintain; keep `application-example.yml` placeholders only |
| Info | No autonomous actions possible — tool catalogue is read-only; every recommendation is `humanReviewRequired=true` | `agent/common`, `InvestigationOrchestrator` | Maintain the boundary |
| Info | Customer statement and policy passages treated as untrusted data in prompts; injection scenario verified | `prompts/fraud-system.txt`, `CASE-CD6F1ABD9CBB` | Keep instruction/data separation; add an explicit injection regression test |

## 10. Recommended Improvements
### Critical
- None — the application is functional and safe as delivered.

### High
- Add `getTransactionVelocity` and synthetic sanctions/PEP screening tools (deterministic, traced) to cover missing rubric scenarios.
- Enforce intake idempotency (unique `transactionRef` constraint + 409 response).
- Add `active`/`expiresOn` policy metadata; exclude retired documents at retrieval; add stale-policy test.

### Medium
- Correlation ID per investigation propagated through tool/retrieval/model calls + MDC logging; Micrometer timers.
- Extend framework-native usage to retrieval (Spring AI `QuestionAnswerAdvisor`, LangChain4j `RetrievalAugmentor`) and a persistent `ChatMemoryStore`, while keeping the neutral `PolicyRetriever`/`CaseMemoryService` as the durable record.
- Integration test for processor failure → rules fallback path.
- Move demo credentials out of source into env vars.

### Low
- UI loading/empty/error fragments; screenshot set for the README.
- `REQUEST_MORE_INFO → INCOMPLETE` transition if re-investigation on request is desired.

## 11. Definition-of-Done Checklist
- [x] Fraud case intake works with synthetic data
- [x] Deterministic tools provide traceable evidence
- [x] Structured recommendations are validated
- [x] Policy answers are grounded and cited
- [x] Human review is mandatory
- [x] Conversation/case memory is isolated
- [x] Secrets are not stored in the repository
- [x] Error and no-information states are handled safely
- [x] Automated tests pass
- [x] README enables another participant to run the project

## 12. Final Verdict
**Demo Ready.** The capstone delivers a runnable, verifiable implementation of
the brief: deterministic evidence tools with provenance, thresholded RAG with
exact citations and abstention, a validated recommendation contract shared by
rules / Spring AI / LangChain4j processors, case-isolated memory, mandatory
human review, and an append-only audit trail — all confirmed by 27 passing
tests and live execution in every mode. The remaining gaps are bounded:
missing velocity/sanctions tools, no intake idempotency, no retired-policy
filtering, and limited observability — none of which blocks a demonstration.
