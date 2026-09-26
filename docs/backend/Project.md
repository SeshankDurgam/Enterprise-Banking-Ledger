---
type: prd
stack: [java, core-java, spring-boot, spring-security, spring-data-jpa, hibernate, rest, oracle, plsql, transactions, acid, concurrency, redis, kafka, jms, docker, testcontainers, git, cicd]
repo:
status: planned
created: 2026-05-02
updated: 2026-05-26
tags: [prd, project, java, banking, fintech, ledger]
---

# Product Requirements Document: Mini Core Banking Ledger

## 1. Overview

Mini Core Banking Ledger is a compact banking-style backend system that demonstrates double-entry accounting, ACID transaction handling, auditability, concurrency-safe transfers, Oracle-oriented persistence, reconciliation, and operational investigation workflows.

The product is designed as a portfolio-grade Spring Boot backend for banking, fintech, payments, and enterprise Java roles where financial correctness, traceability, and transactional integrity matter more than feature volume.

## 2. Problem Statement

Financial systems must move money without losing consistency under retries, concurrent requests, partial failures, and operational corrections. A simple CRUD-style account balance service does not demonstrate the invariants expected from banking software, such as balanced postings, immutable ledgers, idempotent commands, auditable reversals, and reconciliation against external settlement data.

This project addresses that gap by building a small but serious ledger and transaction engine with production-style safeguards.

## 3. Goals

- Implement a double-entry ledger where every posted financial transaction creates balanced debit and credit postings.
- Guarantee atomic posting, reversal, transfer, audit, balance, idempotency, and outbox writes through explicit transaction boundaries.
- Prevent duplicate money movement through idempotency keys and replay-safe transfer APIs.
- Preserve correct balances under concurrent transfers and overdraft attempts.
- Support operational reversal, adjustment, reconciliation, audit lookup, and reporting workflows.
- Demonstrate secure API design with role-based access, ownership checks, and sensitive-operation authorization.
- Provide portfolio deliverables that make design decisions, data model, API behavior, test coverage, and operational tradeoffs reviewable.

## 4. Non-Goals

- Build a full retail banking platform with cards, loans, interest accrual, branch operations, or customer onboarding.
- Integrate with real payment networks, ISO-8583 switches, core banking hosts, or external settlement providers.
- Provide a production-ready mobile or web customer application.
- Optimize for high-frequency trading or extreme low-latency throughput.
- Replace regulatory, compliance, fraud, AML, or KYC systems.

## 5. Target Users

| User | Need |
| --- | --- |
| Customer | View account balances, recent transactions, and transfer money safely. |
| Teller | Inspect customer account activity and support operational workflows. |
| Operator | Reverse transactions, submit adjustments, investigate failures, and review reconciliation results. |
| Auditor | Query immutable ledger records, audit events, and daily balance reports. |
| Service Client | Submit transfer and posting commands with idempotency and correlation IDs. |
| Engineer | Demonstrate banking backend design, test coverage, observability, and operational reasoning. |

## 6. Success Metrics

- All posted journal entries have total debits equal to total credits.
- Duplicate transfer requests with the same idempotency key do not create duplicate ledger postings.
- Concurrent transfer tests preserve correct account balances and consistently reject overdrafts.
- Reversal attempts are linked to original transactions and duplicate reversals are rejected.
- Audit events exist for financial operations, protected operations, and reconciliation workflows.
- Integration tests verify rollback behavior for failed posting and transfer flows.
- OpenAPI documentation, local run steps, database migrations, and architecture docs are complete enough for reviewer setup.

## 7. Scope

### 7.1 In Scope

- Account management, account status, account balance views, and account limits.
- Ledger transaction creation, journal entries, postings, and immutable posted records.
- Internal account-to-account transfers with validation, idempotency, and concurrency protection.
- Full transaction reversals and operational adjustment entries with required audit reasons.
- Settlement batch import simulation, mismatch detection, and reconciliation result lookup.
- Protected operations APIs for ledger investigation, reversal requests, reconciliation, reporting, and audit lookup.
- Oracle-compatible schema design, constraints, migrations, and PL-SQL-style reporting examples.
- Kafka or JMS event publishing through a transactional outbox.
- Unit, integration, concurrency, API, authorization, and error-response tests.

