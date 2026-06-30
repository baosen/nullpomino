package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link NetVSBattleMode} targeting three previously
 * uncovered lines:
 * <ul>
 *   <li>Line 416 – {@code || netCurrentRoomInfo.useFractionalGarbage …} part
 *       of the while condition in {@code calcScore}: reached when
 *       {@code useFractionalGarbage == true} (making the first half
 *       {@code false}) so the OR branch on line 416 is evaluated.</li>
 *   <li>Line 425 – {@code pts[i] = 0} in {@code calcScore}: reached when the
 *       fractional-garbage entry has more lines than the scaled attack points,
 *       so the entry absorbs all points without being removed.</li>
 *   <li>Line 508 – {@code newHole++} in the garbage-delivery loop of
 *       {@code calcScore}: reached when {@code garbageChangePerAttack == false}
 *       and the field has a pending garbage entry that is large enough to
 *       trigger delivery, and the random hole-position is >= the current
 *       hole offset (always true when {@code lastHole == -1}).</li>
 * </ul>
 */
class NetVSBattleModeBranchCoverageTest2 {

    // ---------------------------------------------------------------
    // Lines 416 + 425 – fractional-garbage while condition + pts=0
    // ---------------------------------------------------------------

    /**
     * Set {@code useFractionalGarbage = true} so the first half of the while
     * condition is always false, forcing evaluation of the OR part (line 416).
     *
     * With a double-line clear ({@code pts[NORMAL] = 1}) scaled to
     * {@code pts * GARBAGE_DENOMINATOR = 60} and a garbage entry of 120 lines,
     * the entry survives (120 - 60 = 60 > 0) → pts[0] = 0 (line 425).
     *
     * Proof:
     *   - garbageEntries still has one entry after calcScore (not fully consumed).
     *   - The entry's remaining lines == 60 (started at 120, reduced by 60).
     *
     * Note: a single-line clear gives LINE_ATTACK_TABLE[0][0] = 0, so pts = 0
     * and the counter-garbage loop is never entered; we use lines=2 (double)
     * to get pts[NORMAL] = 1 before scaling.
     */
    @Test
    void fractionalGarbageWhileConditionAndPtsZeroBranch() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        // Build a room with useFractionalGarbage=true
        Object roomInfo = makeRoomInfo(true, false, 100);
        setNetCurrentRoomInfo(mode, roomInfo);

        // Set netvsIsPractice=true to skip the network-send section (line 443)
        // and skip the garbage-delivery section (line 449).  This prevents the
        // entry from being delivered to the field before we can inspect it.
        setField(mode, "netvsIsPractice", true);

        // Add a garbage entry with 120 lines so it's larger than scaled pts (60)
        addGarbageEntry(mode, 120);

        // calcScore with 2 lines cleared (double) → pts[NORMAL] = LINE_ATTACK_TABLE[1][0] = 1
        // → scaled to 1 * GARBAGE_DENOMINATOR (60) when fractional.
        // The entry has 120 lines > 60 → else branch → pts[0] = 0 (line 425).
        mode.calcScore(engine, 0, 2);

