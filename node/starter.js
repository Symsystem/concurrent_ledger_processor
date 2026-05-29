// Candidate starter. Read README.md first.
//
// The scaffolding below puts every transaction onto a shared queue, then
// launches 4 worker coroutines that shift from it concurrently. Each queue
// entry is delivered to exactly one worker, but the input stream itself
// contains duplicates (upstream is at-least-once), so `handle` must still
// be idempotent.
//
// Run with:  node starter.js   (Node 18+; ESM)
// Goal:      `ok: true`, well under 400 ms wall-clock.
//
// Hint to keep in mind: Node runs on a single-threaded event loop. Async
// tasks can only interleave at `await` points — code between two awaits
// runs to completion uninterrupted. Use that to your advantage.

import fs from 'node:fs';

const WORKER_COUNT = 4;
const IO_DELAY_MS = 30;

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

// === The thing you need to design ============================================

class LedgerService {
  constructor(initialBalances) {
    this.balances = { ...initialBalances };
    // TODO: add the bookkeeping state you need.
  }

  /**
   * Called from multiple worker coroutines, one transaction at a time.
   * The queue delivers each entry to exactly one worker, but the stream
   * contains duplicates. Apply each transaction at most once. Don't
   * corrupt shared state.
   */
  async handle(tx) {
    await sleep(IO_DELAY_MS); // simulated upstream lookup
    // TODO: implement.
  }

  snapshot() {
    // TODO: fill in applied_tx_ids and rejected_tx_ids (both seq-sorted).
    return {
      balances: { ...this.balances },
      applied_tx_ids: [],
      rejected_tx_ids: [],
    };
  }
}

// === Worker scaffolding — DO NOT MODIFY ======================================
// Transactions live on a shared queue. Workers race to pull from it. In
// Node's single-threaded event loop, `queue.shift()` is atomic between
// awaits, so each entry is handed to exactly one worker.

async function worker(id, service, queue) {
  while (queue.length > 0) {
    const tx = queue.shift();
    if (tx === undefined) return;
    await service.handle(tx);
  }
}

async function run(data) {
  const service = new LedgerService(data.initial_balances);
  const queue = [...data.transactions];
  const workers = Array.from({ length: WORKER_COUNT }, (_, i) => worker(i, service, queue));
  await Promise.all(workers);
  return service.snapshot();
}

// === Test harness ============================================================

function canonicalize(value) {
  if (Array.isArray(value)) return value.map(canonicalize);
  if (value && typeof value === 'object') {
    return Object.keys(value).sort().reduce((acc, k) => {
      acc[k] = canonicalize(value[k]);
      return acc;
    }, {});
  }
  return value;
}

const deepEqual = (a, b) => JSON.stringify(canonicalize(a)) === JSON.stringify(canonicalize(b));

const filePath = process.argv[2] || new URL('../ledger_test_data.json', import.meta.url).pathname;
const data = JSON.parse(fs.readFileSync(filePath, 'utf-8'));

const start = process.hrtime.bigint();
const result = await run(data);
const elapsed = Number(process.hrtime.bigint() - start) / 1e9;
const expected = data.expected;
const ok = deepEqual(result, expected);

console.log(`elapsed: ${elapsed.toFixed(3)}s   ok: ${ok}`);
if (!ok) {
  console.log('got:     ', JSON.stringify(result, null, 2));
  console.log('expected:', JSON.stringify(expected, null, 2));
  process.exit(1);
}