### 7.2 Optional Scope

- Reporting service for SQL and PL-SQL-style operational reports.
- Redis-backed caching for read-heavy account or reporting views, if cache invalidation remains simple and safe.
- Additional validation strategy examples such as fee calculation, transfer policy composition, or transaction search specifications.

## 8. Product Requirements

### PRD-001: Double-Entry Ledger

Every financial transaction must produce one or more journal entries with balanced debit and credit postings.

Acceptance criteria:
- The system rejects any journal entry whose total debit amount does not equal total credit amount.
- Posted ledger records cannot be destructively updated.
- Corrections use reversal or adjustment entries.
- Posting amounts use integer minor units or `BigDecimal` with strict scale rules.
- Floating-point arithmetic is not used for money.

### PRD-002: ACID Transaction Handling

Posting, transfer, reversal, audit, idempotency, cached balance, and outbox writes must commit atomically.

Acceptance criteria:
- Transfer creation commits all related rows or rolls back all related rows.
- Reversal creation commits all related rows or rolls back all related rows.
- Database constraints enforce uniqueness, referential integrity, valid statuses, valid directions, and positive amounts.
- Isolation-level and locking decisions are documented.
- Tests verify rollback behavior when posting or publishing preparation fails.

### PRD-003: Concurrency-Safe Transfers

The transfer engine must preserve account balance correctness when multiple requests target the same accounts.

Acceptance criteria:
- Concurrent debit requests cannot create lost updates.
- Overdraft attempts are rejected consistently under concurrent load.
- The implementation demonstrates optimistic locking and/or pessimistic row locks.
- Tests simulate concurrent transfer requests against the same source account.

### PRD-004: Idempotent Internal Transfer API

Clients must provide idempotency keys when creating transfers so retries are replay-safe.

Acceptance criteria:
- `POST /api/v1/transfers` requires an idempotency key.
- Replaying the same request with the same key returns the original result.
- Replaying the same key with a conflicting payload is rejected.
- Duplicate requests do not create duplicate postings, audit events, or outbox events.
- Transfer validation checks account status, currency, limits, and available balance.

### PRD-005: Reversal And Adjustment Flows

Operators must be able to reverse posted transactions and submit adjustment entries without mutating original ledger records.

Acceptance criteria:
- A posted transaction can be fully reversed once.
- Reversal entries link back to the original transaction.
- Duplicate reversals are rejected.
- Reversal and adjustment requests require a reason.
- Reversal and adjustment operations emit audit events and ledger events.

### PRD-006: Auditability And Traceability

The system must preserve a useful audit trail for financial, operational, and reconciliation workflows.

Acceptance criteria:
- Audit events record actor, role, channel, operation, entity, timestamp, and correlation ID.
- Sensitive data is masked in logs and audit payloads.
- Protected audit query APIs allow investigation by entity type, entity ID, and time range.
- Logs include correlation IDs for financial workflows.

### PRD-007: Secure Role-Based APIs

The API must protect customer, teller, operator, auditor, service, and administrative workflows.

Acceptance criteria:
- Spring Security protects all non-public APIs.
- Roles include `CUSTOMER`, `TELLER`, `AUDITOR`, `OPS_ADMIN`, and `SERVICE`.
- Customer account APIs enforce account ownership.
- Reversal, adjustment, reconciliation, reporting, and outbox operations require privileged roles.
- Method-level authorization protects sensitive commands.

### PRD-008: Reconciliation

The system must support settlement import simulation and mismatch detection between internal ledger records and external batch data.

Acceptance criteria:
- Operators can create or import settlement batches.
- The system compares settlement items with internal ledger records.
- Mismatches are persisted with reason codes and investigation status.
- Reconciliation mismatch events are published through the outbox.
- Operators can query batch and mismatch results.

### PRD-009: Messaging And Reliable Event Publishing

Financial and operational events must be published without coupling database commits to broker availability.

