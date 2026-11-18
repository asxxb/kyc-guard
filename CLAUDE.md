# KYC-Guard AI Coding Guidelines

This repository follows strict development guidelines for all code generation, refactoring, and AI-assisted workflows.

## 1. Java & Architecture Standards
- **Java 21 LTS Only:** Never use preview features.
- **Dependency Injection:** Use constructor injection exclusively. Field injection (`@Autowired` on fields) is strictly forbidden.
- **Layering & Separation of Concerns:**
  - Controllers only handle HTTP/API transport and validation. No business logic in controllers.
  - Services contain business logic and transactional boundaries.
  - Repositories handle data persistence.
  - Use DTOs at system and API boundaries; domain entities must never leak directly into public APIs.
- **Clean Architecture & Packaging:** Follow `io.github.kycguard.<module>`.

## 2. Testing & Quality Requirements
- **Test Coverage:** Every new class must have unit or integration tests. JaCoCo coverage gate is enforced at >= 80% on service modules.
- **Bug Fixes:** Every bug fix requires an accompanying regression test.
- **Testing Libraries:** Use JUnit 5, AssertJ, Mockito, Testcontainers (for real databases, Kafka, Elasticsearch, Redis), and Awaitility for asynchronous events.
- **Verification:** Always execute `./mvnw -q verify` before declaring any task or phase done.
- **Formatting:** Code must adhere to Spotless formatting rules.

## 3. Data Integrity & Persistence
- **Database Migrations:** Use Flyway for every database schema change (`V<n>__description.sql`). Never edit applied migrations.
- **Financial Precision:** Model money as `NUMERIC(19,4)` with an ISO-4217 currency column. Floating-point numbers (`float`, `double`) are strictly forbidden for monetary amounts.
- **Auditability:** Audit logs must be insert-only, enforced by database triggers rejecting UPDATE and DELETE.
- **Eventual Consistency & Messaging:**
  - Outbox pattern for publishing domain events alongside database state changes.
  - Idempotent consumers tracking processed `eventId`s.
  - Exponential backoff retry policies and Dead Letter Queues (`<topic>.dlq`).

## 4. Security & Privacy
- **Synthetic Data Only:** Under no circumstances should real personal data or live sanctions feeds be used or committed.
- **No Secrets:** Never commit passwords, tokens, API keys, or certificates. Use environment variables.
- **PII Protection:** Never log personally identifiable information (PII). Mask names, addresses, and identifiers in logs; never log raw request bodies.
- **Error Handling:** Map all business and validation exceptions to RFC 7807 Problem Details (`application/problem+json`).

## 5. Workflow & Reviews
- Prefer small, incremental, reviewable diffs.
- When specifications or edge cases are ambiguous, stop and ask before assuming.
- For each implementation phase, document decisions, AI output reviews, and fixes in `docs/ai-workflow.md`.