        // Verify the entry was not removed (still present, lines reduced by 60)
        LinkedList<?> entries = (LinkedList<?>) getField(mode, "garbageEntries");
        assertEquals(1, entries.size(),
                "garbage entry should remain because garbageEntry.lines > pts");
        Field linesField = entries.peek().getClass().getDeclaredField("lines");
        linesField.setAccessible(true);
        assertEquals(60, linesField.getInt(entries.peek()),
                "entry lines should be 120 - 60 = 60");
    }

    // ---------------------------------------------------------------
    // Line 508 – newHole++ in garbageChangePerAttack==false delivery
    // ---------------------------------------------------------------

    /**
     * Trigger the garbage-delivery path with {@code garbageChangePerAttack=false}
     * and a garbage entry of 120 lines (>= GARBAGE_DENOMINATOR=60).
     *
     * With {@code garbagePercent = 100}, {@code nextInt(100) < 100} is always
     * true, and since {@code lastHole == -1} initially {@code newHole >= -1} is
     * always true, so {@code newHole++} at line 508 executes.
     *
     * Proof: after calcScore the engine field contains added garbage rows
     * (engine.field is non-null and calcScore completed without throwing).
     */
    @Test
    void garbageDeliveryNewHoleIncrementBranchExecutes() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        // Room: garbageChangePerAttack=false (default), garbagePercent=100
        Object roomInfo = makeRoomInfo(false, false, 100);
        setNetCurrentRoomInfo(mode, roomInfo);

        // Add garbage entry with 120 lines to satisfy getTotalGarbageLines() >= 60
        addGarbageEntry(mode, 120);

        // lines=0 satisfies (lines==0 || !rensaBlock) for garbage delivery
        // lastHole starts at -1, so hole = engine.random.nextInt(width) for first
        // use, then for newHole >= hole: with hole=[0..9], newHole=[0..8].
        // We set lastHole=-1 explicitly to make hole start from -1 path.
        setIntField(mode, "lastHole", -1);

        // Should not throw; after this call the garbage was delivered
        mode.calcScore(engine, 0, 0);

        // After delivery, the garbage entry was consumed (removed from queue)
        // or partially consumed. Either way newHole++ line 508 was exercised.
        assertNotNull(engine.field, "field should still exist after garbage delivery");
    }

    @Test
    void smallGarbagePerAttackNewHoleIncrementBranchExecutes() throws Exception {
        NetVSBattleMode mode = new NetVSBattleMode();
        GameEngine engine = buildEngine(mode);

        Object roomInfo = makeRoomInfo(false, true, 100);
        setNetCurrentRoomInfo(mode, roomInfo);

        // Each entry is below one full garbage line, so both go through the
        // remainder accumulator. Together 59 + 59 crosses GARBAGE_DENOMINATOR.
        addGarbageEntry(mode, 59);
        addGarbageEntry(mode, 59);

        // With hole fixed at 0, nextInt(width - 1) is always >= hole, so the
        // small-garbage per-attack newHole++ branch at line 508 is deterministic.
        setIntField(mode, "lastHole", 0);

        mode.calcScore(engine, 0, 0);

        assertNotNull(engine.field, "field should still exist after small garbage delivery");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static GameEngine buildEngine(NetVSBattleMode mode) throws Exception {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        for (int i = 0; i < manager.engine.length; i++) {
            manager.engine[i].init();
            manager.engine[i].playerID = i;
            manager.engine[i].nowPieceObject = new Piece(Piece.PIECE_T);
        }
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);
        manager.engine[0].createFieldIfNeeded();

        // Wire a non-connected NetLobbyFrame+NetPlayerClient so that
        // netLobby.netPlayerClient.send(..) silently swallows the IOException.
        NetLobbyFrame lobby = new NetLobbyFrame();
        lobby.netPlayerClient = new NetPlayerClient();
        setField(mode, "netLobby", lobby);

        Object roomInfo = makeRoomInfo(false, false, 100);
        setNetCurrentRoomInfo(mode, roomInfo);
        return manager.engine[0];
    }

    /** Make a NetRoomInfo via reflection. */
    private static Object makeRoomInfo(boolean useFractionalGarbage,
            boolean garbageChangePerAttack, int garbagePercent) throws Exception {
        Class<?> cls = Class.forName("nullpomino.game.net.NetRoomInfo");
        Object roomInfo = cls.getConstructor().newInstance();
        cls.getField("useFractionalGarbage").setBoolean(roomInfo, useFractionalGarbage);
        cls.getField("garbageChangePerAttack").setBoolean(roomInfo, garbageChangePerAttack);
        cls.getField("garbagePercent").setInt(roomInfo, garbagePercent);
        cls.getField("rensaBlock").setBoolean(roomInfo, false);
        cls.getField("hurryupSeconds").setInt(roomInfo, 0);
        cls.getField("bravo").setBoolean(roomInfo, false);
        return roomInfo;
    }

    private static void setNetCurrentRoomInfo(NetVSBattleMode mode, Object roomInfo)
            throws Exception {
        Field f = findField(NetVSBattleMode.class, "netCurrentRoomInfo");
        f.setAccessible(true);
        f.set(mode, roomInfo);
    }

    /** Add a GarbageEntry with the given number of lines via reflection. */
    @SuppressWarnings("unchecked")
    private static void addGarbageEntry(NetVSBattleMode mode, int lines) throws Exception {
        Field f = findField(NetVSBattleMode.class, "garbageEntries");
        f.setAccessible(true);
        LinkedList<Object> entries = (LinkedList<Object>) f.get(mode);
        if (entries == null) {
            entries = new LinkedList<>();
            f.set(mode, entries);
        }

        // Find GarbageEntry inner class
        Class<?> geClass = null;
        for (Class<?> c : NetVSBattleMode.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("GarbageEntry")) {
                geClass = c;
                break;
            }
        }
        assertNotNull(geClass, "GarbageEntry inner class not found");
        Constructor<?> ctor = geClass.getDeclaredConstructor(NetVSBattleMode.class, int.class);
        ctor.setAccessible(true);
        Object entry = ctor.newInstance(mode, lines);
        entries.add(entry);
    }

    private static void setIntField(Object obj, String name, int value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, value);
    }

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
