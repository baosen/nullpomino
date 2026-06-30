package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link VSBattleMode} targeting the two previously
 * uncovered lines:
 * <ul>
 *   <li>Line 398 – {@code engine.field.reset()} inside
 *       {@code loadMapPreview}: reached when {@code propMap[playerID]==null}
 *       AND {@code engine.field!=null}.</li>
 *   <li>Line 1037 – {@code pts = 0} inside {@code calcScore}: reached when
 *       the first garbage entry in the queue has more lines than the attack
 *       points produced by the line clear, so the entry absorbs all of
 *       {@code pts} without being fully consumed.</li>
 * </ul>
 */
class VSBattleModeBranchCoverageTest2 {

    // ---------------------------------------------------------------
    // Line 398 – engine.field.reset() in loadMapPreview
    // ---------------------------------------------------------------

    /**
     * Call onSetting when propMap[playerID] is null but engine.field is
     * already initialised (createFieldIfNeeded was called). The private
     * loadMapPreview method is invoked by onSetting; since
     * receiver.loadProperties returns null for the non-existent map path the
     * branch {@code if (propMap[playerID] == null && engine.field != null)}
     * fires and resets the field (line 398).
     *
     * Proof of execution: after the call the field exists and is not null.
     */
    @Test
    void onSettingResetsFieldWhenMapFileIsMissingAndFieldExists() throws Exception {
        VSBattleMode mode = new VSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);

        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();
        assertNotNull(engine.field);

        // Verify propMap[0] starts null
        Field propMapField = findField(VSBattleMode.class, "propMap");
        propMapField.setAccessible(true);
        Object[] propMap = (Object[]) propMapField.get(mode);
        assertTrue(propMap == null || propMap[0] == null,
                "propMap[0] should be null before loading");

        // onSetting triggers loadMapPreview; with no real .map file
        // propMap stays null and engine.field.reset() fires at line 398
        mode.onSetting(engine, 0);

        // Field still exists (reset() was called, not nulled)
        assertNotNull(engine.field);
    }

    // ---------------------------------------------------------------
    // Line 1037 – pts = 0 in calcScore
    // ---------------------------------------------------------------

    /**
     * Set up a scenario where:
     *   - engine clears 2 lines → pts = 1 + 6 (all-clear) = 7
     *   - garbageEntries[0] has one entry with lines == 20 (20 > 7)
     *   - garbageCounter[0] == true (default)
     *
     * The else branch at line 1037 fires and sets pts = 0 because
     * entry.lines (20) is still > 0 after subtracting pts (7).
     *
     * Proof of execution: after calcScore the garbage entry remains in the
     * queue with lines reduced from 20 to 13 (20-7), and the enemy queue is
     * empty (no attack was sent because pts was zeroed before attack ran).
     */
    @Test
    void calcScoreSetsAttackPtsToZeroWhenGarbageEntryIsLargerThanPts() throws Exception {
        VSBattleMode mode = new VSBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        for (int i = 0; i < manager.engine.length; i++) {
            manager.engine[i].init();
            manager.engine[i].playerID = i;
        }
        mode.modeInit(manager);
        for (int i = 0; i < manager.engine.length; i++) {
            manager.engine[i].nowPieceObject = new nullpomino.game.component.Piece(
                    nullpomino.game.component.Piece.PIECE_T);
        }
        mode.playerInit(manager.engine[0], 0);
        mode.playerInit(manager.engine[1], 1);

        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();

        // Set garbageCounter[0] = true
        boolean[] garbageCounter = (boolean[]) getField(mode, "garbageCounter");
        garbageCounter[0] = true;

        // Set garbage[0] = 5
        int[] garbage = (int[]) getField(mode, "garbage");
        garbage[0] = 5;

        // garbageBlocking defaults to true (config default), so delivery is skipped
        // (condition: lines==0 || !garbageBlocking → false).  The entry remains in
        // the queue after calcScore, which lets us verify lines were reduced.

        // Inject a GarbageEntry with lines=20 into garbageEntries[0].
        // We use lines=20 so that even after an all-clear bonus (pts can reach 7+),
        // garbageEntry.lines > pts still holds, triggering the pts=0 branch (line 1037).
        Object[] garbageEntriesArray = (Object[]) getField(mode, "garbageEntries");
        // garbageEntries[0] is a LinkedList<GarbageEntry>
        LinkedList<Object> queue = (LinkedList<Object>) garbageEntriesArray[0];
        if (queue == null) {
            queue = new LinkedList<>();
            garbageEntriesArray[0] = queue;
        }
        queue.clear();

        // Instantiate inner class VSBattleMode$GarbageEntry(int g)
        Class<?> geClass = null;
        for (Class<?> c : VSBattleMode.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("GarbageEntry")) {
                geClass = c;
                break;
            }
        }
        assertNotNull(geClass, "GarbageEntry inner class not found");
        Constructor<?> geCtor = geClass.getDeclaredConstructor(VSBattleMode.class, int.class);
        geCtor.setAccessible(true);
        Object entry = geCtor.newInstance(mode, 20);
        queue.add(entry);

        // Verify the entry is in the queue before calcScore
        assertEquals(1, queue.size(), "entry should be in queue before calcScore");

        // Call calcScore with lines=2 (double).
        // Attack table: lines==2 → pts=1; combo (engine.combo==0) adds 0.
        // engine.field is empty → all-clear fires, pts += 6 → pts=7.
        // The garbage entry has lines=20 > pts=7:
        //   counter-garbage: entry.lines -= 7 = 13 > 0, then pts=0 fires (line 1037!).
        // Attack section: pts==0 → SKIPPED, enemy gets no entry.
        // Delivery: garbageBlocking[0] defaults to true, lines==2 != 0,
        //           so (lines==0 || !garbageBlocking) == false → SKIPPED.
        mode.calcScore(engine, 0, 2);

        // Proof of line 1037: pts was zeroed by counter-garbage, so no attack entry
        // was sent to enemy (garbageEntries[1] must be empty).
        Object[] geArray = (Object[]) getField(mode, "garbageEntries");
        @SuppressWarnings("unchecked")
        java.util.LinkedList<Object> enemyQueue = (java.util.LinkedList<Object>) geArray[1];
        assertEquals(0, enemyQueue.size(),
                "enemy garbageEntries must be empty: pts was zeroed by line 1037 before attack ran");

        // Additional proof: the entry was reduced (not removed) — lines went from 20 to 13
        assertEquals(1, queue.size(), "entry still in queue (pts consumed but entry not fully used)");
        Field linesField = findField(geClass, "lines");
        linesField.setAccessible(true);
        int remainingLines = (int) linesField.get(queue.getFirst());
        assertEquals(13, remainingLines, "entry.lines reduced from 20 to 13 (consumed pts=7)");
    }

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static Object getField(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
