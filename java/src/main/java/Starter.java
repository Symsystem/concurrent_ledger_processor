// Candidate starter. Read PROBLEM.md first.
//
// The scaffolding below already launches 4 concurrent virtual threads; each
// one replays the FULL transaction stream into your `LedgerService.handle`.
// Your job is to make `handle` race-safe and idempotent and to return the
// final result from `snapshot`. Do not modify the worker loop or `main`.
//
// Run with:  mvn -q compile exec:java -Dexec.mainClass=Starter
// Goal:      `ok: true`, well under 1 second wall-clock.

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Starter {
    public static final int WORKER_COUNT = 4;
    public static final long IO_DELAY_MS = 20;

    // === The thing you need to design ========================================

    public static class LedgerService {
        private final Map<String, Double> balances;
        // TODO: add the synchronization primitives + bookkeeping state you need.

        public LedgerService(Map<String, Double> initial) {
            this.balances = new HashMap<>(initial);
        }

        /**
         * Called concurrently by every worker, for every transaction.
         * Each tx will arrive here up to WORKER_COUNT times (plus stream
         * duplicates). Apply each transaction at most once. Don't corrupt
         * shared state.
         */
        public void handle(Models.Transaction tx) throws InterruptedException {
            Thread.sleep(IO_DELAY_MS); // simulated upstream lookup
            // TODO: implement.
        }

        public Models.SnapshotResult snapshot() {
            // TODO: fill in appliedTxIds and rejectedTxIds (both seq-sorted).
            return new Models.SnapshotResult(new HashMap<>(balances), List.of(), List.of());
        }
    }

    // === Worker scaffolding — DO NOT MODIFY ==================================

    private static void worker(int id, LedgerService service, List<Models.Transaction> txs) {
        for (var tx : txs) {
            try {
                service.handle(tx);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "../ledger_test_data.json";
        var mapper = new ObjectMapper();
        Models.InputData data = mapper.readValue(new File(path), Models.InputData.class);

        var service = new LedgerService(data.initialBalances());
        List<Models.Transaction> txs = data.transactions();

        long start = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < WORKER_COUNT; i++) {
                final int id = i;
                futures.add(executor.submit(() -> { worker(id, service, txs); return null; }));
            }
            for (var f : futures) f.get();
        }
        double elapsed = (System.nanoTime() - start) / 1e9;

        var result = service.snapshot();
        boolean ok = result.equals(data.expected());
        System.out.printf("elapsed: %.3fs   ok: %s%n", elapsed, ok);
        if (!ok) {
            System.out.println("got:      " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
            System.out.println("expected: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(data.expected()));
            System.exit(1);
        }
    }
}
