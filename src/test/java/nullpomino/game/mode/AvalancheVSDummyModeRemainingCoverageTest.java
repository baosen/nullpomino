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
 * Targets remaining uncovered lines in AvalancheVSDummyMode.java.
 * Uses a concrete subclass to test the abstract class.
 */
class AvalancheVSDummyModeRemainingCoverageTest {

	// Concrete subclass to instantiate abstract AvalancheVSDummyMode
	static class ConcreteAvalancheVSDummyMode extends AvalancheVSDummyMode {
		@Override public String getName() { return "TestDummy"; }
		@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
	}

	@Test
	void loadMapPreviewWithPropMapNullResetsField() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		// forceReload with null propMap - should reset field
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("loadMapPreview",
				GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, true);
		// No assertion - just verify no exception
	}

	@Test
	void readyInitBigMode() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBooleanArr(mode, "big", 0, true);

		mode.readyInit(e, 0);

		assertEquals(3, e.colorClearSize);
	}

	@Test
	void readyInitReplayMapLoad() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		mode.playerInit(e, 0);
		setBooleanArr(mode, "useMap", 0, true);
		e.createFieldIfNeeded();
		CustomProperties replayProp = new CustomProperties();
		e.owner.replayProp = replayProp;
		replayProp.setProperty("map.0", "r,g,b,y");

		mode.readyInit(e, 0);
	}

	@Test
	void readyInitMapLoad() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBooleanArr(mode, "useMap", 0, true);
		setIntArr(mode, "mapNumber", 0, 0);
		setIntArr(mode, "mapSet", 0, 0);
		e.createFieldIfNeeded();

		mode.readyInit(e, 0);
	}

	@Test
	void calcScoreBigModeMultiplierShift() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBooleanArr(mode, "big", 0, true);
		// Put a block and trigger a cascade situation
		e.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED));
		e.field.setBlock(1, 0, new Block(Block.BLOCK_COLOR_RED));

		mode.calcScore(e, 0, 0);
	}

	@Test
	void addOjamaWithCounter() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		setIntArr(mode, "ojamaCounterMode", 0, AvalancheVSDummyMode.OJAMA_COUNTER_ON);
		setIntArr(mode, "ojama", 0, 5);
		setIntArr(mode, "ojamaRate", 0, 70);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("addOjama", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 500);
	}

	@Test
	void onLastBothLoseDraw() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameManager mgr = twoPlayerEngine(mode);
		mgr.engine[0].gameActive = true;
		mgr.engine[1].gameActive = true;
		mgr.engine[0].stat = GameEngine.Status.GAMEOVER;
		mgr.engine[1].stat = GameEngine.Status.GAMEOVER;
		mgr.engine[0].timerActive = false;
		mgr.engine[1].timerActive = false;

		mode.onLast(mgr.engine[0], 1);
	}

	@Test
	void onLastP1Win() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameManager mgr = twoPlayerEngine(mode);
		mgr.engine[0].gameActive = true;
		mgr.engine[1].gameActive = true;
		mgr.engine[0].stat = GameEngine.Status.EXCELLENT;
		mgr.engine[1].stat = GameEngine.Status.GAMEOVER;

		mode.onLast(mgr.engine[0], 1);
	}

	@Test
	void onLastP2Win() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameManager mgr = twoPlayerEngine(mode);
		mgr.engine[0].gameActive = true;
		mgr.engine[1].gameActive = true;
		mgr.engine[0].stat = GameEngine.Status.GAMEOVER;
		mgr.engine[1].stat = GameEngine.Status.EXCELLENT;

		mode.onLast(mgr.engine[0], 1);
	}

	@Test
	void gameOverCheckBigColumn() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBooleanArr(mode, "big", 0, true);
		e.field.setBlock(1, 0, new Block(Block.BLOCK_COLOR_RED));

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("gameOverCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);

		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	@Test
	void gameOverCheckDangerColumnDouble() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBooleanArr(mode, "dangerColumnDouble", 0, true);
		e.field.setBlock(3, 0, new Block(Block.BLOCK_COLOR_RED));

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("gameOverCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);

		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	@Test
	void renderLastChainZenkeshiDisplay() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.owner.engine[0].gameActive = true;
		e.chain = 5;
		setIntArr(mode, "chainDisplay", 0, 30);
		setIntArr(mode, "chainDisplayType", 0, 1);
		setBooleanArr(mode, "zenKeshi", 0, true);

		mode.renderLast(e, 0);
	}

	@Test
	void renderLastChainDisplayNone() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.owner.engine[0].gameActive = true;
		e.chain = 3;
		setIntArr(mode, "chainDisplay", 0, 30);
		setIntArr(mode, "chainDisplayType", 0, AvalancheVSDummyMode.CHAIN_DISPLAY_NONE);

		mode.renderLast(e, 0);
	}

	@Test
	void drawXSmallBigDisplay() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		e.displaysize = 1;
		setBooleanArr(mode, "dangerColumnShowX", 0, true);
		setBooleanArr(mode, "dangerColumnDouble", 0, true);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawX", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);
	}

	@Test
	void drawXNormalDisplay() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		e.displaysize = 0;
		setBooleanArr(mode, "dangerColumnShowX", 0, true);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawX", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);
	}

	@Test
	void drawHardOjamaSmallDisplay() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		e.displaysize = 1;
		Block b = new Block(Block.BLOCK_COLOR_GRAY);
		b.hard = 3;
		e.field.setBlock(0, 0, b);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawHardOjama", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);
	}

	@Test
	void drawScoresWithLastscore() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setIntArr(mode, "scgettime", 0, 10);
		setIntArr(mode, "scgettime", 1, 10);
		setIntArr(mode, "lastscore", 0, 100);
		setIntArr(mode, "lastmultiplier", 0, 5);
		setIntArr(mode, "lastscore", 1, 200);
		setIntArr(mode, "lastmultiplier", 1, 3);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawScores",
				GameEngine.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, 0, EventReceiver.COLOR_WHITE);
	}

	@Test
	void drawOjamaWithAdd() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setIntArr(mode, "ojama", 0, 5);
		setIntArr(mode, "ojamaAdd", 0, 3);
		setIntArr(mode, "ojama", 1, 2);
		setIntArr(mode, "ojamaAdd", 1, 1);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawOjama",
				GameEngine.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, 0, EventReceiver.COLOR_WHITE);
	}

	@Test
	void drawAttack() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setIntArr(mode, "ojamaSent", 0, 50);
		setIntArr(mode, "ojamaSent", 1, 30);

		Method m = AvalancheVSDummyMode.class.getDeclaredMethod("drawAttack",
				GameEngine.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0, 0, 0, EventReceiver.COLOR_WHITE);
	}

	@Test
	void renderResultDraw() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		setInt(mode, "winnerID", -1);
		mgr.engine[0].statistics.time = 3600;

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultWin() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		setInt(mode, "winnerID", 0);
		mgr.engine[0].statistics.time = 3600;

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultLose() throws Exception {
		ConcreteAvalancheVSDummyMode mode = new ConcreteAvalancheVSDummyMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		setInt(mode, "winnerID", 1);
		mgr.engine[0].statistics.time = 3600;

		mode.renderResult(e, 0);
	}

	// --- helpers ---

	private static GameManager twoPlayerEngine(ConcreteAvalancheVSDummyMode mode) {
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

	private static GameEngine freshEngine(ConcreteAvalancheVSDummyMode mode) {
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

	private static Object readField(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.get(o);
	}

	static void setInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	static void setIntArr(Object o, String n, int idx, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((int[]) f.get(o))[idx] = v;
	}

	static void setBoolean(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setBoolean(o, v);
	}

	static void setBooleanArr(Object o, String n, int idx, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((boolean[]) f.get(o))[idx] = v;
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}
}
