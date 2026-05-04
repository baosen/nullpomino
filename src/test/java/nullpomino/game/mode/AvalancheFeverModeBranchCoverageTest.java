package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Method;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;

class AvalancheFeverModeBranchCoverageTest {

	@Test void calcChainMultiplierOverflowGivesLastPower() throws Exception {
		assertEquals(800, new AvalancheFeverMode().calcChainMultiplier(25));
	}
	@Test void onClearChainNotOneDoesNotResetLevelMultiplier() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		e.chain = 3; setInt(mode, "chainLevelMultiplier", 10);
		Method m = AvalancheFeverMode.class.getDeclaredMethod("onClear", GameEngine.class, int.class);
		m.setAccessible(true); m.invoke(mode, e, 0);
		assertEquals(10, readInt(mode, "chainLevelMultiplier"));
	}
	@Test void renderSettingPage2() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		setInt(mode, "menuCursor", 6); mode.renderSetting(e, 0);
	}
	@Test void renderLastWithMultiplier() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		setInt(mode, "lastscore", 1000); setInt(mode, "lastmultiplier", 4); setInt(mode, "scgettime", 30);
		mode.renderLast(e, 0);
	}
	@Test void renderLastGarbageAddDisplay() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		setInt(mode, "garbageAdd", 7); mode.renderLast(e, 0);
	}
	@Test void renderLastTimeLimitAddDisplay() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		setInt(mode, "timeLimitAddDisplay", 60); setInt(mode, "timeLimitAdd", 180); mode.renderLast(e, 0);
	}
	@Test void renderLastChainColorGreen() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded(); e.chain = 8; setInt(mode, "chainDisplay", 30);
		setInt(mode, "chainDisplayType", 2); setInt(mode, "feverChainDisplay", 6); e.gameActive = true;
		mode.renderLast(e, 0);
	}
	@Test void lineClearEndTimeLimitAdd() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded(); e.chain = 4; setBoolean(mode, "cleared", true);
		setInt(mode, "feverChainMin", 3); setInt(mode, "feverChainMax", 15);
		setInt(mode, "feverChain", 5); setInt(mode, "timeLimit", 3600);
		mode.lineClearEnd(e, 0); assertEquals(3720, readInt(mode, "timeLimit"));
	}
	@Test void lineClearEndOutOfTime() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		e.createFieldIfNeeded(); setBoolean(mode, "cleared", false);
		setInt(mode, "timeLimit", 0); e.timerActive = true;
		mode.lineClearEnd(e, 0); assertEquals(GameEngine.Status.ENDINGSTART, e.stat);
	}
	@Test void onLastFastenableClearMode() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		setInt(mode, "fastenable", 1); setBoolean(mode, "fastinuse", false);
		e.stat = GameEngine.Status.LINECLEAR; e.ctrl = new SimpleController(Controller.BUTTON_F);
		mode.onLast(e, 0);
	}
	@Test void onSettingReplayMode() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode(); GameEngine e = freshEngine(mode);
		e.owner.replayMode = true; setInt(mode, "menuTime", 59); assertFalse(mode.onSetting(e, 0));
	}

	private static GameEngine freshEngine(AvalancheFeverMode mode) {
		GameManager m = new GameManager(new EventReceiver()); m.mode = mode; m.init(); m.engine[0].init(); return m.engine[0];
	}
	private static int readInt(Object o, String n) throws Exception {
		java.lang.reflect.Field f = findField(o.getClass(), n); f.setAccessible(true); return f.getInt(o);
	}
	private static void setInt(Object o, String n, int v) throws Exception {
		java.lang.reflect.Field f = findField(o.getClass(), n); f.setAccessible(true); f.setInt(o, v);
	}
	private static void setBoolean(Object o, String n, boolean v) throws Exception {
		java.lang.reflect.Field f = findField(o.getClass(), n); f.setAccessible(true); f.setBoolean(o, v);
	}
	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try { return c.getDeclaredField(name); } catch (NoSuchFieldException e) { /* continue */ }
		}
		throw new NoSuchFieldException(name);
	}
	@SuppressWarnings("serial")
	private static class SimpleController extends Controller {
		private final int btn; SimpleController(int b) { super(); btn = b; }
		@Override public boolean isPush(int b) { return (btn & b) != 0; }
		@Override public boolean isMenuRepeatKey(int b) { return (btn & b) != 0; }
		@Override public boolean isPress(int b) { return (btn & b) != 0; }
	}
}
