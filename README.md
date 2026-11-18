# KYC-Guard

[![CI](https://github.com/kycguard/kyc-guard/actions/workflows/ci.yml/badge.svg)](https://github.com/kycguard/kyc-guard/actions/workflows/ci.yml)
[![Java 21](https://img.shields.io/badge/Java-21%20LTS-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-brightgreen.svg)](https://spring.io/projects/spring-boot)

> Real-time customer screening, explainable risk scoring, and AML transaction monitoring platform built with Java 21, Spring Boot 3, Kafka, Elasticsearch, PostgreSQL, and React.

---

## 1. Architecture Overview

```
 [UI / API client]
        │  POST /api/v1/customers
        ▼
 customer-service ──► Kafka: customer.onboarded
                              │
                  ┌───────────┴─────────────┐
                  ▼                         ▼
          screening-service           ownership-service
     (Elasticsearch fuzzy match)     (links to related parties)
                  │                         │
                  ▼                         ▼
      Kafka: screening.completed   Kafka: ownership.updated
                  └───────────┬─────────────┘
                              ▼
                        risk-service
              (score + reasons + audit trail)
                              │
                              ▼
                 Kafka: risk.assessed
                              ▼
                        case-service ──► GraphQL ──► UI (analyst console)
                              ▲
   Kafka: transactions.raw ──► txn-monitor-service ──► Kafka: alerts.raised
```

---

## 2. Modules

- **`common`**: Shared event envelope, versioned domain events (`CustomerOnboardedV1`, `ScreeningCompletedV1`, `OwnershipUpdatedV1`, `RiskAssessedV1`, `TransactionV1`, `AlertRaisedV1`), RFC 7807 problem details, and serialization utilities.
- **`customer-service`**: Customer onboarding API, idempotency enforcement, state machine, and transactional outbox.
- **`screening-service`**: Real-time fuzzy watchlist (sanctions, PEP, adverse media) matching backed by Elasticsearch phonetic/n-gram search.
- **`risk-service`**: Deterministic risk scorecard, model registry, explainability breakdown, and append-only audit trail.
- **`case-service`**: Compliance analyst case workflow, SLA monitoring, and GraphQL API with subscription support.
- **`txn-monitor-service`**: Kafka Streams AML pattern detection (structuring, velocity, rapid in-out, dormant spikes).
- **`ownership-service`**: Corporate beneficial-ownership graph queries with recursive CTEs and cycle protection.
- **`data-generator`**: CLI for synthetic ground-truth data generation (watchlists, customers, transactions, ownership trees).
- **`ui`**: React + TypeScript analyst console.

---

## 3. Quick Start

### Prerequisites
- Java 21 LTS
- Docker and Docker Compose
- Node.js 20+ (for UI)

### Start Infrastructure
```bash
docker compose -f infra/docker-compose.yml up -d
```

### Build & Run Tests
```bash
./mvnw clean verify
```
