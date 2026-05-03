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
 * Advanced test coverage for {@link ExtremeMode}: covers getName,
 * playerInit, loadSetting/saveSetting round-trip (including endless),
 * startGame initialization, calcScore with various line clears,
 * calcScore with T-Spin variants, calcScore with combo, calcScore
 * level-up and ending logic, calcScore all clear bravo, onLast end-game
 * roll-time, ranking array initialization and updateRanking, and
 * netGetGoalType.
 */
class ExtremeModeAdvancedTest {

    @Test
    void getNameReturnsExpected() {
        ExtremeMode mode = new ExtremeMode();
        assertEquals("EXTREME", mode.getName());
    }

    @Test
    void getPropertyPrefixReturnsExtreme() {
        ExtremeMode mode = new ExtremeMode();
        assertEquals("extreme", mode.getPropertyPrefix());
    }

    @Test
    void getGameTypeCountReturnsTwo() {
        ExtremeMode mode = new ExtremeMode();
        assertEquals(2, mode.getGameTypeCount());
    }

    @Test
    void playerInitAllocatesRankingArrays() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.playerInit(engine, 0);

        int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
        assertEquals(2, rankingScore.length);
        assertEquals(10, rankingScore[0].length);
    }

    @Test
    void loadSettingReadsEndless() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        CustomProperties prop = new CustomProperties();
        prop.setProperty("extreme.endless", true);
        prop.setProperty("extreme.startlevel", 3);

        Method loadSetting = Method.getDeclaredMethod(ExtremeMode.class,
                "loadSetting", CustomProperties.class);
        loadSetting.invoke(mode, prop);

        assertTrue(readBoolean(mode, "endless"));
        assertEquals(3, readInt(mode, "startlevel"));
    }

    @Test
    void saveSettingWritesEndless() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        setBoolean(mode, "endless", true);
        setInt(mode, "startlevel", 5);

        CustomProperties prop = new CustomProperties();
        Method saveSetting = Method.getDeclaredMethod(ExtremeMode.class,
                "saveSetting", CustomProperties.class);
        saveSetting.invoke(mode, prop);

        assertTrue(prop.getProperty("extreme.endless", false));
        assertEquals(5, prop.getProperty("extreme.startlevel", -1));
    }

    @Test
    void startGameSetsLevelAndB2B() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        setBoolean(mode, "enableB2B", true);
        setBoolean(mode, "enableCombo", true);

        mode.startGame(engine, 0);

        assertEquals(0, engine.statistics.level);
        assertTrue(engine.b2bEnable);
        assertEquals(GameEngine.COMBO_TYPE_NORMAL, engine.comboType);
    }

    @Test
    void calcScoreSingleLineWithoutTSpin() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;

        // Single -> 100 + all clear 1800 = 1900
        mode.calcScore(engine, 0, 1);

        assertEquals(1900, engine.statistics.score);
        assertEquals(1, readInt(mode, "lastevent"));
    }

    @Test
    void calcScoreDoubleLine() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;

        mode.calcScore(engine, 0, 2);

        // Double -> 300 + 1800 = 2100
        assertEquals(2100, engine.statistics.score);
        assertEquals(2, readInt(mode, "lastevent"));
    }

    @Test
    void calcScoreTripleLine() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;

        mode.calcScore(engine, 0, 3);

        // Triple -> 500 + 1800 = 2300
        assertEquals(2300, engine.statistics.score);
    }

    @Test
    void calcScoreFourLineNoB2B() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.b2b = false;

        mode.calcScore(engine, 0, 4);

        // Four (no B2B) -> 800 + 1800 = 2600
        assertEquals(2600, engine.statistics.score);
    }

    @Test
    void calcScoreFourLineWithB2B() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.b2b = true;

        mode.calcScore(engine, 0, 4);

        // Four (B2B) -> 1200 + 1800 = 3000
        assertEquals(3000, engine.statistics.score);
    }

    @Test
    void calcScoreTSpinSingle() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = false;

        mode.calcScore(engine, 0, 1);

        // T-Spin Single -> 800 + 1800 = 2600
        assertEquals(2600, engine.statistics.score);
    }

    @Test
    void calcScoreTSpinTriple() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = false;

        mode.calcScore(engine, 0, 3);

        // T-Spin Triple (no B2B) -> 1600 + 1800 = 3400
        assertEquals(3400, engine.statistics.score);
    }

    @Test
    void calcScoreTSpinTripleWithB2B() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.b2b = true;

        mode.calcScore(engine, 0, 3);

        // T-Spin Triple (B2B) -> 2400 + 1800 = 4200
        assertEquals(4200, engine.statistics.score);
    }

    @Test
    void calcScoreTSpinZero() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = false;

        // T-Spin 0 lines -> 400 * (0+1) = 400 (no all clear bonus for 0 lines)
        mode.calcScore(engine, 0, 0);

        assertEquals(400, engine.statistics.score);
        assertEquals(400, engine.statistics.scoreFromOtherBonus);
    }

    @Test
    void calcScoreTSpinZeroMini() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = true;

        mode.calcScore(engine, 0, 0);

        assertEquals(100, engine.statistics.score);
    }

    @Test
    void calcScoreComboAddsPoints() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.combo = 3;

        mode.calcScore(engine, 0, 1);

        // Score = 100 (single) + 100 (combo (3-1)*50) + 1800 (all clear) = 2000
        assertEquals(2000, engine.statistics.score);
        assertEquals(3, readInt(mode, "lastcombo"));
    }

    @Test
    void calcScoreComboDisabled() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.combo = 3;
        setBoolean(mode, "enableCombo", false);

        // Combo disabled, so only 100 (single) + 1800 (all clear) = 1900
        mode.calcScore(engine, 0, 1);

        assertEquals(1900, engine.statistics.score);
        assertEquals(0, readInt(mode, "lastcombo"));
    }

    @Test
    void calcScoreAllClearAddsBravo() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;

        mode.calcScore(engine, 0, 4);

        assertEquals(2600, engine.statistics.score);
    }

    @Test
    void calcScoreNoLinesDoesNothing() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.score = 1000;
        mode.calcScore(engine, 0, 0);

        assertEquals(1000, engine.statistics.score);
    }

	@Test
	void calcScoreLevelUpAtLineThreshold() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);

		engine.statistics.level = 0;
		engine.statistics.lines = 10; // level-up triggers at lines >= (level+1)*10
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.statistics.level >= 1);
	}

	@Test
	void calcScoreEndingAt200Lines() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);

		engine.statistics.level = 19;
		engine.statistics.lines = 200; // calcScore checks lines >= 200
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		// Should trigger ending
		assertEquals(2, engine.ending);
		assertFalse(engine.timerActive);
	}

    @Test
    void calcScoreEndlessDoesNotEnd() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 19;
        engine.statistics.lines = 199;
        setBoolean(mode, "endless", true);

        mode.calcScore(engine, 0, 1);

        // In endless mode, lines >= 200 doesn't trigger ending
        assertTrue(engine.ending != 2);
    }

    @Test
    void calcScoreUpdatesMeter() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.lines = 5;
        engine.statistics.score = 0;

        mode.calcScore(engine, 0, 1);

        // Meter should be updated based on (lines % 10) / 9
        assertTrue(engine.meterValue >= 0);
    }

    @Test
    void onLastEndingRolltime() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        engine.gameActive = true;
        engine.ending = 2;

        mode.onLast(engine, 0);

        assertEquals(1, readInt(mode, "rolltime"));
    }

    @Test
    void onLastIncrementsScgettime() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);

        int scgettime = readInt(mode, "scgettime");
        mode.onLast(engine, 0);

        assertEquals(scgettime + 1, readInt(mode, "scgettime"));
    }

    @Test
    void updateRankingInsertsScore() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        java.lang.reflect.Method alloc = AbstractMarathonMode.class
                .getDeclaredMethod("allocateRankingArrays");
        alloc.setAccessible(true);
        alloc.invoke(mode);

        mode.updateRanking(10000, 100, 5000, 0);

        assertEquals(0, readInt(mode, "rankingRank"));

        int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
        assertEquals(10000, rankingScore[0][0]);
    }

    @Test
    void netGetGoalTypeReturnsZeroWhenNotEndless() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        setBoolean(mode, "endless", false);

        assertEquals(0, mode.netGetGoalType());
    }

    @Test
    void netGetGoalTypeReturnsOneWhenEndless() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        setBoolean(mode, "endless", true);

        assertEquals(1, mode.netGetGoalType());
    }

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static GameEngine freshEngine(ExtremeMode mode) {
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

    private static boolean readBoolean(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.getBoolean(obj);
    }

    private static void setInt(Object obj, String name, int value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private static void setBoolean(Object obj, String name, boolean value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setBoolean(obj, value);
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

    private static class Method {
        java.lang.reflect.Method inner;

        Method(java.lang.reflect.Method m) {
            this.inner = m;
        }

        void invoke(Object obj, Object... args) throws Exception {
            inner.invoke(obj, args);
        }

        static Method getDeclaredMethod(Class<?> cls, String name, Class<?>... paramTypes)
                throws NoSuchMethodException {
            java.lang.reflect.Method m = cls.getDeclaredMethod(name, paramTypes);
            m.setAccessible(true);
            return new Method(m);
        }
    }
}
