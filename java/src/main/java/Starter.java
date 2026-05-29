// Candidate starter. Read README.md first.
//
// The scaffolding below puts every transaction onto a shared queue, then
// launches 4 worker virtual threads that pull from it concurrently. Each
// queue entry is delivered to exactly one worker, but the input stream
// itself contains duplicates (upstream is at-least-once), so `handle`
// must still be idempotent.
//
// Run with:  mvn -q compile exec:java -Dexec.mainClass=Starter
// Goal:      `ok: true`, well under 400 ms wall-clock.

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Starter {
    public static final int WORKER_COUNT = 4;
    public static final long IO_DELAY_MS = 30;

    // === The thing you need to design ========================================

    public static class LedgerService {
        private final Map<String, Double> balances;
        // TODO: add the synchronization primitives + bookkeeping state you need.

        public LedgerService(Map<String, Double> initial) {
            this.balances = new HashMap<>(initial);
        }

        /**
         * Called from multiple worker threads, one transaction at a time.
         * The queue delivers each entry to exactly one worker, but the
         * stream contains duplicates. Apply each transaction at most once.
         * Don't corrupt shared state.
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

    private static void worker(int id, LedgerService service, ConcurrentLinkedQueue<Models.Transaction> queue) {
        Models.Transaction tx;
        while ((tx = queue.poll()) != null) {
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
        var queue = new ConcurrentLinkedQueue<>(data.transactions());

        long start = System.nanoTime();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < WORKER_COUNT; i++) {
                final int id = i;
                futures.add(executor.submit(() -> { worker(id, service, queue); return null; }));
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
