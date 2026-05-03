package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link AvalancheVSSPFMode}: covers getName,
 * playerInit initialization, loadOtherSetting/saveOtherSetting round-trip,
 * static multiplier methods, onMove/onClear flag resets, lineClearEnd
 * countdown/ojama drop logic, and readyInit drop pattern setup.
 */
class AvalancheVSSPFModeExtendedGameLogicTest {

    @Test
    void getNameReturnsExpected() {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        assertEquals("AVALANCHE-SPF VS-BATTLE (BETA)", mode.getName());
    }

    @Test
    void modeInitAllocatesArrays() throws Exception {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        mode.modeInit(new GameManager(new EventReceiver()));

        assertEquals(2, ((int[]) readField(mode, "ojamaCountdown")).length);
        assertEquals(2, ((int[]) readField(mode, "dropSet")).length);
        assertEquals(2, ((double[]) readField(mode, "attackMultiplier")).length);
        assertEquals(2, ((boolean[]) readField(mode, "countdownDecremented")).length);
    }

	@Test
	void playerInitSetsDefaults() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.owner.modeConfig = new CustomProperties();

		mode.playerInit(engine, 0);

		// playerInit sets numColors[0]=4, but then loadOtherSetting defaults to 5
		assertEquals(5, readInt(mode, "numColors", 0));
		assertEquals(4, readInt(mode, "ojamaHard", 0));
		assertTrue(readBoolean(mode, "countdownDecremented", 0));
		assertFalse(readBoolean(mode, "ojamaChecked", 0));
	}

    @Test
    void loadOtherSettingAppliesDefaults() throws Exception {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        invokeLoadOtherSetting(mode, engine, new CustomProperties());

        assertEquals(120, readInt(mode, "ojamaRate", 0));
        assertEquals(4, readInt(mode, "ojamaHard", 0));
        assertEquals(3, readInt(mode, "ojamaCountdown", 0));
        assertEquals(4, readInt(mode, "dropSet", 0));
        assertEquals(0, readInt(mode, "dropMap", 0));
    }

    @Test
    void loadOtherSettingReadsCustomValues() throws Exception {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        CustomProperties prop = new CustomProperties();
        prop.setProperty("avalanchevsspf.ojamaRate.p0", 200);
        prop.setProperty("avalanchevsspf.ojamaCountdown.p0", 7);
        prop.setProperty("avalanchevsspf.dropSet.p0", 2);
        prop.setProperty("avalanchevsspf.dropMap.p0", 3);

        invokeLoadOtherSetting(mode, engine, prop);

        assertEquals(200, readInt(mode, "ojamaRate", 0));
        assertEquals(7, readInt(mode, "ojamaCountdown", 0));
        assertEquals(2, readInt(mode, "dropSet", 0));
        assertEquals(3, readInt(mode, "dropMap", 0));
    }

    @Test
    void saveOtherSettingWritesValues() throws Exception {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        int[] dropSet = (int[]) readField(mode, "dropSet");
        dropSet[0] = 5;
        int[] dropMap = (int[]) readField(mode, "dropMap");
        dropMap[0] = 2;

        CustomProperties prop = new CustomProperties();
        invokeSaveOtherSetting(mode, engine, prop);

        assertEquals(5, prop.getProperty("avalanchevsspf.dropSet.p0", -1));
        assertEquals(2, prop.getProperty("avalanchevsspf.dropMap.p0", -1));
    }

    @Test
    void getAttackMultiplierReturnsTableValue() {
        // DROP_PATTERNS_ATTACK_MULTIPLIERS[4][0] = 1.0
        assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(4, 0), 0.001);
        // DROP_PATTERNS_ATTACK_MULTIPLIERS[0][8] = 0.7
        assertEquals(0.7, AvalancheVSSPFMode.getAttackMultiplier(0, 8), 0.001);
    }

    @Test
    void getAttackMultiplierReturnsOneForOutOfBounds() {
        assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(99, 0), 0.001);
        assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(0, 99), 0.001);
    }

    @Test
    void getDefendMultiplierReturnsTableValue() {
        // DROP_PATTERNS_DEFEND_MULTIPLIERS[1][8] = 1.2
        assertEquals(1.2, AvalancheVSSPFMode.getDefendMultiplier(1, 8), 0.001);
        assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(0, 0), 0.001);
    }

    @Test
    void getDefendMultiplierReturnsOneForOutOfBounds() {
        assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(99, 0), 0.001);
    }

    @Test
    void onMoveResetsFlags() throws Exception {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = true;
        boolean[] ojamaDrop = (boolean[]) readField(mode, "ojamaDrop");
        ojamaDrop[0] = true;
        boolean[] countdownDecremented = (boolean[]) readField(mode, "countdownDecremented");
        countdownDecremented[0] = true;

        mode.onMove(engine, 0);

        assertFalse(cleared[0]);
        assertFalse(ojamaDrop[0]);
        assertFalse(countdownDecremented[0]);
    }

    @Test
    void onClearResetsOjamaChecked() throws Exception {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        boolean[] ojamaChecked = (boolean[]) readField(mode, "ojamaChecked");
        ojamaChecked[0] = true;

        mode.onClear(engine, 0);

        assertFalse(ojamaChecked[0]);
    }

    @Test
    void readyInitSetsBlockColorsAndDropPattern() throws Exception {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        mode.readyInit(engine, 0);

		assertEquals(4, engine.numColors);
		// Default drop set 4, drop map 0 should have a non-null pattern
		// dropPattern is int[][][], so cast as 3D array
		int[][][] dropPattern = (int[][][]) readField(mode, "dropPattern");
		assertNotNull(dropPattern[0]);
    }

    @Test
    void lineClearEndDecrementsCountdown() throws Exception {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        // Place a block with countdown
        engine.field.setBlockColor(0, 0, 1);
        engine.field.getBlock(0, 0).countdown = 3;

        // Set up state so we can call lineClearEnd without triggering ojama drop
        boolean[] ojamaDrop = (boolean[]) readField(mode, "ojamaDrop");
        ojamaDrop[0] = true;
        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = true;
        boolean[] countdownDecremented = (boolean[]) readField(mode, "countdownDecremented");
        countdownDecremented[0] = false;
        int[] ojamaCountdown = (int[]) readField(mode, "ojamaCountdown");
        ojamaCountdown[0] = 3; // Not 10, so countdown is processed

        mode.lineClearEnd(engine, 0);

        assertEquals(2, engine.field.getBlock(0, 0).countdown);
    }

    @Test
    void lineClearEndChecksGameOver() throws Exception {
        AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        // Fill column 2 top
        engine.field.setBlockColor(2, 0, 1);

        // Prevent ojama drop
        boolean[] ojamaDrop = (boolean[]) readField(mode, "ojamaDrop");
        ojamaDrop[0] = true;
        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = true;

        boolean[] countdownDecremented = (boolean[]) readField(mode, "countdownDecremented");
        countdownDecremented[0] = true;

        mode.lineClearEnd(engine, 0);

        assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
    }

	@Test
	void saveReplayDoesNotThrow() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		// saveReplay writes to owner.replayProp, not the passed prop
		mode.saveReplay(engine, 0, engine.owner.replayProp);

		assertEquals(0, engine.owner.replayProp.getProperty("avalanchevs.version", -1));
	}

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static GameEngine freshEngine(AvalancheVSSPFMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[0].createFieldIfNeeded();
        manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
        return manager.engine[0];
    }

    private static Object readField(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.get(obj);
    }

    private static int readInt(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.getInt(obj);
    }

    private static int readInt(Object obj, String name, int index) throws Exception {
        int[] arr = (int[]) readField(obj, name);
        return arr[index];
    }

    private static boolean readBoolean(Object obj, String name, int index) throws Exception {
        boolean[] arr = (boolean[]) readField(obj, name);
        return arr[index];
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

    private static void invokeLoadOtherSetting(AvalancheVSSPFMode mode, GameEngine engine,
            CustomProperties prop) throws Exception {
        java.lang.reflect.Method m = AvalancheVSSPFMode.class.getDeclaredMethod(
                "loadOtherSetting", GameEngine.class, CustomProperties.class);
        m.setAccessible(true);
        m.invoke(mode, engine, prop);
    }

    private static void invokeSaveOtherSetting(AvalancheVSSPFMode mode, GameEngine engine,
            CustomProperties prop) throws Exception {
        java.lang.reflect.Method m = AvalancheVSSPFMode.class.getDeclaredMethod(
                "saveOtherSetting", GameEngine.class, CustomProperties.class);
        m.setAccessible(true);
        m.invoke(mode, engine, prop);
    }
}
