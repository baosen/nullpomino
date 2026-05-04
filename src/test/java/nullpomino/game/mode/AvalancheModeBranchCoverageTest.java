package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;

class AvalancheModeBranchCoverageTest {

	@Test void calcChainMultiplierClassicChain1Returns0() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 0);
		assertEquals(0, mode.calcChainMultiplier(1));
	}
	@Test void calcChainMultiplierFeverChain2() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 1);
		assertEquals(12, mode.calcChainMultiplier(2));
	}
	@Test void addBonusGametypeNonSprintCallsSuper() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); setInt(mode, "gametype", 0);
		Method m = AvalancheMode.class.getDeclaredMethod("addBonus", GameEngine.class, int.class);
		m.setAccessible(true); m.invoke(mode, e, 0);
	}
	@Test void addBonusGametypeSprintNoOp() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); setInt(mode, "gametype", 2);
		Method m = AvalancheMode.class.getDeclaredMethod("addBonus", GameEngine.class, int.class);
		m.setAccessible(true); m.invoke(mode, e, 0);
	}
	@Test void renderSettingPage2() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		setInt(mode, "menuCursor", 9); mode.renderSetting(e, 0);
	}
	@Test void renderSettingSprintTarget() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		setInt(mode, "menuCursor", 0); setInt(mode, "gametype", 2); mode.renderSetting(e, 0);
	}
	@Test void renderLastWithMultiplier() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); setInt(mode, "gametype", 0);
		setInt(mode, "lastscore", 500); setInt(mode, "lastmultiplier", 3); setInt(mode, "scgettime", 10);
		mode.renderLast(e, 0);
	}
	@Test void renderLastGarbageAddDisplay() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); setInt(mode, "garbageAdd", 5); mode.renderLast(e, 0);
	}
	@Test void onLastUltraMeterRed() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); setInt(mode, "gametype", 1);
		e.statistics.time = 10201; mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
	}
	@Test void onLastSprintMeterRed() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); setInt(mode, "gametype", 2);
		e.statistics.score = 14991; mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
	}
	@Test void renderResultSprint() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); setInt(mode, "gametype", 2); setInt(mode, "rankingRank", -1);
		mode.renderResult(e, 0);
	}
	@Test void renderResultNonSprintWithRank() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); setInt(mode, "gametype", 0); setInt(mode, "rankingRank", 5);
		mode.renderResult(e, 0);
	}
	@Test void onSettingReplayMode() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		e.owner.replayMode = true; assertTrue(mode.onSetting(e, 0));
		assertEquals(-1, readInt(mode, "menuCursor"));
	}
	@Test void saveReplayUpdatesRanking() throws Exception {
		AvalancheMode mode = new AvalancheMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.ai = null; e.colorClearSize = 4;
		setInt(mode, "gametype", 0); e.statistics.score = 50000; e.statistics.time = 3600;
		mode.saveReplay(e, 0, new CustomProperties());
		assertEquals(0, readInt(mode, "rankingRank"));
	}

	private static GameEngine freshEngine(AvalancheMode mode) {
		GameManager m = new GameManager(new EventReceiver()); m.mode = mode; m.init(); m.engine[0].init(); return m.engine[0];
	}
	private static int readInt(Object o, String n) throws Exception {
		java.lang.reflect.Field f = findField(o.getClass(), n); f.setAccessible(true); return f.getInt(o);
	}
	private static void setInt(Object o, String n, int v) throws Exception {
		java.lang.reflect.Field f = findField(o.getClass(), n); f.setAccessible(true); f.setInt(o, v);
	}
	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try { return c.getDeclaredField(name); } catch (NoSuchFieldException e) { /* continue */ }
		}
		throw new NoSuchFieldException(name);
	}
}
