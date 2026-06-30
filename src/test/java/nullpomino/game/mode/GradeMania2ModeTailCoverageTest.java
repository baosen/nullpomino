package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.OnOffMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Tail-coverage tests for {@link GradeMania2Mode}: targets onMove v1 RE-medal
 * recovery-set branch and ending-start mroll branch, levelUp v2 RE-medal
 * award branch, calcScore SK-medal big branch, CO-medal big/non-big higher
 * combo else-if branches, next-section BGM-change branch, and the
 * level==nextseclv-1 levelstop branch.
 */
class GradeMania2ModeTailCoverageTest {

	// --- 644, 645: onMove v1 RE medal sets recoveryFlag (blocks >= 150) ---
	@Test
	void onMoveV1SetsRecoveryFlag() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.statc[0] = 0;
		e.holdDisable = false;
		setBool(mode, "lvupflag", false);
		setInt(mode, "version", 1);
		e.timerActive = true;
		setInt(mode, "medalRE", 0);
		setBool(mode, "recoveryFlag", false);
		e.createFieldIfNeeded();
		fillBlocks(e, 16); // 16 rows * 10 = 160 blocks (>= 150)

		mode.onMove(e, 0);

		assertTrue(readBool(mode, "recoveryFlag"));
	}

	// --- 679-681: onMove ending start with mrollFlag uses lockflash hide ---
	@Test
	void onMoveEndingStartMroll() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 2;
		setBool(mode, "rollstarted", false);
		setBool(mode, "mrollFlag", true);
		e.ruleopt.lockflash = 2;
		e.createFieldIfNeeded();

		mode.onMove(e, 0);

		assertTrue(readBool(mode, "rollstarted"));
		assertEquals(e.ruleopt.lockflash, e.blockHidden);
	}

	// --- 747-749: levelUp v2 RE medal award (recoveryFlag true, blocks <= 70) ---
	@Test
	void levelUpV2AwardsREMedal() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "version", 2);
		e.timerActive = true;
		setInt(mode, "medalRE", 0);
		setBool(mode, "recoveryFlag", true);
		e.createFieldIfNeeded();
		fillBlocks(e, 5); // 50 blocks (<= 70)

		Method m = GradeMania2Mode.class.getDeclaredMethod("levelUp", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, e);

		assertEquals(1, readInt(mode, "medalRE"));
	}

	// --- 810-812: calcScore SK medal in big mode (totalFour boundary) ---
	@Test
	void calcScoreSKMedalBig() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		((OnOffMenuItem) readField(mode, "big")).value = true;
		e.statistics.level = 0;
		e.statistics.totalFour = 1; // {1,2,4} triggers SK medal in big mode

		mode.calcScore(e, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	// --- 837-839: calcScore CO medal big combo >= 3 (medalCO already 1) ---
	@Test
	void calcScoreCOMedalBigCombo3() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		((OnOffMenuItem) readField(mode, "big")).value = true;
		setInt(mode, "medalCO", 1);
		e.combo = 3;

		mode.calcScore(e, 0, 1);

		assertEquals(2, readInt(mode, "medalCO"));
	}

	// --- 840-842: calcScore CO medal big combo >= 4 (medalCO already 2) ---
	@Test
	void calcScoreCOMedalBigCombo4() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		((OnOffMenuItem) readField(mode, "big")).value = true;
		setInt(mode, "medalCO", 2);
		e.combo = 4;

		mode.calcScore(e, 0, 1);

		assertEquals(3, readInt(mode, "medalCO"));
	}

	// --- 849-850: calcScore CO medal non-big combo >= 5 (medalCO already 1) ---
	@Test
	void calcScoreCOMedalNonBigCombo5() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(mode, "medalCO", 1);
		e.combo = 5;

		mode.calcScore(e, 0, 1);

		assertEquals(2, readInt(mode, "medalCO"));
	}

	// --- 852-853: calcScore CO medal non-big combo >= 7 (medalCO already 2) ---
	@Test
	void calcScoreCOMedalNonBigCombo7() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(mode, "medalCO", 2);
		e.combo = 7;

		mode.calcScore(e, 0, 1);

		assertEquals(3, readInt(mode, "medalCO"));
	}

	// --- 899-901: calcScore next-section BGM change ---
	@Test
	void calcScoreNextSectionBgmChange() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(mode, "bgmlv", 0); // tableBGMChange[0] = 500
		setInt(mode, "nextseclv", 500);
		e.statistics.level = 499; // +1 line -> 500 >= nextseclv and >= 500

		mode.calcScore(e, 0, 1);

		assertEquals(1, readInt(mode, "bgmlv"));
	}

	// --- 922: calcScore level == nextseclv-1 with levelstop SE ---
	@Test
	void calcScoreLevelStopSE() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		((OnOffMenuItem) readField(mode, "lvstopse")).value = true;
		setInt(mode, "nextseclv", 2);
		e.statistics.level = 0; // +1 line -> level 1 == nextseclv-1

		mode.calcScore(e, 0, 1);

		assertEquals(1, e.statistics.level);
	}

	// --- helpers ---

	private static void fillBlocks(GameEngine e, int rows) {
		int w = e.field.getWidth();
		int h = e.field.getHeight();
		for (int y = h - 1; y > h - 1 - rows && y >= 0; y--) {
			for (int x = 0; x < w; x++) {
				e.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_RED));
			}
		}
	}

	private static GameEngine freshEngine(GradeMania2Mode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		return m.engine[0];
	}

	private static int readInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static boolean readBool(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getBoolean(o);
	}

	private static Object readField(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.get(o);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	private static void setBool(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setBoolean(o, v);
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException ex) {}
		throw new NoSuchFieldException(n);
	}
}
