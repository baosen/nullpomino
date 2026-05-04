package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;

class AbstractModeEdgeTest {
	private static class VSModeStub extends AbstractMode {
		private final boolean vs, net;
		VSModeStub(boolean vs, boolean net) { this.vs = vs; this.net = net; }
		@Override public boolean isVSMode() { return vs; }
		@Override public boolean isNetplayMode() { return net; }
	}
	@Test void renderInputVSModePlayer0() throws Exception {
		VSModeStub m = new VSModeStub(true, false);
		GameEngine e = fresh(); m.playerInit(e, 0); e.ctrl = new Controller(); m.renderInput(e, 0);
	}
	@Test void renderInputVSModePlayer1() throws Exception {
		VSModeStub m = new VSModeStub(true, false);
		GameManager gm = new GameManager(new EventReceiver()); gm.init(); gm.engine[0].init();
		GameEngine e = gm.engine[0]; m.playerInit(e, 0); e.ctrl = new Controller(); m.renderInput(e, 0);
	}
	@Test void renderInputNonVSMode() throws Exception {
		VSModeStub m = new VSModeStub(false, false);
		GameEngine e = fresh(); m.playerInit(e, 0); e.ctrl = new Controller(); m.renderInput(e, 0);
	}
	@Test void renderInputNetplayMode() throws Exception {
		VSModeStub m = new VSModeStub(true, true);
		GameEngine e = fresh(); m.playerInit(e, 0); e.ctrl = new Controller(); m.renderInput(e, 0);
	}
	@Test void renderSettingReplay() throws Exception {
		VSModeStub m = new VSModeStub(false, false);
		GameEngine e = fresh(); m.playerInit(e, 0); e.owner.replayMode = true; m.renderSetting(e, 0);
	}
	@Test void drawResultRankScale() throws Exception {
		VSModeStub m = new VSModeStub(false, false);
		GameEngine e = fresh(); EventReceiver r = e.owner.receiver;
		Method me = AbstractMode.class.getDeclaredMethod("drawResultRankScale", GameEngine.class, int.class, EventReceiver.class, int.class, int.class, float.class, int.class);
		me.setAccessible(true); me.invoke(m, e, 0, r, 0, EventReceiver.COLOR_WHITE, 1.0f, 3); me.invoke(m, e, 0, r, 0, EventReceiver.COLOR_WHITE, 1.0f, -1);
	}
	@Test void drawResultNetRankScale() throws Exception {
		VSModeStub m = new VSModeStub(false, false);
		GameEngine e = fresh(); EventReceiver r = e.owner.receiver;
		Method me = AbstractMode.class.getDeclaredMethod("drawResultNetRankScale", GameEngine.class, int.class, EventReceiver.class, int.class, int.class, float.class, int.class);
		me.setAccessible(true); me.invoke(m, e, 0, r, 0, EventReceiver.COLOR_WHITE, 1.0f, 2); me.invoke(m, e, 0, r, 0, EventReceiver.COLOR_WHITE, 1.0f, -1);
	}
	@Test void drawResultNetRankDailyScale() throws Exception {
		VSModeStub m = new VSModeStub(false, false);
		GameEngine e = fresh(); EventReceiver r = e.owner.receiver;
		Method me = AbstractMode.class.getDeclaredMethod("drawResultNetRankDailyScale", GameEngine.class, int.class, EventReceiver.class, int.class, int.class, float.class, int.class);
		me.setAccessible(true); me.invoke(m, e, 0, r, 0, EventReceiver.COLOR_WHITE, 1.0f, 5); me.invoke(m, e, 0, r, 0, EventReceiver.COLOR_WHITE, 1.0f, -1);
	}
	@Test void drawMenuCompact() throws Exception {
		VSModeStub m = new VSModeStub(false, false);
		GameEngine e = fresh(); EventReceiver r = e.owner.receiver; m.playerInit(e, 0);
		Method me = AbstractMode.class.getDeclaredMethod("drawMenuCompact", GameEngine.class, int.class, EventReceiver.class, String[].class);
		me.setAccessible(true); me.invoke(m, e, 0, r, new String[]{"A","1","B","2"});
	}
	@Test void drawResultStatsScale() throws Exception {
		VSModeStub m = new VSModeStub(false, false);
		GameEngine e = fresh(); EventReceiver r = e.owner.receiver; m.playerInit(e, 0);
		e.statistics.score=100; e.statistics.lines=5; e.statistics.time=360; e.statistics.level=2;
		e.statistics.totalPieceLocked=25; e.statistics.maxCombo=3; e.statistics.maxChain=4;
		e.statistics.levelDispAdd=1; e.statistics.spl=20f; e.statistics.spm=1200f;
		e.statistics.sps=2.5f; e.statistics.lpm=30f; e.statistics.lps=0.5f;
		e.statistics.ppm=60f; e.statistics.pps=1f;
		Class<?> sc = Class.forName("nullpomino.game.mode.AbstractMode$Statistic");
		Object[] all = (Object[])sc.getMethod("values").invoke(null);
		Method me = AbstractMode.class.getDeclaredMethod("drawResultStatsScale", GameEngine.class, int.class, EventReceiver.class, int.class, int.class, float.class, all.getClass());
		me.setAccessible(true); me.invoke(m, e, 0, r, 0, EventReceiver.COLOR_WHITE, 1.0f, all);
	}
	/**
	 * Covers the overloaded {@code drawMenuCompact(GameEngine, int, EventReceiver,
	 * int, int, int, String...)} at lines 229-235.  Verifies that the method sets
	 * {@code menuY}, {@code menuColor}, and {@code statcMenu} before delegating
	 * to the varargs overload.
	 */
	@Test void drawMenuCompactWithPositionArgs() throws Exception {
		VSModeStub m = new VSModeStub(false, false);
		GameEngine e = fresh(); EventReceiver r = e.owner.receiver; m.playerInit(e, 0);
		// Use reflection to find the 6-param + varargs overload
		Method me = AbstractMode.class.getDeclaredMethod("drawMenuCompact",
				GameEngine.class, int.class, EventReceiver.class,
				int.class, int.class, int.class, String[].class);
		me.setAccessible(true);
		// Invoke with known position args
		me.invoke(m, e, 0, r, 42, EventReceiver.COLOR_RED, 7, new String[]{"K","1","L","2"});
		// Verify the delegation set the state fields
		java.lang.reflect.Field fMenuY = AbstractMode.class.getDeclaredField("menuY");
		java.lang.reflect.Field fMenuColor = AbstractMode.class.getDeclaredField("menuColor");
		java.lang.reflect.Field fStatcMenu = AbstractMode.class.getDeclaredField("statcMenu");
		fMenuY.setAccessible(true);
		fMenuColor.setAccessible(true);
		fStatcMenu.setAccessible(true);
		// The 4-arg delegate increments menuY/statcMenu once per string pair
		// (we passed 2 pairs: "K","1" and "L","2"), so:
		//   menuY    = 42 + 2 = 44
		//   statcMenu = 7 + 2 = 9
		assertEquals(44, fMenuY.getInt(m));
		assertEquals(EventReceiver.COLOR_RED, fMenuColor.getInt(m));
		assertEquals(9, fStatcMenu.getInt(m));
	}
	private static GameEngine fresh() { GameManager gm = new GameManager(new EventReceiver()); gm.init(); gm.engine[0].init(); return gm.engine[0]; }
}
