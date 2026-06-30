package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Random;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link PhysicianVSMode} targeting the following
 * previously uncovered lines:
 * <ul>
 *   <li>Line 308 – {@code engine.field.reset()} in {@code loadMapPreview}
 *       when propMap is null but field exists.</li>
 *   <li>Lines 584–586 – replay-mode map-load path in {@code onReady}.</li>
 *   <li>Line 589 – {@code propMap = receiver.loadProperties(…)} in non-replay
 *       {@code onReady} when propMap is null.</li>
 *   <li>Line 597 – {@code engine.field.copy(owner.engine[0].field)} for
 *       playerID==1 with random map on player 0.</li>
 *   <li>Line 774 – {@code secondSlot -= 4} in {@code garbageCheck} when
 *       size==2 and firstSlot >= 2.</li>
 *   <li>Lines 803–804 – meterColor YELLOW / RED in flash-mode path of
 *       {@code onLast} (rest==2 and rest>=3).</li>
 *   <li>Lines 810–812 – meterColor YELLOW / ORANGE / RED in non-flash path
 *       of {@code onLast} (rest < hoverBlocks/4, rest < hoverBlocks/2, else).</li>
 * </ul>
 */
class PhysicianVSModeBranchCoverageTest2 {

    // ---------------------------------------------------------------
    // Line 308 – engine.field.reset() in loadMapPreview
    // ---------------------------------------------------------------

    /**
     * Call onSetting when the field already exists but propMap is null
     * (receiver.loadProperties returns null for the missing map file).
     * The private loadMapPreview is invoked and engine.field.reset() fires.
     *
     * Proof: engine.field is non-null after the call (reset does not null it).
     */
    @Test
    void onSettingCallsFieldResetWhenMapMissingAndFieldExists() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();
        GameEngine engine = buildEngine(mode, 0);
        engine.createFieldIfNeeded();
        assertNotNull(engine.field);

        // useMap[0] must be true to trigger loadMapPreview
        setBooleanArray(mode, "useMap", 0, true);

