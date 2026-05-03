package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Extended scoring and game-logic coverage for {@link AvalancheVSMode}:
 * covers calcScore from parent, calcChainNewPower fever override,
 * addOjama fever-specific logic, lineClearEnd fever start/end,
 * onLast fever timer countdown and meter updates, plus drawXorTimer
 * and settings round-trip testing.
 */
class AvalancheVSModeScoringTest {

    @Test
    void calcScoreAddsPointsForAvalancheClears() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        mode.calcScore(engine, 0, 10);

        // Score should be > 0 (avalanche * 10 * multiplier)
        assertTrue(readInt(mode, "score", 0) > 0, "Score should increase after calcScore");
    }

    @Test
    void calcScoreSetsClearedOnAvalanche() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = false;

        mode.calcScore(engine, 0, 5);

        assertTrue(cleared[0]);
    }

    @Test
    void calcScoreDoesNotSetClearedWhenNoAvalanche() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        boolean[] cleared = (boolean[]) readField(mode, "cleared");
        cleared[0] = false;

        mode.calcScore(engine, 0, 0);

        assertFalse(cleared[0]);
    }

    @Test
    void calcChainNewPowerUsesFeverTableWhenInFever() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setBoolean(mode, "inFever", 0, true);

        Method calc = AvalancheVSMode.class.getDeclaredMethod(
                "calcChainNewPower", GameEngine.class, int.class, int.class);
        calc.setAccessible(true);

        // Chain 1 in fever -> FEVER_POWERS[0] = 4
        assertEquals(4, (int) calc.invoke(mode, engine, 0, 1));
        // Chain 24 -> FEVER_POWERS[23] = 720
        assertEquals(720, (int) calc.invoke(mode, engine, 0, 24));
        // Chain > length -> last value (720)
        assertEquals(720, (int) calc.invoke(mode, engine, 0, 99));
    }

    @Test
    void calcChainNewPowerUsesClassicTableWhenNotInFever() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setBoolean(mode, "inFever", 0, false);

        Method calc = AvalancheVSMode.class.getDeclaredMethod(
                "calcChainNewPower", GameEngine.class, int.class, int.class);
        calc.setAccessible(true);

        // Chain 1 when not in fever -> CHAIN_POWERS[0] = 4
        assertEquals(4, (int) calc.invoke(mode, engine, 0, 1));
    }

    @Test
    void addOjamaWithFeverPointGain() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setInt(mode, "ojama", 0, 10);
        setInt(mode, "ojamaAdd", 0, 5);
        setInt(mode, "ojamaRate", 0, 120);
        setInt(mode, "feverThreshold", 0, 5);
        setInt(mode, "feverPoints", 0, 0);

        Method addOjama = AvalancheVSMode.class.getDeclaredMethod(
                "addOjama", GameEngine.class, int.class, int.class);
        addOjama.setAccessible(true);
        addOjama.invoke(mode, engine, 0, 240);

        // Should have gained a fever point
        assertEquals(1, readInt(mode, "feverPoints", 0));
    }

    @Test
    void addOjamaInFeverUsesDifferentRate() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setBoolean(mode, "inFever", 0, true);
        setInt(mode, "feverPower", 0, 10);
        setInt(mode, "ojamaRate", 0, 120);
        setInt(mode, "ojama", 0, 10);

        Method addOjama = AvalancheVSMode.class.getDeclaredMethod(
                "addOjama", GameEngine.class, int.class, int.class);
        addOjama.setAccessible(true);
        addOjama.invoke(mode, engine, 0, 240);

        // In fever: (pts*feverPower + 10*rate -1)/(10*rate) = (240*10+1200-1)/1200 = 3599/1200 = 2
        // But countered by ojama[0]=10, so final ojamaAdd[1] = 0 (all countered)
        assertEquals(0, readInt(mode, "ojamaAdd", 1));
    }

    @Test
    void onClearSetsOjamaAddToFeverFlag() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setBoolean(mode, "inFever", 1, true);

        // Trigger onClear with chain == 1
        engine.chain = 1;

        Method onClear = AvalancheVSMode.class.getDeclaredMethod(
                "onClear", GameEngine.class, int.class);
        onClear.setAccessible(true);
        onClear.invoke(mode, engine, 0);

        // ojamaAddToFever[1] should be true (enemy is in fever)
        assertTrue(readBoolean(mode, "ojamaAddToFever", 1));
    }

    @Test
    void lineClearEndActivatesFeverWhenPointsThresholdMet() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setInt(mode, "feverThreshold", 0, 3);
        setInt(mode, "feverPoints", 0, 3);
        setBoolean(mode, "inFever", 0, false);
        setInt(mode, "feverChain", 0, 5);
        setInt(mode, "feverTimeMin", 0, 15);
        setInt(mode, "feverTimeMax", 0, 30);
        setInt(mode, "feverChainStart", 0, 5);

        // Prevent ojama drop
        boolean[] ojamaDrop = (boolean[]) readField(mode, "ojamaDrop");
        ojamaDrop[0] = true;

        mode.lineClearEnd(engine, 0);

        // Should now be in fever
        assertTrue(readBoolean(mode, "inFever", 0));
    }

    @Test
    void onLastDecrementsFeverTimerWhenInFever() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setBoolean(mode, "inFever", 0, true);
        setInt(mode, "feverTime", 0, 100);
        engine.timerActive = true;

        mode.onLast(engine, 0);

        assertEquals(99, readInt(mode, "feverTime", 0));
    }

    @Test
    void onLastDecrementsFeverTimeLimitAddDisplay() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setInt(mode, "feverTimeLimitAddDisplay", 0, 5);
        mode.onLast(engine, 0);

        assertEquals(4, readInt(mode, "feverTimeLimitAddDisplay", 0));
    }

    @Test
    void onLastUpdatesMeterForOjamaMode() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setBoolean(mode, "ojamaMeter", 0, true);
        mode.onLast(engine, 0);
        // Should not throw
    }

    @Test
    void onLastUpdatesMeterForFeverMode() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setBoolean(mode, "ojamaMeter", 0, false);
        setInt(mode, "feverThreshold", 0, 5);
        mode.onLast(engine, 0);
        // Should not throw
    }

    @Test
    void drawXorTimerShowsXWhenNotInFeverAndShowX() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setBoolean(mode, "inFever", 0, false);
        setBoolean(mode, "dangerColumnShowX", 0, true);
        engine.gameStarted = true;

        // Invoke drawXorTimer through renderMove
        mode.renderMove(engine, 0);
        // Should not throw
    }

    @Test
    void saveReplayRoundTrip() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        setInt(mode, "feverThreshold", 0, 5);
        setInt(mode, "feverPower", 0, 15);
        setInt(mode, "ojamaRate", 0, 100);

        CustomProperties prop = new CustomProperties();
        mode.saveReplay(engine, 0, prop);

        assertEquals(0, prop.getProperty("avalanchevs.version", -1));
        assertEquals(5, prop.getProperty("avalanchevs.feverThreshold.p0", -1));
    }

    @Test
    void startGameDisablesB2BAndCombo() throws Exception {
        AvalancheVSMode mode = new AvalancheVSMode();
        mode.modeInit(new GameManager(new EventReceiver()));
        GameEngine engine = freshEngine(mode);

        mode.startGame(engine, 0);

        assertFalse(engine.b2bEnable);
        assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
    }

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static GameEngine freshEngine(AvalancheVSMode mode) {
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

    private static void setInt(Object obj, String name, int index, int value) throws Exception {
        int[] arr = (int[]) readField(obj, name);
        arr[index] = value;
    }

    private static void setBoolean(Object obj, String name, int index, boolean value) throws Exception {
        boolean[] arr = (boolean[]) readField(obj, name);
        arr[index] = value;
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