Acceptance criteria:
- The system writes outbox records in the same transaction as business data.
- A publisher sends events to Kafka or JMS after commit.
- Event types include `LedgerTransactionPosted`, `LedgerTransactionReversed`, `AccountBalanceChanged`, and `ReconciliationMismatchFound`.
- Failed event publishing supports retry, dead-letter handling, and replay.
- Outbox lag and failure metrics are exposed.

### PRD-010: Reporting And Investigation

Auditors and operators must be able to inspect balances, transactions, reconciliation state, and audit activity.

Acceptance criteria:
- Account transaction history supports date range and pagination.
- Operations APIs support ledger transaction lookup by transaction ID.
- Audit APIs support entity and date filters.
- At least one Oracle-compatible report is included, such as daily trial balance or reconciliation summary.

## 9. User Stories

| ID | Story | Priority |
| --- | --- | --- |
| CB01 | As a customer, I want to view my account balance and recent transactions. | Must |
| CB02 | As a customer, I want to transfer money to another account safely. | Must |
| CB03 | As the system, I want every posted transfer to produce balanced debit and credit postings. | Must |
| CB04 | As the system, I want duplicate transfer requests to return the original result without double posting. | Must |
| CB05 | As the system, I want concurrent transfers to preserve correct account balances. | Must |
| CB06 | As an operator, I want to reverse a transaction with a required reason and audit trail. | Must |
| CB07 | As an auditor, I want to query immutable ledger entries and verify daily balances. | Must |
| CB08 | As the reconciliation process, I want to compare internal ledger totals with an external settlement batch and flag mismatches. | Should |
| CB09 | As an engineer, I want logs, metrics, and correlation IDs for every financial workflow. | Should |

## 10. Core Domain Model

### 10.1 Main Concepts

- `Customer`
- `Account`
- `LedgerTransaction`
- `JournalEntry`
- `Posting`
- `TransferRequest`
- `Reversal`
- `SettlementBatch`
- `ReconciliationResult`
- `AuditEvent`
- `IdempotencyRecord`
- `OutboxEvent`

### 10.2 Account Types

- `CURRENT`
- `SAVINGS`
- `WALLET`
- `SUSPENSE`
- `FEE_INCOME`
- `CLEARING`

### 10.3 Transaction Statuses

- `PENDING`
- `POSTED`
- `REJECTED`
- `REVERSED`
- `FAILED`

## 11. API Requirements

### 11.1 Customer And Account APIs

- `GET /api/v1/accounts/{accountId}`
- `GET /api/v1/accounts/{accountId}/balance`
- `GET /api/v1/accounts/{accountId}/transactions?from=&to=&page=&size=`

### 11.2 Transfer APIs

- `POST /api/v1/transfers`
- `GET /api/v1/transfers/{transferId}`
- `POST /api/v1/transfers/{transferId}/reverse`

### 11.3 Operations And Audit APIs

- `GET /api/v1/ops/ledger/transactions/{transactionId}`
- `GET /api/v1/ops/reconciliation/batches/{batchId}`
- `POST /api/v1/ops/reconciliation/batches`
- `GET /api/v1/audit/events?entityType=&entityId=&from=&to=`

## 12. Data Requirements

### 12.1 Required Tables

- `customers`
- `accounts`
- `ledger_transactions`
- `journal_entries`
- `postings`
- `transfer_requests`
- `reversals`
- `settlement_batches`
- `settlement_items`
- `reconciliation_results`
- `audit_events`
- `idempotency_records`
- `outbox_events`

### 12.2 Required Constraints

- Unique account number.
- Unique idempotency key per operation scope.
- Unique reversal per original transaction.
- Foreign keys from postings to journal entries and accounts.
- Check constraints for debit or credit direction and positive amounts.
- Version column for optimistic locking where used.

### 12.3 Required Indexes

- Account transaction history by account and posted time.
- Transaction lookup by external reference.
- Idempotency key lookup.
- Reconciliation item lookup by settlement reference.
- Outbox lookup by status and next retry time.

## 13. Non-Functional Requirements

- Correctness takes priority over throughput for financial posting.
- Posted financial records are append-only.
- DTOs, domain models, and persistence entities remain separated.
- API errors use structured business and security error codes.
- Sensitive values are not written to application logs.
- Workers and consumers support graceful shutdown.
- Metrics cover transaction volume, failed transfers, reversal count, reconciliation mismatch count, outbox lag, and API latency.
- Local development must run through Docker Compose with Oracle and Kafka.
- CI must run unit tests, integration tests, security checks, and coverage reporting.

