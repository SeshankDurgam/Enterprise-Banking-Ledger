# Hands-On Interview Preparation Curriculum

This curriculum is designed around the Mini Core Banking Ledger project. It prepares a candidate to discuss, extend, debug, and defend the system in backend, fintech, Java, Spring Boot, SQL, distributed systems, and data-structures-and-algorithms interviews.

## Target Outcomes

By the end of this curriculum, you should be able to:

- Explain the project architecture from REST API to database commit.
- Defend double-entry ledger invariants, transaction boundaries, locking, idempotency, audit, reconciliation, and outbox design decisions.
- Implement focused changes in the codebase under interview-style time limits.
- Write unit, integration, concurrency, repository, and controller tests.
- Solve DSA problems that map to real project scenarios instead of practicing them in isolation.
- Discuss operational failure modes, tradeoffs, and observability clearly.

## Prerequisites

- Java 21 fundamentals, records, collections, streams, exceptions, and time APIs.
- Spring Boot basics, dependency injection, REST controllers, validation, and profiles.
- SQL joins, indexes, constraints, transactions, isolation levels, and aggregate queries.
- Basic Git workflow and ability to explain a diff.

Recommended project reading before starting:

- `README.md`
- `docs/backend/CoreBusinessLogic.md`
- `docs/backend/API.md`
- `docs/backend/DatabaseDesign.md`
- `docs/backend/TransferConcurrencyLockingStrategy.md`
- `docs/backend/TransferTransactionBoundaryAudit.md`
- `docs/adr/ADR-DoubleEntryAmountIdempotency.md`
- `docs/adr/ADR-Phase5-TransactionIsolationAndLocking.md`
- `docs/adr/ADR-Phase9-OutboxKafkaPublishing.md`
- `reports/README.md`

## Curriculum Structure

Use this as a 6-week plan. Each week has a codebase focus, DSA focus, hands-on exercises, and interview prompts.

Expected weekly effort:

- 4 to 6 hours reading and tracing code.
- 4 to 6 hours implementing exercises.
- 3 to 4 hours DSA practice.
- 1 to 2 mock interview sessions.

## Week 1: Project Orientation And Core Java

### Codebase Focus

Understand the package structure and how requests flow through the application.

Study:

- `banking-ledger-api/src/main/java/dev/kavrin/banking_ledger/BankingLedgerApplication.java`
- `account/api`, `account/application`, `account/domain`, `account/persistence`
- `shared/error`
- `shared/money`
- `docs/diagrams/architecture.mmd`

Key concepts:

- Controller, DTO, command, use case, repository, and entity boundaries.
- Domain policies versus application orchestration.
- Structured exception handling and API error responses.
- Money representation in integer minor units.

### DSA Focus

Practice Java fundamentals through collections-heavy problems.

Problems:

- Implement an LRU cache using `LinkedHashMap`.
- Group account transactions by account id and transaction type.
- Deduplicate account numbers while preserving insertion order.
- Find the first duplicate external reference in a list of requests.
- Merge two sorted transaction histories by timestamp.

Project mapping:

- Deduplication maps to idempotency and external reference handling.
- Ordering maps to transaction history pagination.
- Grouping maps to reporting and audit query responses.

### Hands-On Exercises

- Trace `CreateAccountUseCase` from controller input to persisted entity and response.
- Add one validation rule to account creation and cover it with tests.
- Add one new `ApiErrorCode` and use it in a focused failure path.
- Write a short architecture note explaining why DTOs, commands, and entities are separate.

### Interview Prompts

- Walk me through `POST /accounts` from HTTP request to database write.
- Why should money not use `double`?
- What is the difference between a domain policy and an application service?
- Where should validation live in this project?
- How would you keep API errors stable for clients?

## Week 2: Double-Entry Ledger And Invariants

### Codebase Focus

Understand the ledger posting engine and the financial invariants it enforces.

Study:

