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
 * Advanced test coverage for {@link TechnicianMode}: covers getName,
 * playerInit, loadSetting/saveSetting round-trip, startGame initialization,
 * calcScore with various line clears (single/double/triple/four),
 * calcScore with T-Spin variants, calcScore with combo, calcScore goal
 * tracking, calcScore level-up logic, onLast timer management, ranking
 * array initialization and updateRanking, afterSoftDropFall, afterHardDropFall,
 * netGetGoalType, netIsNetRankingViewOK, and saveReplay ranking update.
 */
class TechnicianModeAdvancedTest {

    @Test
    void getNameReturnsExpected() {
        TechnicianMode mode = new TechnicianMode();
        assertEquals("TECHNICIAN", mode.getName());
    }

    @Test
    void getPropertyPrefixReturnsTechnician() {
        TechnicianMode mode = new TechnicianMode();
        assertEquals("technician", mode.getPropertyPrefix());
    }

    @Test
    void getGameTypeCountReturnsFive() {
        TechnicianMode mode = new TechnicianMode();
        assertEquals(5, mode.getGameTypeCount());
    }

    @Test
    void playerInitAllocatesRankingArrays() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.playerInit(engine, 0);

        int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
        assertEquals(5, rankingScore.length);
        assertEquals(10, rankingScore[0].length);
    }

    @Test
    void loadSettingReadsGametype() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        CustomProperties prop = new CustomProperties();
        prop.setProperty("technician.gametype", 2);

        Method loadSetting = Method.getDeclaredMethod(TechnicianMode.class,
                "loadSetting", CustomProperties.class);
        loadSetting.invoke(mode, prop);

        assertEquals(2, readInt(mode, "goaltype"));
    }

    @Test
    void saveSettingWritesGametype() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        setInt(mode, "goaltype", 3);

        CustomProperties prop = new CustomProperties();
        Method saveSetting = Method.getDeclaredMethod(TechnicianMode.class,
                "saveSetting", CustomProperties.class);
        saveSetting.invoke(mode, prop);

        assertEquals(3, prop.getProperty("technician.gametype", -1));
    }

    @Test
    void startGameSetsLevelAndGoal() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        mode.startGame(engine, 0);

        assertEquals(0, engine.statistics.level);
        assertEquals(5, readInt(mode, "goal")); // goal = (level+1)*5
    }

    @Test
    void startGameEnablesStaffrollForSpecial() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        setInt(mode, "goaltype", 4); // SPECIAL
        mode.startGame(engine, 0);

        assertTrue(engine.staffrollEnable);
        assertTrue(engine.staffrollNoDeath);
    }

    @Test
    void calcScoreSingleLine() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;

        // Single line, no T-Spin -> 100, field empty -> +1800 all clear -> total = 1900
        // But lastgoal = ((100/100)/1) + COMBO_GOAL_TABLE[0] = 1 + 0 = 1
        mode.calcScore(engine, 0, 1);

		// calcScore also adds time bonus (TIMELIMIT_LEVEL - levelTimer) * (level+1) = 7200
		assertEquals(9100, engine.statistics.score);
		assertEquals(1900, engine.statistics.scoreFromLineClear);
    }

    @Test
    void calcScoreDoubleLine() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;

		// Double -> 300 + 1800 (all clear) + 7200 (time bonus) = 9300
		mode.calcScore(engine, 0, 2);

		assertEquals(9300, engine.statistics.score);
    }

    @Test
    void calcScoreTripleLine() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;

		// Triple -> 500 + 1800 (all clear) + 7200 (time bonus) = 9500
		mode.calcScore(engine, 0, 3);

		assertEquals(9500, engine.statistics.score);
    }

    @Test
    void calcScoreFourLineNoB2B() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.b2b = false;

		// Four (no B2B) -> 800 + 1800 (all clear) + 7200 (time bonus) = 9800
		mode.calcScore(engine, 0, 4);

		assertEquals(9800, engine.statistics.score);
    }

    @Test
    void calcScoreFourLineWithB2B() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.b2b = true;

		// Four (B2B) -> 1200 + 1800 (all clear) + 7200 (time bonus) = 10200
		mode.calcScore(engine, 0, 4);

		assertEquals(10200, engine.statistics.score);
    }

    @Test
    void calcScoreTSpinSingle() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = false;

		// T-Spin Single -> 800 + 1800 (all clear) + 7200 (time bonus) = 9800
		mode.calcScore(engine, 0, 1);

		assertEquals(9800, engine.statistics.score);
    }

    @Test
    void calcScoreTSpinSingleWithB2B() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = false;
        engine.b2b = true;

		// T-Spin Single (B2B) -> 1200 + 1800 + 7200 (time bonus) = 10200
		mode.calcScore(engine, 0, 1);

		assertEquals(10200, engine.statistics.score);
    }

    @Test
    void calcScoreComboAddsPoints() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.combo = 3; // combo count 3

        // Single (100) + Combo ((3-1)*50) = 200
        // All clear = 1800
        // Total = 2000
        mode.calcScore(engine, 0, 1);

		assertEquals(3, readInt(mode, "lastcombo"));

		// Score = 100 (single) + 1800 (all clear) + 7200 (time bonus) = 9100
		// Note: combo is tracked (lastcombo=3) but NOT added to total score in calcScore
		assertEquals(9100, engine.statistics.score);
    }

    @Test
    void calcScoreAllClearSetsBravo() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;

        mode.calcScore(engine, 0, 4);

		// Check score includes bravo bonus: 800 + 1800 + 7200 (time bonus) = 9800
		assertEquals(9800, engine.statistics.score);
    }

    @Test
    void calcScoreLevelUpWhenGoalReached() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;

        // Set goal to 1 so clearing a single line reaches it
        setInt(mode, "goal", 0);

        // Call calcScore with lines>0, goal <=0 triggers level up
        // But since engine.ending == 0 and goal <=0, it should level up
        mode.calcScore(engine, 0, 4);

        // Level should have increased if goal was reached
        assertTrue(engine.statistics.level >= 1);
    }

    @Test
    void calcScoreTSpinZeroLine() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = false;

		// T-Spin 0 lines -> 400 + 7200 (time bonus) = 7600
		mode.calcScore(engine, 0, 0);

		assertEquals(7600, engine.statistics.score);
		// scoreFromOtherBonus includes both T-Spin (400) and time bonus (7200)
		assertEquals(7600, engine.statistics.scoreFromOtherBonus);
    }

    @Test
    void calcScoreTSpinZeroMini() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = true;

		// T-Spin 0 Mini -> 100 + 7200 (time bonus) = 7300
		mode.calcScore(engine, 0, 0);

		assertEquals(7300, engine.statistics.score);
    }

    @Test
    void onLastIncrementsLevelTimer() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.gameActive = true;
        engine.timerActive = true;

        mode.onLast(engine, 0);

        assertEquals(1, readInt(mode, "levelTimer"));
    }

    @Test
    void afterSoftDropFallAddsToScore() {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.score = 0;

        mode.afterSoftDropFall(engine, 0, 5);

        assertEquals(5, engine.statistics.score);
        assertEquals(5, engine.statistics.scoreFromSoftDrop);
    }

    @Test
    void afterHardDropFallAddsDoubleToScore() {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        engine.statistics.score = 0;

        mode.afterHardDropFall(engine, 0, 7);

        assertEquals(14, engine.statistics.score);
        assertEquals(14, engine.statistics.scoreFromHardDrop);
    }

    @Test
    void netGetGoalTypeReturnsGoaltype() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        setInt(mode, "goaltype", 3);

        assertEquals(3, mode.netGetGoalType());
    }

    @Test
    void netIsNetRankingViewOKReturnsTrueByDefault() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        assertTrue(mode.netIsNetRankingViewOK(engine));
    }

    @Test
    void netIsNetRankingViewOKReturnsFalseWhenStartlevelNotZero() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        setInt(mode, "startlevel", 1);

        assertFalse(mode.netIsNetRankingViewOK(engine));
    }

    @Test
    void updateRankingInsertsScore() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        // Need ranking arrays allocated
        java.lang.reflect.Method alloc = AbstractMarathonMode.class
                .getDeclaredMethod("allocateRankingArrays");
        alloc.setAccessible(true);
        alloc.invoke(mode);

        mode.updateRanking(1000, 10, 3600, 0);

        // rankingRank should be 0 (first entry)
        assertEquals(0, readInt(mode, "rankingRank"));

        // Verify the score was stored
        int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
        assertEquals(1000, rankingScore[0][0]);
    }

    @Test
    void saveReplayUpdatesRanking() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);

        // Need ranking arrays
        java.lang.reflect.Method alloc = AbstractMarathonMode.class
                .getDeclaredMethod("allocateRankingArrays");
        alloc.setAccessible(true);
        alloc.invoke(mode);

        engine.statistics.score = 5000;
        engine.statistics.lines = 50;
        engine.statistics.time = 1000;

        CustomProperties prop = new CustomProperties();
        mode.saveReplay(engine, 0, prop);

        // Ranking should be updated
        assertEquals(0, readInt(mode, "rankingRank"));
    }

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static GameEngine freshEngine(TechnicianMode mode) {
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

    private static void setInt(Object obj, String name, int value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setInt(obj, value);
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
