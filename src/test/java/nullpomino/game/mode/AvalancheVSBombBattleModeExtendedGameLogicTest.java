package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link AvalancheVSBombBattleMode}: covers getName,
 * playerInit initialization, loadOtherSetting/saveOtherSetting round-trip,
 * lineClearEnd countdown explosion chain, gameOverCheck, onLast updateOjamaMeter,
 * and saveReplay.
 */
class AvalancheVSBombBattleModeExtendedGameLogicTest {

    @Test
    void getNameReturnsExpected() {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        assertEquals("AVALANCHE VS BOMB BATTLE (RC1)", mode.getName());
    }

    @Test
    void modeInitAllocatesArrays() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));

        assertEquals(2, ((int[]) readField(mode, "ojamaCountdown")).length);
        assertEquals(2, ((boolean[]) readField(mode, "newChainPower")).length);
    }

    @Test
    void playerInitLoadsSettings() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.playerInit(engine, 0);

        assertEquals(0, readInt(mode, "menuTime"));
        assertEquals(0, readInt(mode, "menuCursor"));
    }

    @Test
    void loadOtherSettingAppliesDefaults() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        invokeLoadOtherSetting(mode, engine, new CustomProperties());

        assertEquals(60, readInt(mode, "ojamaRate", 0));
        assertEquals(1, readInt(mode, "ojamaHard", 0));
        assertFalse(readBoolean(mode, "newChainPower", 0));
        assertEquals(5, readInt(mode, "ojamaCountdown", 0));
    }

    @Test
    void loadOtherSettingReadsCustomValues() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        CustomProperties prop = new CustomProperties();
        prop.setProperty("avalanchevsbombbattle.ojamaRate.p0", 100);
        prop.setProperty("avalanchevsbombbattle.ojamaHard.p0", 3);
        prop.setProperty("avalanchevsbombbattle.newChainPower.p0", true);
        prop.setProperty("avalanchevsbombbattle.ojamaCountdown.p0", 8);

        invokeLoadOtherSetting(mode, engine, prop);

        assertEquals(100, readInt(mode, "ojamaRate", 0));
        assertEquals(3, readInt(mode, "ojamaHard", 0));
        assertTrue(readBoolean(mode, "newChainPower", 0));
        assertEquals(8, readInt(mode, "ojamaCountdown", 0));
    }

    @Test
    void saveOtherSettingWritesValues() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        boolean[] newChainPower = (boolean[]) readField(mode, "newChainPower");
        newChainPower[0] = true;
        int[] ojamaCountdown = (int[]) readField(mode, "ojamaCountdown");
        ojamaCountdown[0] = 9;

        CustomProperties prop = new CustomProperties();
        invokeSaveOtherSetting(mode, engine, prop);

        assertTrue(prop.getProperty("avalanchevsbombbattle.newChainPower.p0", false));
        assertEquals(9, prop.getProperty("avalanchevsbombbattle.ojamaCountdown.p0", -1));
    }

    @Test
    void lineClearEndTransfersOjamaAdd() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        int[] ojamaAdd = (int[]) readField(mode, "ojamaAdd");
        ojamaAdd[1] = 12; // 2 full rows of 6

        mode.lineClearEnd(engine, 0);

        // ojama[1] should have been increased by 12, ojamaAdd[1] reset to 0
        assertEquals(12, readInt(mode, "ojama", 1));
        assertEquals(0, readInt(mode, "ojamaAdd", 1));
    }

    @Test
    void lineClearEndDropsOjamaWhenAtLeastSix() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        int[] ojama = (int[]) readField(mode, "ojama");
        ojama[0] = 12; // 2 rows
        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = false;
        int[] ojamaCounterMode = (int[]) readField(mode, "ojamaCounterMode");
        ojamaCounterMode[0] = AvalancheVSDummyMode.OJAMA_COUNTER_ON;

        mode.lineClearEnd(engine, 0);

        // ojama[0] should have been reduced
        assertTrue(readInt(mode, "ojama", 0) < 12);
    }

    @Test
    void lineClearEndDecrementsCountdownAndExplodes() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        // Place a block with countdown 1 (about to explode)
        engine.field.setBlockColor(4, 4, 1);
        engine.field.getBlock(4, 4).countdown = 1;

        // Prevent ojama drop path
        boolean[] ojamaDrop = (boolean[]) readField(mode, "ojamaDrop");
        ojamaDrop[0] = true;
        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = true;
        int[] ojama = (int[]) readField(mode, "ojama");
        ojama[0] = 0;

        mode.lineClearEnd(engine, 0);

        // The block should have exploded (countdown 0, turned to garbage)
        assertTrue(engine.field.getBlock(4, 4).getAttribute(nullpomino.game.component.Block.BLOCK_ATTRIBUTE_GARBAGE));
    }

    @Test
    void lineClearEndGameOverOnColumnBlocked() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        // Block column 2 top
        engine.field.setBlockColor(2, 0, 1);

        // Prevent ojama drop path
        boolean[] ojamaDrop = (boolean[]) readField(mode, "ojamaDrop");
        ojamaDrop[0] = true;
        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = true;
        int[] ojama = (int[]) readField(mode, "ojama");
        ojama[0] = 0;

        mode.lineClearEnd(engine, 0);

        assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
    }

    @Test
    void onLastCallsSuperAndUpdatesOjamaMeter() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        int[] scgettime = (int[]) readField(mode, "scgettime");
        scgettime[0] = 15;

        mode.onLast(engine, 0);

        assertEquals(14, scgettime[0]);
    }

    @Test
    void onLastDoesNotThrowWhenEngineInactive() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        engine.gameActive = false;
        mode.onLast(engine, 0);
        // Should not throw
    }

    @Test
    void saveReplayDoesNotThrow() throws Exception {
        AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        CustomProperties prop = new CustomProperties();
        mode.saveReplay(engine, 0, prop);

        assertEquals(0, prop.getProperty("avalanchevs.version", -1));
    }

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static GameEngine freshEngine(AvalancheVSBombBattleMode mode) {
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

    private static void invokeLoadOtherSetting(AvalancheVSBombBattleMode mode, GameEngine engine,
            CustomProperties prop) throws Exception {
        java.lang.reflect.Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod(
                "loadOtherSetting", GameEngine.class, CustomProperties.class);
        m.setAccessible(true);
        m.invoke(mode, engine, prop);
    }

    private static void invokeSaveOtherSetting(AvalancheVSBombBattleMode mode, GameEngine engine,
            CustomProperties prop) throws Exception {
        java.lang.reflect.Method m = AvalancheVSBombBattleMode.class.getDeclaredMethod(
                "saveOtherSetting", GameEngine.class, CustomProperties.class);
        m.setAccessible(true);
        m.invoke(mode, engine, prop);
    }
}
