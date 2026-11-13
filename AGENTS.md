# AGENTS.md

## Technology

- Java 21, Spring Boot 3.4.12, Maven.
- Spring AI 1.0.9 (`spring-ai-client-chat`, `spring-ai-vector-store`) and
  LangChain4j 1.0.1 (`langchain4j`, `langchain4j-core`). Versions are pinned in
  `pom.xml` properties — verify API signatures against these exact versions
  before changing them.
- MySQL 8 via `DB_URL`/`DB_USER`/`DB_PASSWORD` env vars. Tests use H2 only.

## Build / verify

```bash
mvn -q compile          # compile
mvn test                # full test suite (no MySQL needed)
mvn spring-boot:run     # run (needs MySQL unless overridden)
```

## Rules

- Domain, tools, persistence and controllers must stay framework-neutral. All
  AI integration lives behind `FraudCaseProcessor` (`agent/common`) and
  `PolicyRetriever` (`knowledge`). `tools/EvidenceToolset` is the single
  exception: it carries both frameworks' `@Tool` annotations on the same
  methods so framework tool-calling executes the identical deterministic
  fixtures — keep the two annotations in sync.
- Every recommendation must pass `RecommendationValidator` before persistence
  or display: valid enums, confidence 0–1, case ID, `humanReviewRequired=true`,
  evidence or explicit INSUFFICIENT_EVIDENCE, well-formed citations.
- Citations, evidence IDs and trace IDs come from retrieval/tools only —
  `ModelOutputNormalizer` re-attaches them. Never trust model-produced IDs.
- Tool results must stay deterministic (seeded by `capstone.tools.deterministic-seed`);
  do not introduce `Instant.now()`/randomness into fixtures — anchor to intake data.
- Append-only audit: record event type + payload hash, never raw sensitive data.
- No autonomous actions: no card block, refund, transfer, report or customer contact.
- No secrets in the repo; configuration goes through `application.yml` + env vars.
- Case memory is keyed strictly by caseId — never share context between cases.
- State transitions only via `FraudCaseService.transition` (guarded by CaseStatus table).
