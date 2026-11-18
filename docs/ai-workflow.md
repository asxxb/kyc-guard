# AI-Assisted Engineering Workflow Log

This document records the human-in-the-loop review, validation, and hardening process for every phase of KYC-Guard developed using AI code generation tools.

## Phase Log

| Phase | What the AI generated | What I reviewed | Problems found and fixed | Prompt improvements |
|---|---|---|---|---|
| **Phase 0: Foundation** | Maven multi-module parent POM, module POMs, `infra/docker-compose.yml`, GitHub Actions `ci.yml`, `CLAUDE.md`, Event Envelope and 6 domain event records with Jackson tests. | JaCoCo rule enforcement, JDK 21 compatibility, Maven wrapper configuration, Docker Compose service configurations and KRaft cluster parameters, Spotless formatting rules, Event schema backward-compatibility and JSON serialization correctness. | 1. Maven wrapper defaulted to host JDK 17 when `JAVA_HOME` was not explicitly set; patched `mvnw` launcher script to automatically select installed JDK 21.<br>2. Verified Docker Compose KRaft environment variables to ensure Kafka 3.7 boots without ZooKeeper and without cluster format race conditions.<br>3. Enforced `BigDecimal` for monetary amounts across transaction events to prevent IEEE-754 floating-point inaccuracies.<br>4. Fixed JaCoCo test execution configuration to properly generate reports across submodules. | Explicitly specify JDK 21 path fallback and define strict immutable records with explicit null-safe collection defaults for event records. |
| **Phase 1: customer-service** | `customer-service` REST API (`/api/v1/customers`), Flyway V1 schema migration, `IdempotencyService` with SHA-256 conflict detection, `CustomerStateMachine`, transactional outbox entity and `OutboxRelay`, RFC 7807 error handling, OpenAPI docs, unit tests, and Testcontainers integration tests with real PostgreSQL and Kafka. | Idempotency replay vs. conflict semantics, NFKC Unicode name normalization, ISO-3166 alpha-2 country validation, state machine transition enforcement and terminal state immutability, outbox publishing atomicity, and Testcontainers Docker environment compatibility. | 1. `TestRestTemplate` failed on `PATCH /status` with `ProtocolException: Invalid HTTP method: PATCH`; configured `JdkClientHttpRequestFactory` in the test client.<br>2. Testcontainers Ryuk failed to mount Colima socket path inside VM; configured `ryuk.disabled=true` and Docker API version 1.44.<br>3. Spotless detected unused entity imports in mapper; cleaned and enforced formatting.<br>4. Confirmed JaCoCo test coverage achieved 82% (meeting the >= 80% service gate). | Specify `JdkClientHttpRequestFactory` for integration test clients invoking HTTP PATCH, and configure Colima socket path and Ryuk compatibility flags. |
| **Phase 2: data-generator** | *(Pending)* | | | |
| **Phase 3: screening-service** | *(Pending)* | | | |
| **Phase 4: risk-service** | *(Pending)* | | | |
| **Phase 5: case-service** | *(Pending)* | | | |
| **Phase 6: txn-monitor-service** | *(Pending)* | | | |
| **Phase 7: ownership-service** | *(Pending)* | | | |
| **Phase 8: UI** | *(Pending)* | | | |
| **Phase 9: Observability & Deploy** | *(Pending)* | | | |
| **Phase 10: Hardening & Proof** | *(Pending)* | | | |