- `ledger/domain/model`
- `ledger/domain/policy/DoubleEntryPostingPolicy.java`
- `ledger/domain/factory/JournalEntryFactory.java`
- `ledger/application/service/PostLedgerTransactionUseCase.java`
- `ledger/application/service/AccountBalanceUpdater.java`
- `ledger/application/command`
- `ledger/domain/policy/DoubleEntryPostingPolicyTest.java`
- `ledger/application/service/PostLedgerTransactionUseCaseIntegrationTests.java`

Key concepts:

- Ledger transaction, journal entry, and posting relationships.
- Balanced debit and credit totals.
- Append-only financial records.
- Cached account balances as derived views.
- Rejection of invalid mixed-currency or unbalanced postings.

### DSA Focus

Practice invariant checking and graph-style modeling.

Problems:

- Validate whether a list of posting lines is balanced by currency.
- Detect cycles in a reversal graph.
- Build an adjacency list from transfers between accounts.
- Find connected components in accounts linked by transfers.
- Compute net account movement from a stream of debit and credit postings.

Project mapping:

- Balance validation maps directly to `DoubleEntryPostingPolicy`.
- Graph traversal maps to investigation workflows and reversal relationships.
- Net movement maps to account statements and reports.

### Hands-On Exercises

- Implement a pure Java function that validates posting lines before persistence.
- Add a test case for mixed currencies or missing debit/credit lines.
- Write a failing test for an unbalanced journal, then make it pass if needed.
- Explain how `AccountBalanceUpdater` handles debit and credit directions.

### Interview Prompts

- Why is double-entry safer than directly updating balances?
- If the app crashes after inserting postings but before updating balances, what protects correctness?
- What database constraints should backstop ledger invariants?
- How would you investigate a customer balance mismatch?
- What is the source of truth: postings or cached balances?

## Week 3: Transfers, Idempotency, And Concurrency

### Codebase Focus

Understand safe money movement under retries and concurrent requests.

Study:

- `transfer/application/service/CreateTransferUseCase.java`
- `transfer/domain/policy/TransferValidationPolicy.java`
- `transfer/persistence/TransferRequestRepository.java`
- `idempotency/application/service/IdempotencyService.java`
- `idempotency/application/service/TransferRequestHasher.java`
- `account/application/service/LockedAccountLoader.java`
- `transfer/application/service/CreateTransferUseCaseTest.java`
- `transfer/application/service/CreateTransferUseCaseIntegrationTests.java`
- `account/application/service/LockedAccountLoaderTest.java`
- `docs/backend/TransferConcurrencyLockingStrategy.md`

Key concepts:

- Idempotency key validation.
- Request hash comparison for retry safety.
- Pessimistic account locking.
- Deterministic lock ordering.
- Overdraft rejection and rollback.
- Transactional boundaries for transfer, ledger, audit, and outbox writes.

### DSA Focus

Practice hashing, lock ordering, and concurrency-adjacent reasoning.

Problems:

- Create a canonical hash for a transfer request object.
- Sort two account ids deterministically before locking.
- Detect conflicting requests sharing the same idempotency key.
- Simulate concurrent withdrawals and prove final balance correctness.
- Design a bounded retry queue for failed transfer attempts.

Project mapping:

- Canonical hashing maps to `TransferRequestHasher`.
- Deterministic sorting maps to deadlock prevention.
- Conflict detection maps to idempotency replay behavior.
- Queue design maps to retry and outbox processing.

### Hands-On Exercises

- Add a test that verifies the same idempotency key with a different payload is rejected.
- Add or review a concurrency test for multiple transfers from the same source account.
- Explain why account locking should use stable ordering.
- Build a small command-line or unit-test simulation of concurrent balance updates without locks, then fix it.

### Interview Prompts

- What makes an API idempotent?
- What should happen when the same idempotency key is reused with a different payload?
- How do pessimistic locks differ from optimistic locks?
- How can two-account transfers deadlock?
- Which operations must commit in the same database transaction?

## Week 4: Reversal, Adjustment, Audit, And Security

### Codebase Focus

Understand protected operational workflows and traceability.

Study:

