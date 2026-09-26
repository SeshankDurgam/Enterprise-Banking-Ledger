# Glossary

## People and operational roles

BankingLedger documents five access roles: `CUSTOMER`, `TELLER`, `OPS_ADMIN`, `AUDITOR`, and `SERVICE`. Job titles describe responsibilities; access roles determine permissions. The broader banking terms below provide context and do not imply additional implemented roles or workflows. See [Project](../backend/Project.md) for the documented actors and security scope.

| Term | Meaning |
| --- | --- |
| Customer | A person or organization using the bank's services. `CUSTOMER` identifies customer access in BankingLedger; access to an account also depends on ownership checks. |
| Teller | A customer-facing branch employee who handles routine account service and, in a bank, cash transactions such as deposits and withdrawals. BankingLedger documents `TELLER` access for account creation and account-service workflows; the title does not imply a cash-drawer implementation. |
| Front office | Staff and tools that interact directly with customers, such as tellers and customer-service staff. This describes a business function, not a separate BankingLedger access role. |
| Back office | Staff and processes that support transaction processing, settlement, reconciliation, and correction after customer interaction. This describes a business function, not a separate BankingLedger access role. |
| Operations staff / operator | Staff who investigate transaction problems and carry out operational work such as reversals, adjustments, and reconciliation. BankingLedger uses `OPS_ADMIN` for privileged operations access. |
| Operations administrator / ops admin | A privileged operational user represented by `OPS_ADMIN` in BankingLedger. The documented scope includes financial corrections, reconciliation, and outbox operations. Exact permissions depend on the endpoint. |
| Office administrator | A general workplace administration title, often covering records, scheduling, and office coordination. It is not a BankingLedger access role. Use **operations administrator** when referring to `OPS_ADMIN`. |
| Branch manager / supervisor | A person responsible for branch operations and staff oversight. Depending on bank policy, they may approve exceptions or higher-value transactions. BankingLedger does not list a separate branch-manager role. |
| Auditor | A reviewer who examines financial records and operational evidence to check what happened and who acted. BankingLedger documents `AUDITOR` access for ledger, audit, and reporting queries. |
| Reconciliation analyst | An operations specialist who compares internal transactions with settlement records and investigates mismatches. This is a responsibility within operational work, not a separately documented BankingLedger role. |
| Service client | Software that calls the API on behalf of a system rather than a person. `SERVICE` is BankingLedger's documented machine-access role; it should not be confused with a customer-service employee. |
| Technical administrator | A person who manages infrastructure, deployment, databases, or system configuration. Technical administration is distinct from the financial operations represented by `OPS_ADMIN`. |
| Maker / checker | Two participants in an approval process: the maker proposes an action and a different checker reviews it before execution. These are workflow responsibilities, not additional BankingLedger access roles. |

## Ledger and integration terms

| Term | Meaning |
| --- | --- |
| Account | A customer or internal balance container denominated in one currency. |
| Available balance | Cached spendable balance used for fast reads and transfer validation. |
| Ledger balance | Cached posted ledger balance derived from accepted journal postings. |
| Ledger transaction | The business-level financial transaction, such as a transfer, reversal, fee, or adjustment. |
| Journal entry | A balanced accounting entry attached to one ledger transaction. |
| Posting | A single debit or credit line in a journal entry. |
| Double-entry | Accounting model requiring every journal entry to have equal total debits and credits. |
| Transfer | Movement between two accounts posted through the ledger engine. |
| Reversal | Immutable correction that posts opposite entries instead of editing the original transaction. |
| Adjustment | Operational correction with explicit balanced posting lines and reason codes. |
| Reconciliation | Comparison between internal ledger transactions and external settlement records. |
| Settlement batch | Imported group of external settlement items. |
| Mismatch | A reconciliation result showing amount, currency, status, duplicate, or missing-record differences. |
| Audit event | Append-only operational event containing actor, role, entity, correlation id, and redacted payload. |
| Outbox event | Database-backed event record published asynchronously to Kafka after commit. |
| Idempotency key | Client-provided key allowing safe retry of a mutating request. |
| Correlation id | Request identifier propagated through logs, audit records, outbox events, and error responses. |
