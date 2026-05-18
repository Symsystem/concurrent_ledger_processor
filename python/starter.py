"""
Candidate starter. Read PROBLEM.md first.

The scaffolding below already launches 4 concurrent workers, each of which
replays the FULL transaction stream into your `LedgerService.handle` method.
Your job is to make `handle` race-safe and idempotent and to surface the
final result via `snapshot`. Do not modify the worker loop or `run`.

Run with:  python3 starter.py
Goal:      `ok: True`, well under 1 second wall-clock.
"""

import asyncio
import json
import os
import sys
import time

WORKER_COUNT = 4
IO_DELAY_S = 0.02
DEFAULT_DATA_PATH = os.path.join(os.path.dirname(__file__), "..", "ledger_test_data.json")


# === The thing you need to design =============================================

class LedgerService:
    def __init__(self, initial_balances: dict) -> None:
        self.balances: dict = dict(initial_balances)
        # TODO: add the synchronization primitives + bookkeeping state you need.

    async def handle(self, tx: dict) -> None:
        """Called concurrently by every worker, for every transaction.

        Each transaction will arrive here up to WORKER_COUNT times (once per
        worker), on top of any duplicates already present in the input stream.
        Apply each transaction at most once. Don't corrupt shared state.

        Validation rules and rejection semantics: see PROBLEM.md.
        """
        await asyncio.sleep(IO_DELAY_S)  # simulated upstream lookup
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
# Four workers run concurrently. Each one walks the whole transaction stream
# and dispatches every transaction into the service. Concurrent calls into
# `handle` are the whole point of the exercise.

async def worker(worker_id: int, service: LedgerService, transactions: list) -> None:
    for tx in transactions:
        await service.handle(tx)


async def run(data: dict) -> dict:
    service = LedgerService(data["initial_balances"])
    txs = data["transactions"]
    await asyncio.gather(*(worker(i, service, txs) for i in range(WORKER_COUNT)))
    return service.snapshot()


# === Test harness =============================================================

if __name__ == "__main__":
    path = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_DATA_PATH
    with open(path) as f:
        data = json.load(f)
    start = time.perf_counter()
    result = asyncio.run(run(data))
    elapsed = time.perf_counter() - start
    expected = data["expected"]
    ok = result == expected
    print(f"elapsed: {elapsed:.3f}s   ok: {ok}")
    if not ok:
        print("got:     ", json.dumps(result, indent=2))
        print("expected:", json.dumps(expected, indent=2))
        sys.exit(1)