- `reversal/application/service/ReverseTransferUseCase.java`
- `reversal/domain/policy/ReversalValidationPolicy.java`
- `adjustment/application/service/CreateAdjustmentUseCase.java`
- `audit/application/service/AuditEventWriter.java`
- `audit/application/service/AuditEventQueryUseCase.java`
- `security/config/SecurityConfig.java`
- `security/auth`
- `security/application/AccountAccessAuthorizer.java`
- `reversal/application/service/ReverseTransferUseCaseIntegrationTests.java`
- `adjustment/application/service/CreateAdjustmentUseCaseIntegrationTests.java`
- `security/auth/Phase7SecurityIntegrationTest.java`

Key concepts:

- Immutable correction through reversal and adjustment entries.
- Required reasons for sensitive financial operations.
- Audit event payloads and correlation ids.
- Role-based authorization.
- Account ownership checks.
- Secure error handling.

### DSA Focus

Practice search, filtering, and rule evaluation.

Problems:

- Filter audit events by actor, entity type, and time range.
- Find all reversal chains for a transaction id.
- Implement interval overlap checks for audit query ranges.
- Evaluate role permissions from a role-to-actions map.
- Find the shortest path from an audit event to a ledger transaction through related ids.

Project mapping:

- Filtering maps to audit search.
- Reversal chains map to ledger investigation.
- Permission maps map to Spring Security authorization decisions.
- Shortest path maps to operational traceability.

### Hands-On Exercises

- Add a new audit query test for time range filtering.
- Add a negative security test for a customer trying to access another customer's account.
- Explain the difference between authentication, authorization, and ownership checks.
- Write a short runbook for investigating a reversed transfer.

### Interview Prompts

- Why should reversals not mutate the original ledger transaction?
- What belongs in an audit payload?
- How do you avoid leaking sensitive information in logs or errors?
- Why use method-level authorization in addition to URL security?
- How would you prove an operator reversed the right transaction?

## Week 5: Reconciliation, SQL Reporting, And Data Modeling

### Codebase Focus

Understand how internal ledger records are compared with external settlement data.

Study:

- `reconciliation/application/service`
- `reconciliation/domain/policy/SettlementBatchValidationPolicy.java`
- `reconciliation/persistence`
- `reports/sql`
- `reports/plsql`
- `banking-ledger-api/src/main/resources/db/migration`
- `docs/backend/DatabaseDesign.md`
- `docs/adr/ADR-Phase10-Reconciliation.md`

Key concepts:

- Settlement batches and settlement items.
- Match, mismatch, duplicate, and missing item detection.
- Relational constraints.
- SQL aggregation and reconciliation reports.
- Index choices for investigation queries.

### DSA Focus

Practice matching, diffing, sorting, and aggregation.

Problems:

- Compare internal transactions and settlement items by external reference.
- Find missing, extra, duplicate, and amount-mismatched records.
- Compute daily trial balance from posting lines.
- Group reconciliation mismatches by reason code.
- Find top K failed transfer reasons.

Project mapping:

- Diffing maps to `ReconciliationMatcher`.
- Aggregation maps to trial balance and account statement reports.
- Top K maps to operational reporting.
- Duplicate detection maps to settlement import validation.

### Hands-On Exercises

- Implement a standalone reconciliation matcher over two in-memory lists.
- Add a unit test for duplicate settlement references.
- Explain one SQL report line by line.
- Propose an index for a slow audit or reconciliation query and defend it.

### Interview Prompts

- How do you reconcile internal ledger data with an external provider?
- What should happen when settlement has a transaction the ledger does not?
- How would you model reconciliation statuses?
- When would you use SQL aggregation instead of Java aggregation?
- What indexes would support account transaction history?

## Week 6: Outbox, Kafka, Observability, And System Design

### Codebase Focus

Understand reliable event publishing and operational recovery.

Study:

- `outbox/application/service/OutboxWriterService.java`
- `outbox/application/service/OutboxPublisherWorker.java`
- `outbox/application/service/KafkaOutboxEventPublisher.java`
- `outbox/application/service/RequeueOutboxEventUseCase.java`
- `outbox/domain/model`
- `outbox/application/service/OutboxPublisherWorkerTest.java`
- `outbox/application/service/RequeueOutboxEventUseCaseTest.java`
- `docs/diagrams/outbox-publishing.mmd`
- `docs/adr/ADR-Phase9-OutboxKafkaPublishing.md`

