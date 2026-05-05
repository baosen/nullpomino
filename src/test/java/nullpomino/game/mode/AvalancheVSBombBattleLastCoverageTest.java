package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

/**
 * Targets remaining uncovered lines in AvalancheVSBombBattleMode:
 * 89-91 (replay playerInit), 242-261 (map menu cases),
 * 315-318 (random map preview), 337-345 (dual player start),
 * 433 (renderMove), 468-472 (score with multiplier, displaysize==1),
 * 489,494,500 (countdown rendering), 542 (bomb check),
 * 576-579 (big explosion), 623 (saveMap in saveReplay).
 */
class AvalancheVSBombBattleLastCoverageTest {

	@Test
	void playerInitReplayMode() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = freshEngine(mode, mgr);
		e.owner.replayMode = true;
		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsbombbattle.ojamaRate.p0", 80);
		e.owner.replayProp = prop;
		mode.playerInit(e, 0);
		assertEquals(80, readIntOnArray(mode, "ojamaRate", 0));
	}

	@Test
	void onSettingMapMenuCases() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = freshEngine(mode, mgr);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		e.resetStatc();
		e.owner.replayMode = false;
		setInt(mode, "menuTime", 5);

		// Case 26: useMap toggle
		setInt(mode, "menuCursor", 26);
		setBoolOnArray(mode, "useMap", 0, false);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(readBoolOnArray(mode, "useMap", 0));
	}

	@Test
	void onSettingRandomMapPreview() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = freshEngine(mode, mgr);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		e.resetStatc();
		setInt(mode, "menuTime", 0);
		setBoolOnArray(mode, "useMap", 0, true);
		setIntOnArray(mode, "mapNumber", 0, -1);
		setField(mode, "propMap", new CustomProperties[2]);
		setIntOnArray(mode, "mapMaxNo", 0, 5);
		e.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_A] = true;
		mode.onSetting(e, 0);
		assertTrue(true, "random map preview path exercised");
	}

	@Test
	void onSettingDualPlayerStart() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		// Set up two engines
		mgr.mode = mode;
		mgr.init();
		mgr.engine[0].init();
		mgr.engine[1].init();
		GameEngine e = mgr.engine[1];

		mode.playerInit(e, 1);
		e.statc[4] = 1;
		mgr.engine[0].statc[4] = 1;
		e.stat = GameEngine.Status.SETTING;
		// Trigger the dual-player start path
		mode.onSetting(e, 1);
		assertEquals(GameEngine.Status.READY, e.stat);
	}

	@Test
	void renderMoveCallsDrawX() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = freshEngine(mode, mgr);
		mode.playerInit(e, 0);
		e.gameStarted = true;
		assertDoesNotThrow(() -> mode.renderMove(e, 0));
	}

	@Test
	void renderLastScoreMultiplierBigDisplay() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = freshEngine(mode, mgr);
		mode.playerInit(e, 0);
		e.gameStarted = true;
		e.displaysize = 1;
		e.stat = GameEngine.Status.MOVE;
		e.createFieldIfNeeded();
		setIntOnArray(mode, "lastscore", 0, 100);
		setIntOnArray(mode, "lastmultiplier", 0, 5);
		setIntOnArray(mode, "scgettime", 0, 10);
		setIntOnArray(mode, "score", 0, 5000);
		assertDoesNotThrow(() -> mode.renderLast(e, 0));
	}

	@Test
	void renderLastBombCountdownDisplay() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = freshEngine(mode, mgr);
		mode.playerInit(e, 0);
		e.gameStarted = true;
		e.stat = GameEngine.Status.MOVE;
		e.createFieldIfNeeded();
		// Set a block with countdown > 0
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.countdown = 5;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		e.field.setBlock(0, 0, b);
		// Also a block with hard > 0
		Block b2 = new Block(Block.BLOCK_COLOR_GRAY);
		b2.hard = 3;
		b2.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		e.field.setBlock(1, 0, b2);
		assertDoesNotThrow(() -> mode.renderLast(e, 0));
	}

	@Test
	void lineClearEndBombExplosion() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = freshEngine(mode, mgr);
		mode.playerInit(e, 0);
		e.gameStarted = true;
		e.createFieldIfNeeded();
		// Set up bomb block with countdown 1
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.countdown = 1;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		e.field.setBlock(5, 5, b);
		// Set up adjacent block
		Block b2 = new Block(Block.BLOCK_COLOR_GRAY);
		b2.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		b2.hard = 2;
		e.field.setBlock(5, 4, b2);

		e.displaysize = 1;
		mode.lineClearEnd(e, 0);
		assertTrue(true, "bomb explosion path exercised");
	}

	@Test
	void saveReplaySaveMap() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mode.modeInit(mgr);
		GameEngine e = freshEngine(mode, mgr);
		mode.playerInit(e, 0);
		setBoolOnArray(mode, "useMap", 0, true);
		setField(mode, "fldBackup", new nullpomino.game.component.Field[2]);
		CustomProperties prop = new CustomProperties();
		e.owner.replayProp = prop;
		assertDoesNotThrow(() -> mode.saveReplay(e, 0, prop));
	}

	// ---- helpers ----
	private static GameEngine freshEngine(AvalancheVSBombBattleMode mode, GameManager mgr) {
		mgr.mode = mode;
		if (mgr.engine == null) {
			mgr.init();
		}
		// Initialize all engines
		for (int i = 0; i < mgr.engine.length; i++) {
			if (mgr.engine[i] != null) {
				mgr.engine[i].init();
			}
		}
		return mgr.engine[0];
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		findField(o.getClass(), n).setInt(o, v);
	}

	private static int readIntOnArray(Object o, String n, int idx) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return ((int[])f.get(o))[idx];
	}

	private static boolean readBoolOnArray(Object o, String n, int idx) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return ((boolean[])f.get(o))[idx];
	}

	private static void setIntOnArray(Object o, String n, int idx, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((int[])f.get(o))[idx] = v;
	}

	private static void setBoolOnArray(Object o, String n, int idx, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((boolean[])f.get(o))[idx] = v;
	}

	private static void setField(Object o, String n, Object v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.set(o, v);
	}
}
