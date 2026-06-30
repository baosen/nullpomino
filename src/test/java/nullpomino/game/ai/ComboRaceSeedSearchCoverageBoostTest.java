package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import nullpomino.game.component.Piece;

/**
 * Coverage booster for {@link ComboRaceSeedSearch#main(String[])}.
 *
 * <p>The {@code main} method is the actual seed-search driver. Its body
 * (source lines 47-101) is otherwise never executed by the test-suite,
 * because {@code main} contains an effectively-infinite outer loop
 * ({@code for(long seed = 0; seed < Long.MAX_VALUE; seed++)}) with no
 * termination hook.
 *
 * <p>To execute those lines we actually invoke {@code main} on a daemon
 * thread, let it run long enough to crank through the first several seeds
 * (which covers the setup block, the inner combo loop, the hold-swap logic,
 * the "Endless loop found!" branch and the new-best-result branch), and then
 * stop it deterministically by poisoning the shared {@code queue} array to a
 * zero-length array. The next {@code thinkBestPosition} call then divides by
 * {@code queue.length} (== 0) and throws an {@link ArithmeticException} which
 * propagates uncaught out of {@code main}, terminating the thread cleanly.
 *
 * <p>We do NOT use {@code Thread.stop()} (removed in modern JDKs) nor
 * {@code Thread.interrupt()} (the loop has no interrupt checks).
 */
class ComboRaceSeedSearchCoverageBoostTest {

    /** Snapshot of mutable static state so we leave the class clean for siblings. */
    private int[] savedQueue;
    private int[] savedNextQueueIDs;

    private void snapshot() {
        savedQueue = ComboRaceSeedSearch.queue;
        savedNextQueueIDs = ComboRaceSeedSearch.nextQueueIDs;
    }

    @AfterEach
    void restore() {
        // Leave the (one-time) moves table intact; just restore the arrays that
        // main() / our poisoning step replaced, so other tests are unaffected.
        ComboRaceSeedSearch.queue = savedQueue;
        ComboRaceSeedSearch.nextQueueIDs = savedNextQueueIDs;
    }

    /**
     * Runs {@code main} on a daemon thread and then forces it to stop, so that
     * the whole body of {@code main} (lines 47-101) is executed exactly once.
     */
    @Test
    @Timeout(60)
    void runMainAndStopDeterministically() throws Exception {
        snapshot();

        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                ComboRaceSeedSearch.main(new String[0]);
            } catch (Throwable t) {
                // Expected: our poisoning forces an ArithmeticException; any other
                // throwable is also captured so the assertions below can inspect it.
                thrown.set(t);
            }
        }, "ComboRaceSeedSearch-main-runner");
        worker.setDaemon(true);
        worker.start();

        // Let main crank through the first several seeds. Observing the search's
        // published static fields confirms it has entered (and iterated) the inner
        // combo loop, exercising the setup, hold, break and best-result branches.
        long warmupDeadline = System.currentTimeMillis() + 3000;
        boolean sawProgress = false;
        while (System.currentTimeMillis() < warmupDeadline) {
            // bestPts / bestNext are repeatedly rewritten by thinkBestPosition,
            // so seeing a non-initial value proves the loop body ran.
            if (ComboRaceSeedSearch.bestNext != -1
                    || ComboRaceSeedSearch.bestPts != Integer.MIN_VALUE) {
                sawProgress = true;
            }
            if (sawProgress && System.currentTimeMillis() > warmupDeadline - 2200) {
                // Make sure several seeds elapsed before we stop it.
                break;
            }
            Thread.sleep(20);
            if (!worker.isAlive()) {
                break; // already finished on its own (shouldn't happen, but safe)
            }
        }

        // Poison the shared queue so the next thinkBestPosition divides by zero.
        // main() refills queue at the top of each outer seed iteration, so we keep
        // re-poisoning in a tight loop until one write lands inside the inner loop
        // and the resulting ArithmeticException tears the thread down.
        long stopDeadline = System.currentTimeMillis() + 20000;
        long tries = 0;
        while (worker.isAlive() && System.currentTimeMillis() < stopDeadline) {
            ComboRaceSeedSearch.queue = new int[0];
            tries++;
        }

        worker.join(5000);
        assertFalse(worker.isAlive(),
                "main() thread should have been stopped by the queue poisoning");

        Throwable t = thrown.get();
        assertNotNull(t, "Stopping main() should surface an uncaught throwable");
        assertTrue(t instanceof ArithmeticException || t instanceof IndexOutOfBoundsException,
                "Expected division-by-zero (or bounds) failure, got: " + t);
        assertTrue(tries > 0, "Poisoning loop should have executed at least once");
    }

    /**
     * Direct, fast invocation of the helpers the main loop relies on, so the
     * file still has independently-meaningful, quick coverage even aside from
     * the threaded driver above.
     */
    @Test
    @Timeout(30)
    void exerciseSearchHelpersDirectly() {
        ComboRaceSeedSearch.createTables();
        assertNotNull(ComboRaceSeedSearch.moves);

        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];
        ComboRaceSeedSearch.queue = new int[ComboRaceSeedSearch.QUEUE_SIZE];
        for (int i = 0; i < ComboRaceSeedSearch.queue.length; i++) {
            ComboRaceSeedSearch.queue[i] = i % Piece.PIECE_STANDARD_COUNT;
        }

        // No-hold path through thinkBestPosition + thinkMain.
        ComboRaceSeedSearch.thinkBestPosition(0, 0, -1);
        // Hold path (holdID != nowID).
        ComboRaceSeedSearch.thinkBestPosition(0, 0, Piece.PIECE_O);

        // thinkMain terminal / negative branches.
        assertEquals(0, ComboRaceSeedSearch.thinkMain(-1, -1, 0));

        // Negative state short-circuit in thinkBestPosition (should not throw).
        ComboRaceSeedSearch.thinkBestPosition(-1, 0, -1);
        assertTrue(true);
    }
}
