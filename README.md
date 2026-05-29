# Concurrent Ledger Processor

**Time:** ~30 minutes &nbsp;•&nbsp; **Language:** Python / Java / Node.js — pick one

Starter files are provided for each language. The scaffolding (the work
queue and the test harness) is identical across all three; only the
`LedgerService` class is yours to fill in.

```
.
├── README.md
├── ledger_test_data.json
├── python/   starter.py            (run: python3 starter.py)
├── java/     src/main/java/Starter.java
│                                   (run: mvn -q compile exec:java -Dexec.mainClass=Starter)
└── node/     starter.js            (run: node starter.js, requires Node 18+)
```

## Context

You're building the consumer for a payment-platform event stream. Your
service pulls transactions off a shared work queue using **4 concurrent
workers** (OS threads in Python and Java; async coroutines in Node). Each
queue entry is consumed by exactly one worker — but the upstream stream
is at-least-once, so the queue itself contains duplicates: the same
`tx_id` can appear in more than one entry.

The starter already wires up the queue and the 4 workers. Each worker
loops pulling from the queue and calls `service.handle(tx)` on whatever
it gets. The worker loop and the harness are out of scope — don't change
them.

## Your task

Implement `LedgerService.handle` and `LedgerService.snapshot` so that the
program's output equals `data["expected"]` for the provided fixture.

### Requirements

1. **Each transaction is applied at most once**, no matter how many times
   it is delivered.

2. **Transfers are atomic** — both balances update, or neither does.

3. **No corrupted state** under concurrent delivery.

4. **Validation — reject (don't apply) a transaction if:**
   - the source account would go below 0,
   - any referenced account doesn't exist,
   - `amount <= 0`,
   - a transfer's `from` equals its `to`.

   Each rejected `tx_id` appears once in `rejected_tx_ids`, even if
   delivered many times.

5. **`applied_tx_ids` and `rejected_tx_ids` are returned in ascending
   `seq` order.**

6. **Performance.** The simulated upstream lookup inside `handle` is
   30 ms. A correct, properly concurrent implementation finishes well
   under 400 ms wall-clock. There are 38 transactions in total.

### Transaction shape

```jsonc
// deposit / withdraw
{ "tx_id": "t01", "seq": 1, "type": "deposit",  "account": "alice", "amount": 50 }
{ "tx_id": "t03", "seq": 3, "type": "withdraw", "account": "alice", "amount": 30 }

// transfer
{ "tx_id": "t04", "seq": 5, "type": "transfer", "from": "alice", "to": "bob", "amount": 70 }
```

We care more about how you reason than how much you finish. Be ready to
talk through your choices and what you'd add next.
