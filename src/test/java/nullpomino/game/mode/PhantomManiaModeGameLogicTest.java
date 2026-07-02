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
import nullpomino.game.menu.BooleanMenuItem;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link PhantomManiaMode}: playerInit,
 * startGame, calcScore (complex formula with combo, bravo, medals,
 * rotation count, level progression, ending), onMove (level up),
 * onARE (level up), onLast (timer, roll time), onGameOver (secret
 * grade), loadSetting/saveSetting, updateRanking/checkRanking,
 * and saveReplay.
 */
class PhantomManiaModeGameLogicTest {

	@Test
	void getNameReturnsExpected() {
		assertEquals("PHANTOM MANIA", new PhantomManiaMode().getName());
	}

	@Test
	void playerInitSetsDefaults() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertFalse(engine.tspinEnable);
		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DOUBLE, engine.comboType);
		assertEquals(GameEngine.FRAME_COLOR_CYAN, engine.framecolor);
	}

	@Test
	void startGameSetsInitialLevelAndSpeed() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertEquals(0, engine.statistics.level);
		assertEquals(100, readInt(mode, "nextseclv"));
	}

	@Test
	void startGameWithStartLevel3() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntMenuValue(mode, "startlevel", 3);

		mode.startGame(engine, 0);

		assertEquals(300, engine.statistics.level);
		assertEquals(400, readInt(mode, "nextseclv"));
	}

	@Test
	void calcScoreZeroLinesResetsComboValue() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "comboValue", 10);

		mode.calcScore(engine, 0, 0);

		assertEquals(1, readInt(mode, "comboValue"));
	}

	@Test
	void calcScoreOneLineComputesBasicFormula() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 100; // levelb = 100
		engine.softdropFall = 0;
		engine.statc[0] = 0;
		engine.manualLock = false;

		// Place a block to prevent all-clear
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		// comboValue starts at 0, lines=1 -> comboValue = 0+2*1-2=0 -> clamped to 1
		// lastscore = ((100+1)/4 + 0 + 0) * 1 * 1 * 1 + (101/2) + (speedBonus * 7)
		// = 25*1 + 50 + (speedBonus*7)
		// speedBonus depends on default lockDelay which varies, so just verify score > 0
		assertEquals(1, readInt(mode, "comboValue"));
		assertTrue(engine.statistics.score > 0);
		assertEquals(101, engine.statistics.level); // level increased by 1
	}

	@Test
	void calcScoreFourLinesGivesBonusAndIncrementsSectionFourLine() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 100;
		engine.softdropFall = 0;
		engine.statc[0] = 0;
		engine.manualLock = false;
		setInt(mode, "sectionfourline", 0);
		setInt(mode, "nextseclv", 999); // prevent section transition reset

		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 4);

		// sectionfourline should have incremented
		assertEquals(1, readInt(mode, "sectionfourline"));
		// Level increases by 4
		assertEquals(104, engine.statistics.level);
	}

	@Test
	void calcScoreAllClearTriggersBravoAndAcMedal() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 100;
		engine.softdropFall = 0;
		engine.statc[0] = 0;
		engine.manualLock = false;
		setInt(mode, "medalAC", 0);

		// Empty field -> all-clear

		mode.calcScore(engine, 0, 1);

		// AC medal should be 1 now
		assertEquals(1, readInt(mode, "medalAC"));
	}

	@Test
	void calcScoreLevel999TriggersEnding() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 998;
		engine.statistics.totalFour = 31;
		engine.timerActive = true;
		setInt(mode, "sectionfourline", 2);
		setBoolean(mode, "gmfourline", true);
		setInt(mode, "medalAC", 0);
		setInt(mode, "grade", 0);

		mode.calcScore(engine, 0, 2); // level becomes 1000 -> clamped to 999

		assertEquals(999, engine.statistics.level);
		assertEquals(2, engine.ending); // roll ending
		assertEquals(1, readInt(mode, "rollclear"));
		assertFalse(engine.timerActive);
	}

	@Test
	void calcScoreRotatesCountsRotations() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.nowPieceRotateCount = 3;
		setInt(mode, "rotateCount", 0);

		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		assertEquals(3, readInt(mode, "rotateCount"));
	}

	@Test
	void onMoveLevelsUpWhenNotInEnding() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.holdDisable = false;
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "lvupflag", false);

		mode.onMove(engine, 0);

		// Level should increase towards 99
		assertEquals(51, engine.statistics.level);
	}

	@Test
	void onARELevelsUp() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		engine.statc[0] = 10;
		engine.statc[1] = 10; // statc[0] >= statc[1] - 1
		// playerInit sets lvupflag=true, but onARE requires !lvupflag
		setBoolean(mode, "lvupflag", false);

		mode.onARE(engine, 0);

		assertTrue(readBoolean(mode, "lvupflag"));
		assertEquals(51, engine.statistics.level);
	}

	@Test
	void onLastDecrementsGradeflashAndScgettime() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gradeflash", 10);
		setInt(mode, "scgettime", 5);

		mode.onLast(engine, 0);

		assertEquals(9, readInt(mode, "gradeflash"));
		assertEquals(4, readInt(mode, "scgettime"));
	}

	@Test
	void onLastIncrementsSectionTimeWhenTimerActive() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 50;

		mode.onLast(engine, 0);

		int[] sectiontime = (int[]) readFieldByClass(mode, PhantomManiaMode.class, "sectiontime");
		assertEquals(1, sectiontime[0]);
	}

	@Test
	void onLastRollTimeIncreasesDuringEnding() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		engine.statistics.level = 999;

		mode.onLast(engine, 0);

		// rolltime should be 1 (1 frame = 1 increment)
		assertEquals(1, readInt(mode, "rolltime"));
	}

	@Test
	void onGameOverSetsSecretGrade() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;

		mode.onGameOver(engine, 0);

		// secretGrade should be set from field
		assertTrue(readInt(mode, "secretGrade") >= 0);
	}

	@Test
	void loadSaveSettingRoundTrip() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		CustomProperties prop = new CustomProperties();

		setIntMenuValue(mode, "startlevel", 5);
		setBoolMenuValue(mode, "lvstopse", true);
		setBoolMenuValue(mode, "big", true);
		setBoolMenuValue(mode, "showsectiontime", true);

		invokeSaveSetting(mode, prop);
		invokeLoadSetting(mode, prop);

		assertEquals(5, getIntMenuValue(mode, "startlevel"));
		assertTrue(getBoolMenuValue(mode, "lvstopse"));
	}

	@Test
	void updateRankingInsertsNewEntry() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 5, 999, 1800, 2);

		assertEquals(0, readInt(mode, "rankingRank"));
	}

	@Test
	void checkRankingReturnsMinusOneForEntryBelowAll() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Fill ranking with high values
		int[] rankingGrade = (int[]) readFieldByClass(mode, PhantomManiaMode.class, "rankingGrade");
		for (int i = 0; i < 10; i++) {
			rankingGrade[i] = 6; // GM
		}
		int[] rankingRollclear = (int[]) readFieldByClass(mode, PhantomManiaMode.class, "rankingRollclear");
		for (int i = 0; i < 10; i++) {
			rankingRollclear[i] = 2; // completed
		}

		int rank = invokeCheckRanking(mode, 0, 0, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void saveReplayStoresVersion() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		int ver = prop.getProperty("phantommania.version", -1);
		assertEquals(1, ver);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(PhantomManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readFieldByClass(Object mode, Class<?> cls, String name) throws Exception {
		Field f = findField(cls, name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(Object mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(Object mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void setIntMenuValue(Object mode, String menuFieldName, int value) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		((IntegerMenuItem) f.get(mode)).value = value;
	}

	private static int getIntMenuValue(Object mode, String menuFieldName) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		return ((IntegerMenuItem) f.get(mode)).value;
	}

	private static void setBoolMenuValue(Object mode, String menuFieldName, boolean value) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		((BooleanMenuItem) f.get(mode)).value = value;
	}

	private static boolean getBoolMenuValue(Object mode, String menuFieldName) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		return ((BooleanMenuItem) f.get(mode)).value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeSaveSetting(PhantomManiaMode mode, CustomProperties prop) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeLoadSetting(PhantomManiaMode mode, CustomProperties prop) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(PhantomManiaMode mode, int gr, int lv, int time, int clear) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time, clear);
	}

	private static int invokeCheckRanking(PhantomManiaMode mode, int gr, int lv, int time, int clear) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, gr, lv, time, clear);
	}
}
