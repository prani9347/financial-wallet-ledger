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

The wallet's sender balance is protected against overdraft using **row-level locking** (`SELECT ... FOR UPDATE`), with wallets always locked in ascending ID order to prevent deadlocks between concurrent transfers.

A JUnit test simulates 50 concurrent threads all attempting to transfer ₹100 from the same sender wallet (starting balance: ₹1000) to the same receiver wallet at the same time.

**Actual test result:**

```
TOTAL ATTEMPTS      = 50
SUCCESS COUNT       = 10
FAILED COUNT        = 40
FINAL SENDER BAL    = 0.00
FINAL RECEIVER BAL  = 1000.00
```

Since the sender only has enough balance for 10 transfers of ₹100 (10 × 100 = 1000), exactly 10 threads succeeded and the remaining 40 correctly failed with "Insufficient balance." The final balances are mathematically exact — no overdraft occurred, no money was lost or duplicated, and the sum of both wallets after the test (0 + 1000 = 1000) matches the starting total exactly.

---

## What This Project Demonstrates

- Real understanding of ACID transactions — not just using an annotation, but implementing commit/rollback manually
- Concurrency control at the database level, not just application level
- Defensive design: every failure path leaves the database in a consistent state
- Ability to test concurrent behavior programmatically, not just assume it works