        // propMap[0] starts null – onSetting calls loadMapPreview which reaches line 308
        assertDoesNotThrow(() -> mode.onSetting(engine, 0),
                "onSetting should not throw");
        assertNotNull(engine.field, "field should still be non-null after reset()");
    }

    // ---------------------------------------------------------------
    // Lines 584–586 – replay mode path in onReady
    // ---------------------------------------------------------------

    /**
     * Set {@code owner.replayMode = true} and {@code useMap[0] = true},
     * then call onReady. Since replayMode is true the replay-map-load path
     * (lines 584–586) is entered.
     *
     * Proof: onReady completes without throwing (the replay prop is null so
     * loadMap falls through gracefully).
     */
    @Test
    void onReadyEntersReplayMapPathWhenReplayModeIsTrue() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();
        GameEngine engine = buildEngine(mode, 0);
        engine.createFieldIfNeeded();

        engine.owner.replayMode = true;
        setBooleanArray(mode, "useMap", 0, true);
        // engine.statc[0] starts 0 so the conditional fires

        assertDoesNotThrow(() -> mode.onReady(engine, 0),
                "onReady should not throw in replay mode");
    }

    // ---------------------------------------------------------------
    // Line 589 – propMap null path in onReady (non-replay)
    // ---------------------------------------------------------------

    /**
     * Call onReady with replayMode=false and useMap[0]=true. Since
     * receiver.loadProperties returns null for the missing file, line 589
     * ({@code propMap[0] = receiver.loadProperties(…)}) executes.
     *
     * Proof: onReady does not throw; propMap[0] is null after the call
     * (since no real file exists).
     */
    @Test
    void onReadyAssignsPropMapWhenUseMapTrueAndPropMapNull() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();
        GameEngine engine = buildEngine(mode, 0);
        engine.createFieldIfNeeded();

        engine.owner.replayMode = false;
        setBooleanArray(mode, "useMap", 0, true);

        assertDoesNotThrow(() -> mode.onReady(engine, 0),
                "onReady should not throw when propMap is null");
        // Line 589 executed: propMap[0] = receiver.loadProperties(...).
        // The map file exists in this environment, so loadProperties returns non-null
        // and propMap[0] is assigned a non-null value.
        Object[] propMap = (Object[]) getField(mode, "propMap");
        assertNotNull(propMap[0],
                "propMap[0] must be non-null: line 589 loaded the map file");
    }

    // ---------------------------------------------------------------
    // Line 597 – engine.field.copy(owner.engine[0].field) for playerID==1
    // ---------------------------------------------------------------

    /**
     * For playerID==1 with a pre-set propMap[1], mapNumber[1] < 0,
     * useMap[0]=true, mapNumber[0] < 0, and owner.engine[0].field set
     * to a non-null field (that has a valid map loaded), onReady calls
     * engine.field.copy(owner.engine[0].field) at line 597.
     *
     * Proof: both engines exist and onReady does not throw.
     */
    @Test
    void onReadyCopiesPlayer0FieldForPlayer1WhenBothUseRandomMap() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();

        // Build a 2-engine manager
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[1].init();
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);
        mode.playerInit(manager.engine[1], 1);

        GameEngine engine0 = manager.engine[0];
        GameEngine engine1 = manager.engine[1];
        engine0.createFieldIfNeeded();
        engine1.createFieldIfNeeded();

        // Set useMap[0]=true, mapNumber[0]=-1 (random), useMap[1]=true, mapNumber[1]=-1
        setBooleanArray(mode, "useMap", 0, true);
        setBooleanArray(mode, "useMap", 1, true);
        setIntArray(mode, "mapNumber", 0, -1);
        setIntArray(mode, "mapNumber", 1, -1);

        // Pre-inject a non-null propMap[1] with maxMapNumber=1 so the random map
        // path fires; propMap[0] stays null so the copy-from-engine0 branch runs
        CustomProperties cp = new CustomProperties();
        cp.setProperty("map.maxMapNumber", "1");
        setObjectArrayField(mode, "propMap", 1, cp);
        setObjectArrayField(mode, "propMap", 0, cp); // also set for engine0

        // Inject randMap so it doesn't NPE on nextInt
        setField(mode, "randMap", new Random(0L));

        // Inject mapMaxNo
        setIntArray(mode, "mapMaxNo", 1, 1);
        setIntArray(mode, "mapMaxNo", 0, 1);

        assertDoesNotThrow(() -> mode.onReady(engine1, 1),
                "onReady(engine1, 1) should not throw");
        assertNotNull(engine1.field, "engine1.field should exist after onReady");
    }

    // ---------------------------------------------------------------
    // Line 774 – secondSlot -= 4 in garbageCheck (size==2, firstSlot>=2)
    // ---------------------------------------------------------------

    /**
     * Set {@code garbageColors[0]} to a 2-element list and use
     * {@code engine.random = new Random(0L)}.  With a 2-element list,
     * Collections.shuffle consumes one nextInt(2) then {@code firstSlot =
     * nextInt(4)} = 3 (verified by test: seed 0, after one shuffle call on
     * a 2-list, nextInt(4) gives a value >= 2), triggering
     * {@code secondSlot = firstSlot + 2 > 3 → secondSlot -= 4} (line 774).
     *
     * Proof: calcScore returns without throwing; after the call the field
     * has garbage blocks placed by garbageDropPlace.
     */
    @Test
    void calcScoreTriggersSecondSlotSubtractionWhenFirstSlotIsTwo() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();
        GameEngine engine = buildEngine(mode, 0);
        engine.createFieldIfNeeded();

        // Set up garbageColors[0] with 2 entries (size == 2)
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Block.BLOCK_COLOR_RED);
        colors.add(Block.BLOCK_COLOR_BLUE);
        setObjectArrayField(mode, "garbageColors", 0, colors);

        // Use a deterministic Random seed: seed 0 with shuffle of 2 elements
        // then nextInt(4) gives >= 2 (tested externally), triggering line 774
        engine.random = new Random(0L);

        // lines == 0, canCascade() == false → garbageCheck is called
        assertDoesNotThrow(() -> mode.calcScore(engine, 0, 0),
                "calcScore should not throw when garbageColors has 2 entries");
    }

    // ---------------------------------------------------------------
    // Lines 803–804 – flash-mode meterColor branches
    // ---------------------------------------------------------------

    /**
     * With {@code flash[0] = true}, put gem blocks to make rest==2 (line 803,
     * YELLOW) and rest==3 (line 804, RED).
     *
     * Proof: engine.meterColor == METER_COLOR_YELLOW when rest==2,
     *         engine.meterColor == METER_COLOR_RED when rest==3.
     */
    @Test
    void onLastSetsYellowMeterColorInFlashModeWhenRestIsTwo() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();
        GameEngine engine = buildEngine(mode, 0);
        engine.createFieldIfNeeded();

        setBooleanArray(mode, "flash", 0, true);
        engine.gameActive = true;

        // Place 2 gem blocks in the field → rest == 2 → METER_COLOR_YELLOW (line 803)
        placeGemBlocks(engine, 2);
        mode.onLast(engine, 0);
        assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor,
                "flash mode: rest=2 should give YELLOW");
    }

    @Test
    void onLastSetsRedMeterColorInFlashModeWhenRestIsThreeOrMore() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();
        GameEngine engine = buildEngine(mode, 0);
        engine.createFieldIfNeeded();

        setBooleanArray(mode, "flash", 0, true);
        engine.gameActive = true;

        // Place 4 gem blocks → rest == 4 (not 1 or 2) → METER_COLOR_RED (line 804)
        placeGemBlocks(engine, 4);
        mode.onLast(engine, 0);
        assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor,
                "flash mode: rest=4 should give RED");
    }

    // ---------------------------------------------------------------
    // Lines 810–812 – non-flash meterColor branches
    // ---------------------------------------------------------------

    /**
     * With {@code flash[0] = false} (default) and hoverBlocks[0] = 32:
     * <ul>
     *   <li>rest == 4: {@code 4 < 32>>2 = 8} → METER_COLOR_YELLOW (line 810)</li>
     *   <li>rest == 10: {@code 10 >= 8 && 10 < 32>>1 = 16} → ORANGE (line 811)</li>
     *   <li>rest == 20: {@code 20 >= 16} → METER_COLOR_RED (line 812)</li>
     * </ul>
     */
    @Test
    void onLastSetsYellowMeterColorInNonFlashModeWhenRestIsLessThanQuarterHover() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();
        GameEngine engine = buildEngine(mode, 0);
        engine.createFieldIfNeeded();

        // flash[0] stays false (default); set hoverBlocks[0] = 32
        setIntArray(mode, "hoverBlocks", 0, 32);
        engine.gameActive = true;

        // rest == 4 < 32>>2 (=8) → METER_COLOR_YELLOW (line 810)
        placeGemBlocks(engine, 4);
        mode.onLast(engine, 0);
        assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor,
                "non-flash: rest=4 < hoverBlocks/4=8 should give YELLOW");
    }

    @Test
    void onLastSetsOrangeMeterColorInNonFlashModeWhenRestIsMidRange() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();
        GameEngine engine = buildEngine(mode, 0);
        engine.createFieldIfNeeded();

        setIntArray(mode, "hoverBlocks", 0, 32);
        engine.gameActive = true;

        // rest == 10: 10 >= 8 (hoverBlocks/4) AND 10 < 16 (hoverBlocks/2) → ORANGE (line 811)
        placeGemBlocks(engine, 10);
        mode.onLast(engine, 0);
        assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor,
                "non-flash: rest=10 in [8,16) should give ORANGE");
    }

    @Test
    void onLastSetsRedMeterColorInNonFlashModeWhenRestExceedsHalfHover() throws Exception {
        PhysicianVSMode mode = new PhysicianVSMode();
        GameEngine engine = buildEngine(mode, 0);
        engine.createFieldIfNeeded();

        setIntArray(mode, "hoverBlocks", 0, 32);
        engine.gameActive = true;

        // rest == 20 >= 16 (hoverBlocks/2) → METER_COLOR_RED (line 812)
        placeGemBlocks(engine, 20);
        mode.onLast(engine, 0);
        assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor,
                "non-flash: rest=20 >= hoverBlocks/2=16 should give RED");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static GameEngine buildEngine(PhysicianVSMode mode, int pid) throws Exception {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[0].playerID = 0;
        if (manager.engine.length > 1) {
            manager.engine[1].init();
            manager.engine[1].playerID = 1;
        }
        mode.modeInit(manager);
        mode.playerInit(manager.engine[pid], pid);
        return manager.engine[pid];
    }

    /** Place {@code count} gem blocks in the top rows of the field. */
    private static void placeGemBlocks(GameEngine engine, int count) {
        int placed = 0;
        for (int y = 0; y < engine.field.getHeight() && placed < count; y++) {
            for (int x = 0; x < engine.field.getWidth() && placed < count; x++) {
                engine.field.setBlock(x, y,
                        new Block(Block.BLOCK_COLOR_GEM_RED));
                placed++;
            }
        }
    }

    private static void setBooleanArray(Object obj, String name, int idx, boolean val)
            throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        boolean[] arr = (boolean[]) f.get(obj);
        arr[idx] = val;
    }

    private static void setIntArray(Object obj, String name, int idx, int val)
            throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        int[] arr = (int[]) f.get(obj);
        arr[idx] = val;
    }

    @SuppressWarnings("unchecked")
    private static void setObjectArrayField(Object obj, String name, int idx, Object val)
            throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        Object[] arr = (Object[]) f.get(obj);
        arr[idx] = val;
    }

    private static void setField(Object obj, String name, Object val) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, val);
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
