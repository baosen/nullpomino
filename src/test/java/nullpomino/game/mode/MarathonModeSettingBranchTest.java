package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers remaining onSetting branches in {@link MarathonMode}: the goaltype-change
 * startlevel clamp, the boolean toggle cases from their opposite state, and the
 * netplay option-send / start / ranking paths.
 */
class MarathonModeSettingBranchTest {

	/** EventReceiver whose config persistence is a no-op (keeps config/setting clean). */
	private static final class NonPersistingReceiver extends EventReceiver {
		@Override public void saveModeConfig(CustomProperties c) { }
	}

	private static GameEngine freshEngine(MarathonMode mode) {
		GameManager manager = new GameManager(new NonPersistingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void press(GameEngine engine, int button) {
		engine.ctrl.reset();
		engine.ctrl.buttonPress[button] = true;
		engine.ctrl.buttonTime[button] = 1;
	}

	@Test
	void goaltypeChangeClampsStartlevelToNewGoalMax() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// goaltype 1 (200 lines -> max level 19), startlevel 19; move to goaltype 0
		// (150 lines -> max level 14): startlevel 19 > 14 with a line limit -> clamp to 14.
		setInt(mode, "goaltype", 1);
		setInt(mode, "startlevel", 19);
		setInt(mode, "menuCursor", 7);
		setInt(mode, "menuTime", 0);
		press(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(0, readInt(mode, "goaltype"));
		assertEquals(14, readInt(mode, "startlevel"));
	}

	@Test
	void booleanTogglesFlipFromOppositeState() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// cursor 2 enableTSpinKick, 4 tspinEnableEZ, 5 enableB2B, 6 enableCombo, 8 big
		int[] cursors = {2, 4, 5, 6, 8};
		String[] fields = {"enableTSpinKick", "tspinEnableEZ", "enableB2B", "enableCombo", "big"};
		for(int i = 0; i < cursors.length; i++) {
			setBool(mode, fields[i], true);
			setInt(mode, "menuCursor", cursors[i]);
			setInt(mode, "menuTime", 0);
			press(engine, Controller.BUTTON_RIGHT);
			mode.onSetting(engine, 0);
			assertFalse(readBool(mode, fields[i]), fields[i] + " should toggle true->false");
		}
	}

	@Test
	void netOptionChangeAndStartAndRankingPaths() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		wireNetLobby(mode);
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 1);

		// L176: a menu value change with spectators present -> netSendOptions
		setInt(mode, "menuCursor", 1);
		setInt(mode, "menuTime", 0);
		press(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		// L188: confirm in net play sends start1p and returns false
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);
		assertFalse(mode.onSetting(engine, 0));
	}

	@Test
	void confirmIgnoredWhenMenuTimeTooLow() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// BUTTON_A with menuTime < 5 -> the (menuTime >= 5) guard is false, no return
		setInt(mode, "menuTime", 0);
		press(engine, Controller.BUTTON_A);
		assertTrue(mode.onSetting(engine, 0));
	}

	// --- reflection helpers ---
	private static void wireNetLobby(Object mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		field(mode.getClass(), "netLobby").set(mode, lobby);
	}

	private static void setInt(Object o, String n, int v) throws Exception { field(o.getClass(), n).setInt(o, v); }
	private static void setBool(Object o, String n, boolean v) throws Exception { field(o.getClass(), n).setBoolean(o, v); }
	private static int readInt(Object o, String n) throws Exception { return field(o.getClass(), n).getInt(o); }
	private static boolean readBool(Object o, String n) throws Exception { return field(o.getClass(), n).getBoolean(o); }

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
