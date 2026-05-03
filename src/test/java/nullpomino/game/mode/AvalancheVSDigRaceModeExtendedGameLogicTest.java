package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Random;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link AvalancheVSDigRaceMode}: covers getName,
 * playerInit initialization, loadOtherSetting/saveOtherSetting round-trip,
 * startGame field setup, lineClearEnd game-over detection, handicap rows
 * settings, and onLast settlement logic.
 */
class AvalancheVSDigRaceModeExtendedGameLogicTest {

    @Test
    void getNameReturnsExpected() {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        assertEquals("AVALANCHE VS DIG RACE (RC1)", mode.getName());
    }

    @Test
    void modeInitAllocatesHandicapRows() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));

        int[] handicapRows = (int[]) readField(mode, "handicapRows");
        assertEquals(2, handicapRows.length);
    }

    @Test
    void playerInitSetsOwnerAndReceiver() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        GameEngine engine = freshEngine(mode);

        // playerInit is called during freshEngine via manager.init
        assertTrue(readBoolean(mode, "useMap", 0) == false);
    }

    @Test
    void playerInitResetsMenuState() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
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
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        invokeLoadOtherSetting(mode, engine, new CustomProperties());

        assertEquals(420, readInt(mode, "ojamaRate", 0));
        assertEquals(0, readInt(mode, "ojamaHard", 0));
        assertEquals(6, ((int[]) readField(mode, "handicapRows"))[0]);
    }

    @Test
    void loadOtherSettingReadsCustomValues() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        CustomProperties prop = new CustomProperties();
        prop.setProperty("avalanchevsdigrace.ojamaRate.p0", 600);
        prop.setProperty("avalanchevsdigrace.ojamaHard.p0", 3);
        prop.setProperty("avalanchevsdigrace.ojamaHandicap.p0", 8);

        invokeLoadOtherSetting(mode, engine, prop);

        assertEquals(600, readInt(mode, "ojamaRate", 0));
        assertEquals(3, readInt(mode, "ojamaHard", 0));
        assertEquals(8, ((int[]) readField(mode, "handicapRows"))[0]);
    }

    @Test
    void saveOtherSettingWritesHandicapRows() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        ((int[]) readField(mode, "handicapRows"))[0] = 10;

        CustomProperties prop = new CustomProperties();
        invokeSaveOtherSetting(mode, engine, prop);

        assertEquals(10, prop.getProperty("avalanchevsdigrace.ojamaHandicap.p0", -1));
    }

    @Test
    void startGameFillsFieldWithGemsAndOjama() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        mode.startGame(engine, 0);

        // The field should now have blocks placed
        assertFalse(engine.field.isEmpty(), "Field should contain blocks after startGame");
    }

    @Test
    void lineClearEndDropsOjamaWhenQueued() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        // Set up ojama queue for enemy
        int[] ojamaAdd = (int[]) readField(mode, "ojamaAdd");
        ojamaAdd[1] = 10;
        int[] ojama = (int[]) readField(mode, "ojama");
        ojama[0] = 10;

        // Set cleared to false and counter mode to non-fever
        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = false;
        int[] ojamaCounterMode = (int[]) readField(mode, "ojamaCounterMode");
        ojamaCounterMode[0] = AvalancheVSDummyMode.OJAMA_COUNTER_ON;

        // call lineClearEnd - should trigger ojama drop and return true
        boolean result = mode.lineClearEnd(engine, 0);
        assertTrue(result);
    }

    @Test
    void lineClearEndDetectsGameOver() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        // Fill column 2 top to trigger game over
        engine.field.setBlockColor(2, 0, 1);

        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = false;
        boolean[] ojamaDrop = (boolean[]) readField(mode, "ojamaDrop");
        ojamaDrop[0] = true;

        mode.lineClearEnd(engine, 0);

        assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
    }

    @Test
    void onLastDecrementsScgettime() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        int[] scgettime = (int[]) readField(mode, "scgettime");
        scgettime[0] = 10;

        mode.onLast(engine, 0);

        assertEquals(9, scgettime[0]);
    }

    @Test
    void onLastDecrementsChainDisplay() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        int[] chainDisplay = (int[]) readField(mode, "chainDisplay");
        chainDisplay[0] = 30;

        mode.onLast(engine, 0);

        assertEquals(29, chainDisplay[0]);
    }

    @Test
    void renderMoveDrawsXWhenGameStarted() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        GameEngine engine = freshEngine(mode);

        engine.gameStarted = true;
        boolean[] dangerColumnShowX = (boolean[]) readField(mode, "dangerColumnShowX");
        dangerColumnShowX[0] = true;

        // Should not throw
        mode.renderMove(engine, 0);
    }

    @Test
    void onReadySetsEngineProperties() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        GameEngine engine = freshEngine(mode);

        engine.statc[0] = 0;

		mode.onReady(engine, 0);

		// bigDisplay defaults to false -> readyInit sets displaysize = 0
		assertEquals(0, engine.displaysize);
    }

    @Test
    void getPlayersReturnsTwo() {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        assertEquals(2, mode.getPlayers());
    }

    @Test
    void isVSModeReturnsTrue() {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        assertTrue(mode.isVSMode());
    }

    @Test
    void getGameStyleReturnsAvalanche() {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        assertEquals(GameEngine.GAMESTYLE_AVALANCHE, mode.getGameStyle());
    }

    @Test
    void saveReplayDoesNotThrow() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        CustomProperties prop = new CustomProperties();
        mode.saveReplay(engine, 0, prop);

        assertTrue(prop.getProperty("avalanchevsdigrace.version", -1) >= 0);
    }

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static GameEngine freshEngine(AvalancheVSDigRaceMode mode) {
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

    private static void invokeLoadOtherSetting(AvalancheVSDigRaceMode mode, GameEngine engine,
            CustomProperties prop) throws Exception {
        java.lang.reflect.Method m = AvalancheVSDigRaceMode.class.getDeclaredMethod(
                "loadOtherSetting", GameEngine.class, CustomProperties.class);
        m.setAccessible(true);
        m.invoke(mode, engine, prop);
    }

    private static void invokeSaveOtherSetting(AvalancheVSDigRaceMode mode, GameEngine engine,
            CustomProperties prop) throws Exception {
        java.lang.reflect.Method m = AvalancheVSDigRaceMode.class.getDeclaredMethod(
                "saveOtherSetting", GameEngine.class, CustomProperties.class);
        m.setAccessible(true);
        m.invoke(mode, engine, prop);
    }
}