Key concepts:

- Transactional outbox pattern.
- At-least-once delivery.
- Retry and dead-letter handling.
- Event schema versioning.
- Outbox lag and operational metrics.
- Recovery after broker outage.

### DSA Focus

Practice queues, heaps, backoff, and scheduling.

Problems:

- Implement a priority queue for outbox events by next attempt time.
- Design exponential backoff with max retry limits.
- Batch pending events without starving older records.
- Track moving average publish latency.
- Deduplicate delivered event ids in a bounded memory cache.

Project mapping:

- Priority queues map to retry scheduling.
- Batching maps to publisher throughput.
- Bounded deduplication maps to consumer idempotency.
- Metrics maps to outbox observability.

### Hands-On Exercises

- Add a test for failed publish retry behavior.
- Explain why the project should not publish to Kafka inside the same database transaction.
- Add or review a metric that exposes failed or delayed outbox events.
- Design a dead-letter requeue flow and its authorization rules.

### Interview Prompts

- What problem does the transactional outbox solve?
- Is outbox delivery exactly once or at least once?
- How should consumers handle duplicate events?
- How do you recover after Kafka is down for 30 minutes?
- What metrics would you page on?

## DSA Practice Bank

Use these problems throughout the plan. Implement them in Java first, then explain complexity and edge cases.

| Topic | Project Scenario | Practice Problem | Target Complexity |
| --- | --- | --- | --- |
| Hash map | Idempotency lookup | Detect duplicate idempotency keys and conflicting payloads | `O(n)` time, `O(n)` space |
| Sorting | Deadlock prevention | Sort account ids before acquiring locks | `O(n log n)` or constant for two ids |
| Two pointers | Statement merge | Merge two sorted account transaction streams | `O(n + m)` time |
| Prefix sums | Running balance | Compute balance after each posting | `O(n)` time |
| Heap | Outbox retries | Process next due retry event | `O(log n)` per operation |
| Graph DFS | Investigation links | Traverse transaction, reversal, audit, and outbox references | `O(V + E)` |
| BFS | Shortest trace path | Find shortest path from customer complaint to ledger posting | `O(V + E)` |
| Union-find | Related accounts | Group accounts connected by transfer activity | Near `O(1)` amortized |
| Sliding window | Fraud signal | Find accounts exceeding N transfers in a rolling time window | `O(n)` after sorting |
| Top K | Failure reporting | Find top failed transfer reasons | `O(n log k)` |
| Intervals | Audit filters | Validate and merge time ranges | `O(n log n)` |
| Dynamic programming | Fee rules | Compute best fee discount path under constraints | Depends on state model |

## System Design Drills

Practice each drill as a 30 to 45 minute whiteboard session.

### Drill 1: Design A Transfer API

Cover:

- Request and response schema.
- Idempotency key strategy.
- Validation and account ownership.
- Locking and isolation.
- Ledger posting.
- Audit and outbox writes.
- Failure responses.

Expected answer:

- Use a single database transaction for transfer request, ledger postings, balance updates, idempotency record, audit event, and outbox rows.
- Lock affected accounts in deterministic order.
- Reject conflicting idempotency replay.
- Publish events after commit through outbox polling.

### Drill 2: Design Reconciliation Import

Cover:

- Batch and item schema.
- Validation rules.
- Matching algorithm.
- Mismatch persistence.
- Audit and event publishing.
- Reporting queries.

Expected answer:

- Store imported settlement data separately from internal ledger data.
- Match by stable external reference plus amount and currency.
- Persist mismatch reason codes for investigation.
- Avoid destructive updates to ledger records.

### Drill 3: Design Outbox Publishing

Cover:

- Event table schema.
- Polling strategy.
- Retry and dead-letter behavior.
- Schema versioning.
- Consumer idempotency.
- Metrics and alerts.

Expected answer:

- Write outbox events in the same transaction as business state.
- Publish asynchronously and mark status after broker acknowledgement.
- Expect duplicate delivery and design consumers accordingly.
- Track lag, failure counts, retry counts, and dead-letter volume.

