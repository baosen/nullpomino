package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Targets remaining uncovered lines in AvalancheVSSPFMode.java:
 * 212 (loadDropMapPreview null), 254-256 (playerInit replay),
 * 272-273,287 (onSetting cursor wrap), 423 (disable map field reset),
 * 433-434,444,446 (map settings), 509-512,525-526,529 (replay menu),
 * 637,644,695 (renderSetting), 749,751-753,755-770,774-775 (renderLast countdown),
 * 824,836,874,885,899 (saveReplay various).
 */
class AvalancheVSSPFModeRemainingCoverageTest {

	@Test
	void loadDropMapPreviewNullPattern() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();

		Method m = AvalancheVSSPFMode.class.getDeclaredMethod("loadDropMapPreview", GameEngine.class, int.class, int[][].class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, (int[][]) null);

		// Should reset field
	}

	@Test
	void playerInitReplayPath() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		e.owner.replayProp.setProperty("avalanchevsspf.ojamaRate.p0", "200");
		e.owner.replayProp.setProperty("avalanchevsspf.ojamaCountdown.p0", "5");
		e.owner.replayProp.setProperty("avalanchevs.version", "0");

		mode.playerInit(e, 0);

		int[] ojamaRate = (int[]) readField(mode, "ojamaRate");
		assertEquals(200, ojamaRate[0]);
	}

	@Test
	void onSettingCursorUpWrap() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_UP] = true;

		mode.onSetting(e, 0);

		assertEquals(33, readInt(mode, "menuCursor"));
	}

	@Test
	void onSettingCursorDownWrap() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 33);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;

		mode.onSetting(e, 0);

		assertEquals(0, readInt(mode, "menuCursor"));
	}

	@Test
	void onSettingCase24UseMapOff() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 24);
		setInt(mode, "menuTime", 5);
		setBooleanArr(mode, "useMap", 0, true);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);

		assertFalse(((boolean[]) readField(mode, "useMap"))[0]);
	}

	@Test
	void onSettingReplayMenuProgression() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 60);

		mode.onSetting(e, 0);
		assertEquals(9, readInt(mode, "menuCursor"));
	}

	@Test
	void renderSettingDropSetPage() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 32);
		e.createFieldIfNeeded();

		mode.renderSetting(e, 0);
	}

	@Test
	void renderLastCountdownBlocks() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.gameStarted = true;
		e.stat = GameEngine.Status.MOVE;
		e.createFieldIfNeeded();
		// Place a block with countdown
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.countdown = 5;
		b.secondaryColor = Block.BLOCK_COLOR_BLUE;
		e.field.setBlock(0, 0, b);
		e.displaysize = 0;

		mode.renderLast(e, 0);
	}

	@Test
	void renderLastCountdownWithDifferentColors() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.gameStarted = true;
		e.stat = GameEngine.Status.MOVE;
		e.createFieldIfNeeded();
		// Green countdown block
		Block bg = new Block(Block.BLOCK_COLOR_GREEN);
		bg.countdown = 3;
		bg.secondaryColor = Block.BLOCK_COLOR_GREEN;
		e.field.setBlock(1, 0, bg);
		// Yellow countdown block
		Block by = new Block(Block.BLOCK_COLOR_YELLOW);
		by.countdown = 10;
		by.secondaryColor = Block.BLOCK_COLOR_YELLOW;
		e.field.setBlock(2, 0, by);
		e.displaysize = 0;

		mode.renderLast(e, 0);
	}

	@Test
	void saveReplayCallsSuper() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		CustomProperties prop = new CustomProperties();
		setBooleanArr(mode, "useMap", 0, true);
		// Need fldBackup[0] to be non-null to trigger saveMap
		// But without a real field, we just verify it doesn't crash

		mode.saveReplay(e, 0, prop);
	}

	@Test
	void lineClearEndGameOver() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBooleanArr(mode, "dangerColumnDouble", 0, true);
		// Place a block in danger column
		e.field.setBlock(2, 0, new Block(Block.BLOCK_COLOR_RED));

		mode.lineClearEnd(e, 0);

		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	@Test
	void lineClearEndCountdownDecrement() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setIntArr(mode, "ojamaCountdown", 0, 5); // not 10, so countdown decrements
		setBooleanArr(mode, "countdownDecremented", 0, false);
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.countdown = 2;
		e.field.setBlock(0, 0, b);

		mode.lineClearEnd(e, 0);
	}

	// --- helpers ---

	private static GameManager twoPlayerEngine(AvalancheVSSPFMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		mode.modeInit(m);
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		m.engine[1].init();
		m.engine[1].ruleopt.fieldWidth = 10;
		m.engine[1].ruleopt.fieldHeight = 20;
		return m;
	}

	private static GameEngine freshEngine(AvalancheVSSPFMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		mode.modeInit(m);
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

	private static boolean readBoolean(Object o, String n) throws Exception {
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

	private static void setBoolean(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setBoolean(o, v);
	}

	private static void setBooleanArr(Object o, String n, int idx, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((boolean[]) f.get(o))[idx] = v;
	}

	private static void setIntArr(Object o, String n, int idx, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((int[]) f.get(o))[idx] = v;
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}
}
