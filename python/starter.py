"""Candidate starter. Read README.md first.

Four OS threads consume transactions from a shared queue and call
`LedgerService.handle` on each one. Concurrency is preemptive — the GIL
gives you bytecode atomicity but not statement atomicity, so you'll need
real locks around read-modify-write critical sections.

Your job is to make `handle` race-safe and idempotent and to surface the
final result via `snapshot`. Do not modify the worker loop or `run`.

Run with:  python3 starter.py
Goal:      `ok: True`, well under 400 ms wall-clock.
"""

import json
import os
import queue
import sys
import threading
import time

WORKER_COUNT = 4
IO_DELAY_S = 0.030
DEFAULT_DATA_PATH = os.path.join(os.path.dirname(__file__), "..", "ledger_test_data.json")


# === The thing you need to design =============================================

class LedgerService:
    def __init__(self, initial_balances: dict) -> None:
        self.balances: dict = dict(initial_balances)
        # TODO: add the synchronization primitives + bookkeeping state you need.

    def handle(self, tx: dict) -> None:
        """Called from multiple worker threads, one transaction at a time.

        The shared queue guarantees each entry is delivered to exactly one
        worker, but the input stream itself contains duplicates (upstream is
        at-least-once). Apply each transaction at most once. Don't corrupt
        shared state.

        Validation rules and rejection semantics: see README.md.
        """
        time.sleep(IO_DELAY_S)  # simulated upstream lookup
        # TODO: implement.

    def snapshot(self) -> dict:
        """Return the final result in the shape expected by the test fixture."""
        # TODO: fill in applied_tx_ids and rejected_tx_ids (both seq-sorted).
        return {
            "balances": dict(self.balances),
            "applied_tx_ids": [],
            "rejected_tx_ids": [],
        }


# === Worker scaffolding — DO NOT MODIFY =======================================
# Transactions sit on a shared queue. WORKER_COUNT threads pull from it and
# dispatch into `service.handle`. Each queue entry goes to exactly one worker,
# but the stream contains duplicates, so `handle` must still be idempotent.

_SENTINEL: object = object()


def worker(worker_id: int, service: LedgerService, q: "queue.Queue") -> None:
    while True:
        tx = q.get()
        if tx is _SENTINEL:
            return
        service.handle(tx)


def run(data: dict) -> dict:
    service = LedgerService(data["initial_balances"])
    q: "queue.Queue" = queue.Queue()
    for tx in data["transactions"]:
        q.put(tx)
    for _ in range(WORKER_COUNT):
        q.put(_SENTINEL)

    threads = [
        threading.Thread(target=worker, args=(i, service, q), daemon=True)
        for i in range(WORKER_COUNT)
    ]
    for t in threads:
        t.start()
    for t in threads:
        t.join()
    return service.snapshot()


# === Test harness =============================================================

if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_DATA_PATH
    with open(path) as f:
        data = json.load(f)
    start = time.perf_counter()
    result = run(data)
    elapsed = time.perf_counter() - start
    expected = data["expected"]
    ok = result == expected
    print(f"elapsed: {elapsed:.3f}s   ok: {ok}")
    if not ok:
        print("got:     ", json.dumps(result, indent=2))
        print("expected:", json.dumps(expected, indent=2))
        sys.exit(1)
