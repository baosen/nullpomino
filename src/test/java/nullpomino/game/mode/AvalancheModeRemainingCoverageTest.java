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
 * Targets remaining uncovered lines in AvalancheMode.java:
 * 165-167 (onSetting sprintTarget), 170-172 (scoreType), 175-177 (numColors),
 * 180-181,183-184,186-188,191-192,194-195,197-199,202 (onSetting other cases),
 * 217,228 (replay menuTime paths), 276,287 (renderMove),
 * 293-294,296-301,304-312 (renderLast ranking display),
 * 315,317,319-320,322-323,325,327-328,330-333,335,337-338,340-341,
 * 343-344,346-347,349-350,353-357,359-363 (renderLast score display),
 * 373-376,378,382 (drawXorTimer),
 * 428,430 (addBonus), 433-447 (calcChainMultiplier).
 */
class AvalancheModeRemainingCoverageTest {

	@Test
	void onSettingCase1SprintTarget() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 1);
		setInt(mode, "menuTime", 5);
		setInt(mode, "gametype", 2); // sprint
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCase2ScoreType() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCase3NumColors() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCase4DangerColumnDouble() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 4);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
		assertTrue(readBoolean(mode, "dangerColumnDouble"));
	}

	@Test
	void onSettingCase5DangerColumnShowX() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 5);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCase6ColorClearSize() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 6);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCase7CascadeSlow() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 7);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCase9OutlineType() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 9);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCase10ShowChains() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 10);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingReplayMenuTime60() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 60);

		// menuCursor should be set to 9 when menuTime >= 60 in replay mode
		assertTrue(mode.onSetting(e, 0));
		assertEquals(9, readInt(mode, "menuCursor"));
	}

	@Test
	void onSettingReplayMenuTime120() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 120);

		// menuTime >= 60 sets menuCursor = 9 and returns true (the else-if for >= 120 is unreachable)
		assertTrue(mode.onSetting(e, 0));
	}

	@Test
	void renderMoveWithDangerColumnShowX() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.gameStarted = true;
		setBoolean(mode, "dangerColumnShowX", true);

		mode.renderMove(e, 0);
	}

	@Test
	void renderLastRankingWithAllGameTypes() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		e.owner.replayMode = false;

		// Test Marathon
		setInt(mode, "gametype", 0);
		mode.renderLast(e, 0);

		// Test Ultra
		setInt(mode, "gametype", 1);
		mode.renderLast(e, 0);

		// Test Sprint
		setInt(mode, "gametype", 2);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastScoreDisplay() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.statistics.score = 5000;
		setInt(mode, "lastscore", 100);
		setInt(mode, "lastmultiplier", 5);
		setInt(mode, "scgettime", 10);
		setInt(mode, "level", 10);
		setInt(mode, "garbageSent", 100);
		setInt(mode, "garbageAdd", 20);
		setInt(mode, "blocksCleared", 200);
		setInt(mode, "zenKeshiCount", 3);
		e.statistics.maxChain = 5;
		e.createFieldIfNeeded();

		mode.renderLast(e, 0);
	}

	@Test
	void drawXorTimerSmallDisplay() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		e.displaysize = 1;
		setBoolean(mode, "dangerColumnDouble", true);

		Method m = AvalancheMode.class.getDeclaredMethod("drawXorTimer", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);
	}

	@Test
	void drawXorTimerNormalDisplay() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		e.displaysize = 0;

		Method m = AvalancheMode.class.getDeclaredMethod("drawXorTimer", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);
	}

	@Test
	void addBonusNonSprint() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "gametype", 0); // marathon
		e.statistics.score = 1000;
		setInt(mode, "zenKeshiCount", 2);
		e.statistics.maxChain = 3;

		mode.addBonus(e, 0);

		assertTrue(e.statistics.score > 1000);
	}

	@Test
	void calcChainMultiplierClassic() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 0);

		// Test via direct calculation
		Method m = AvalancheMode.class.getDeclaredMethod("calcChainMultiplier", int.class);
		m.setAccessible(true);

		assertEquals(8, (int) m.invoke(mode, 2));
		assertEquals(16, (int) m.invoke(mode, 3));
		assertEquals(32, (int) m.invoke(mode, 4));
		assertEquals(64, (int) m.invoke(mode, 5));
		assertEquals(0, (int) m.invoke(mode, 1));
	}

	@Test
	void calcChainMultiplierFever() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 1);

		Method m = AvalancheMode.class.getDeclaredMethod("calcChainMultiplier", int.class);
		m.setAccessible(true);

		int result = (int) m.invoke(mode, 2);
		assertTrue(result > 0);
	}

	@Test
	void lineClearEndGameOverWithDangerDouble() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBoolean(mode, "dangerColumnDouble", true);
		// Put block at (3,0) - the second danger column
		e.field.setBlock(3, 0, new Block(Block.BLOCK_COLOR_RED));

		mode.lineClearEnd(e, 0);

		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	@Test
	void renderLastWithChainAndZenkeshi() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.chain = 7;
		setInt(mode, "chainDisplay", 30);
		setBoolean(mode, "zenKeshi", true);
		e.createFieldIfNeeded();
		// showChains is a boolean field
		setBoolean(mode, "showChains", true);

		mode.renderLast(e, 0);
	}

	// --- helpers ---

	private static GameEngine freshEngine(AvalancheMode mode) {
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

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}
}