## Mock Interview Rotation

Run these sessions after each week.

### Backend Coding Mock

Timebox: 60 minutes.

Task examples:

- Add a validation rule to `TransferValidationPolicy`.
- Implement a new audit query filter.
- Add a report query for failed transfer reasons.
- Add a retry-state transition to outbox publishing.

Evaluation:

- Correctness.
- Test coverage.
- Clear boundaries.
- Small, reviewable diff.
- Explanation of tradeoffs.

### DSA Mock

Timebox: 45 minutes.

Task examples:

- Reconcile two lists of transactions.
- Compute running account balances.
- Detect duplicate settlement references.
- Schedule retry events with a heap.
- Traverse linked ledger investigation records.

Evaluation:

- Clarifies input and edge cases.
- States complexity.
- Uses appropriate data structures.
- Writes readable Java.
- Tests at least normal, empty, duplicate, and invalid cases.

### System Design Mock

Timebox: 45 minutes.

Task examples:

- Design idempotent transfer processing.
- Design audit search for high-volume data.
- Design outbox recovery after broker outage.
- Design reconciliation for nightly settlement files.

Evaluation:

- Defines requirements and non-goals.
- Identifies invariants.
- Handles failure modes.
- Chooses clear data models.
- Explains operational metrics.

## Project Storytelling Guide

Use this structure when presenting the project in interviews:

1. Problem: Financial systems cannot rely on simple balance updates because retries, concurrency, and failures can create inconsistent money movement.
2. Core idea: This project records money movement as immutable double-entry ledger postings, with balances treated as cached views.
3. Safety mechanisms: It uses transaction boundaries, constraints, account locking, idempotency keys, audit records, and outbox events.
4. Operational workflows: It supports investigation, reversal, adjustment, reconciliation, and reporting.
5. Tradeoffs: It favors correctness, traceability, and explicit workflows over raw throughput and feature breadth.

Example pitch:

```text
This is a Spring Boot banking ledger that models transfers as double-entry postings instead of direct balance updates. The interesting parts are the correctness boundaries: idempotency for retries, deterministic account locking for concurrent transfers, immutable ledger records for auditability, and a transactional outbox so external event publishing cannot break database commits. I can walk through the transfer flow, the ledger posting policy, or how reconciliation finds mismatches between internal and settlement records.
```

## Weekly Deliverables

| Week | Deliverable |
| --- | --- |
| 1 | Architecture walkthrough notes and one small validation/test change |
| 2 | Ledger invariant notes and additional posting policy test |
| 3 | Idempotency and concurrency explanation with at least one retry/conflict test |
| 4 | Security and audit investigation runbook |
| 5 | Reconciliation matcher exercise and SQL report explanation |
| 6 | Outbox failure-mode design notes and retry/dead-letter test |

## Final Capstone

Complete one of these capstone tasks in 3 to 5 hours and prepare a 10 minute explanation.

### Option A: Transfer Hardening

- Add or improve a transfer validation rule.
- Add a conflict/idempotency test.
- Add a concurrency or rollback test.
- Explain how the change preserves ledger correctness.

### Option B: Reconciliation Enhancement

- Add a new mismatch reason or report.
- Cover duplicate, missing, amount mismatch, and currency mismatch cases.
- Explain data model and query tradeoffs.

### Option C: Outbox Reliability

- Improve retry or requeue behavior.
- Add tests for status transitions.
- Explain at-least-once delivery and consumer idempotency.

### Option D: Audit Investigation

- Add a query filter or response field.
- Add tests for authorization and filtering.
- Explain how an auditor traces an operation end to end.

## Readiness Checklist

You are interview-ready when you can answer these without notes:

- Why does the system use double-entry accounting?
- What makes a transfer idempotent?
- Which rows are written in the same transaction during a transfer?
- How does deterministic account locking reduce deadlock risk?
- What happens if Kafka is down after a ledger transaction commits?
- How do reversal and adjustment flows preserve immutability?
- How would you detect reconciliation mismatches?
- What indexes matter for account history, audit search, and outbox polling?
- How would you test concurrent overdraft attempts?
- How do you explain this project in under two minutes?

