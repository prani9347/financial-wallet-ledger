# 💰 Financial Wallet & Transaction Ledger

A backend system that simulates a digital wallet — supporting money transfers between users with **ACID-compliant transactions**, **manual rollback handling**, and **concurrency-safe locking**, built using core Java, JDBC, and MySQL (no framework abstraction).

---

## Why this project exists

Most fresher projects use `@Transactional` and never think about what's happening underneath. This one doesn't — every transaction here is managed manually with raw JDBC, to demonstrate a real understanding of:

- Atomic multi-step operations (debit + credit + ledger entry, all-or-nothing)
- Manual commit/rollback control
- Row-level locking to prevent race conditions during concurrent transfers
- Safe handling of concurrent access to the same wallet (double-spend prevention)

---

## Features

- Create wallets with an initial balance
- Credit / debit a wallet
- Transfer money between two wallets atomically
- Full ledger/audit trail of every transaction (success and failed)
- Manual transaction control using `commit()` / `rollback()`
- Row-level locking with `SELECT ... FOR UPDATE` to prevent race conditions
- Concurrent transfer testing with JUnit 5 (multi-threaded)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 23 |
| Build Tool | Maven |
| Database Access | JDBC |
| Database | MySQL |
| Testing | JUnit 5 |
| IDE | IntelliJ IDEA |

---

## Database Schema

**`wallet`**
Stores the current balance of each wallet.

| Column | Type |
|---|---|
| id | BIGINT (PK) |
| owner_name | VARCHAR |
| balance | DECIMAL |
| created_at | TIMESTAMP |

**`ledger_transaction`**
Stores every credit and debit event — the audit trail.

| Column | Type |
|---|---|
| id | BIGINT (PK) |
| wallet_id | BIGINT (FK → wallet.id) |
| type | VARCHAR (DEBIT / CREDIT) |
| amount | DECIMAL |
| description | VARCHAR |
| created_at | TIMESTAMP |

**Relationship:** One `wallet` → many `ledger_transaction` records.
```
1. Start database transaction (autoCommit = false)
2. Lock sender wallet   → SELECT ... FOR UPDATE
3. Lock receiver wallet → SELECT ... FOR UPDATE
4. Validate sender balance
5. Debit sender
6. Credit receiver
7. Insert ledger records for both sides
8. COMMIT
```

If any step fails (e.g. insufficient balance), the entire transaction is **rolled back** — no partial debit/credit ever persists.

```
Transfer failed.
ROLLBACK executed.
Reason: Insufficient balance.
```

---

## Concurrency Handling

Two concurrent transfer requests hitting the same wallet are tested using **JUnit 5 multi-threading**. Row-level locking (`SELECT ... FOR UPDATE`) ensures one transaction fully completes (commit or rollback) before the next can read/modify the same wallet row — preventing double-spending or lost updates.

```
Transfer successful.
Transfer successful.
Both concurrent transfers completed.
```

---

## What This Project Demonstrates

- Real understanding of ACID transactions — not just using an annotation, but implementing commit/rollback manually
- Concurrency control at the database level, not just application level
- Defensive design: every failure path leaves the database in a consistent state
- Ability to test concurrent behavior programmatically, not just assume it works