## 14. Reporting Requirements

At least one Oracle-compatible report must be delivered. Candidate reports include:

- Daily trial balance by currency.
- Account statement summary by period.
- Reconciliation mismatch report.
- Suspense account aging report.
- Top failed transfer reasons by day.

## 15. Architecture And Design Expectations

- Domain model enforces journal entry and posting invariants.
- Spring Data JPA repositories persist aggregate state and query views.
- Factory methods create valid ledger transactions.
- Strategy pattern can be used for fee calculation or transfer validation.
- Specification pattern can support transaction search filters.
- Outbox pattern provides reliable financial event publishing.
- Command pattern can model reversal and adjustment operations.

## 16. Milestones

| Phase | Deliverable |
| --- | --- |
| 1 | Account and ledger schema with Flyway migrations. |
| 2 | Money value object and ledger domain invariants. |
| 3 | Transfer posting with balanced debit and credit postings. |
| 4 | Idempotency records and duplicate request handling. |
| 5 | Reversal flow and audit trail. |
| 6 | Concurrency-safe balance checks and concurrent tests. |
| 7 | Reconciliation batch import and mismatch detection. |
| 8 | Spring Security roles and protected operations APIs. |
| 9 | Outbox events with Kafka or JMS publishing. |
| 10 | Docker Compose, CI, documentation, reports, and ADRs. |

## 17. Deliverables

- Architecture diagram.
- ERD diagram.
- README with local run steps.
- OpenAPI specification.
- Docker Compose setup.
- Database migration scripts.
- PL-SQL or Oracle-compatible reporting examples.
- CI/CD pipeline.
- Test report and coverage summary.
- Concurrent transfer test results.
- Reconciliation example with mismatch report.
- Incident write-up covering duplicate request, overdraft race condition, or failed reversal investigation.
- ADRs for double-entry model, money representation, transaction isolation, locking strategy, immutable ledger and reversal model, idempotency design, and outbox event publishing strategy.

## 18. Dependencies

- Java 21.
- Spring Boot 3.5.x.
- Spring Security and OAuth2 Resource Server.
- Spring Data JPA and Hibernate.
- Oracle Database Free 23c.
- Flyway.
- Kafka or JMS.
- Docker Compose.
- Testcontainers.
- GitHub Actions.

## 19. Risks And Mitigations

| Risk | Mitigation |
| --- | --- |
| Incorrect money arithmetic creates balance drift. | Use integer minor units or strict-scale `BigDecimal`; reject invalid scales; test rounding and equality rules. |
| Concurrent transfers cause lost updates. | Use documented locking strategy, database constraints, and concurrent integration tests. |
| Duplicate retries move money twice. | Require idempotency keys and persist request fingerprints with replay responses. |
| Reversals mutate original records or lose traceability. | Keep posted records immutable and create linked reversal entries with audit reasons. |
| Event publishing fails after financial commit. | Use transactional outbox with retry, dead-letter handling, and replay operations. |
| Sensitive data leaks through logs or audits. | Mask sensitive fields, centralize structured logging, and test representative payloads. |

## 20. Open Questions

- Should the first implementation use pessimistic locking only, or demonstrate both pessimistic and optimistic locking?
- Should reporting be implemented as SQL files, database views, stored procedures, or application-level query endpoints?
- Should Kafka be mandatory in the first release, or can the outbox initially expose pending events through protected APIs?
- Which settlement file format should the reconciliation simulation use?

## 21. Interview And Portfolio Talking Points

- Why financial systems use double-entry accounting.
- Why posted ledger entries should be immutable.
- How reversals differ from deleting or editing records.
- How ACID transactions protect multi-posting financial operations.
- How isolation levels affect concurrent transfers.
- How optimistic and pessimistic locking differ.
- How idempotency prevents duplicate money movement.
- How database constraints make defects fail safely.
- How internal ledger state is reconciled with external settlement data.
- How audit trails support compliance, debugging, and operational investigation.
