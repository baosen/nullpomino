package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link ExtremeMode#onSetting} so the still-uncovered branch outcomes
 * fire. The existing Extreme tests only toggle the boolean options from
 * {@code false}, so the {@code x = !x} negation lines (cases 2/4/5/6/7/8) are
 * never executed with the field starting at {@code true}; here every boolean
 * cursor is flipped from {@code true} back to {@code false}. Also covers the
 * remaining net/confirm/cancel guard combinations in onSetting:
 * <ul>
 *   <li>L185 netIsNetPlay true but no spectators (netSendOptions skipped)</li>
 *   <li>L191 BUTTON_A pressed but menuTime &lt; 5 (decide skipped)</li>
 *   <li>L197 BUTTON_A confirm in net play (start1p sent)</li>
 *   <li>L203 BUTTON_B pressed while in net play (cancel skipped)</li>
 *   <li>L208 BUTTON_D guard chain (netIsNetPlay false short-circuit etc.)</li>
 * </ul>
 */
class ExtremeModeSettingWraparoundTest {

	// ------------------------------------------------------------------
	//  Boolean toggle cases flipped from the true side (lines 160-180)
	// ------------------------------------------------------------------

	@Test
	void booleanCursorsToggleFromTrueToFalse() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		// case 2: enableTSpinKick  (line 160)
		setBool(mode, "enableTSpinKick", true);
		changeRight(mode, engine, 2);
		assertFalse(readBool(mode, "enableTSpinKick"));

		// case 4: tspinEnableEZ    (line 168)
		setBool(mode, "tspinEnableEZ", true);
		changeRight(mode, engine, 4);
		assertFalse(readBool(mode, "tspinEnableEZ"));

		// case 5: enableB2B        (line 171)
		setBool(mode, "enableB2B", true);
		changeRight(mode, engine, 5);
		assertFalse(readBool(mode, "enableB2B"));

		// case 6: enableCombo      (line 174)
		setBool(mode, "enableCombo", true);
		changeRight(mode, engine, 6);
		assertFalse(readBool(mode, "enableCombo"));

		// case 7: endless          (line 177)
		setBool(mode, "endless", true);
		changeRight(mode, engine, 7);
		assertFalse(readBool(mode, "endless"));

		// case 8: big              (line 180)
		setBool(mode, "big", true);
		changeRight(mode, engine, 8);
		assertFalse(readBool(mode, "big"));
	}

	// ------------------------------------------------------------------
	//  L185: netIsNetPlay true but zero spectators -> netSendOptions skipped
	// ------------------------------------------------------------------

	@Test
	void onSettingChangeWithNetPlayButNoSpectatorsSkipsSend() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 0); // second operand false

		setBool(mode, "endless", false);
		changeRight(mode, engine, 7);
		// option changed but no send attempted (no netLobby wired -> would NPE if sent)
		assertTrue(readBool(mode, "endless"));
	}

	// ------------------------------------------------------------------
	//  L191: BUTTON_A pushed but menuTime < 5 -> decide not taken
	// ------------------------------------------------------------------

	@Test
	void onSettingConfirmIgnoredBeforeMenuTimeThreshold() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBool(mode, "netIsNetPlay", false);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0); // < 5, so the decide branch is skipped
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		// stays on the settings screen because menuTime < 5
		assertTrue(mode.onSetting(engine, 0));
	}

	// ------------------------------------------------------------------
	//  L197: BUTTON_A confirm while in net play -> start1p sent
	// ------------------------------------------------------------------

	@Test
	void onSettingConfirmInNetPlaySendsStart() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		wireNetLobby(mode);
		setBool(mode, "netIsNetPlay", true);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10); // >= 5
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		// onSetting returns false (start the game) and sends "start1p" via the
		// wired (unconnected) client which simply swallows the send.
		assertFalse(mode.onSetting(engine, 0));
	}

	// ------------------------------------------------------------------
	//  L203: BUTTON_B while in net play -> quit branch NOT taken
	// ------------------------------------------------------------------

	@Test
	void onSettingCancelInNetPlayDoesNotQuit() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBool(mode, "netIsNetPlay", true);

		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;

		mode.onSetting(engine, 0);
		// !netIsNetPlay is false, so quitflag must stay clear
		assertFalse(engine.quitflag);
	}

	// ------------------------------------------------------------------
	//  L208: BUTTON_D guard chain (offline -> short-circuits at netIsNetPlay)
	// ------------------------------------------------------------------

	@Test
	void onSettingDButtonOfflineDoesNotEnterNetRanking() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBool(mode, "netIsNetPlay", false); // second operand false -> short-circuit

		setInt(mode, "startlevel", 0);
		setBool(mode, "big", false);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

		mode.onSetting(engine, 0);
		assertFalse(readBool(mode, "netIsNetRankingDisplayMode"));
	}

	@Test
	void onSettingDButtonNetPlayButNonZeroLevelDoesNotEnterNetRanking() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		wireNetLobby(mode);
		setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "startlevel", 5); // startlevel == 0 is false -> guard fails
		setBool(mode, "big", false);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

		mode.onSetting(engine, 0);
		assertFalse(readBool(mode, "netIsNetRankingDisplayMode"));
	}

	@Test
	void onSettingDButtonNetPlayAllGuardsPassEntersNetRanking() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		wireNetLobby(mode);
		setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "startlevel", 0);
		setBool(mode, "big", false);
		engine.ai = null;
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

		mode.onSetting(engine, 0);
		assertTrue(readBool(mode, "netIsNetRankingDisplayMode"));
	}

	// ------------------------------------------------------------------
	//  Helpers
	// ------------------------------------------------------------------

	private static GameEngine freshEngine(ExtremeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single RIGHT press, then runs onSetting. */
	private static void changeRight(ExtremeMode mode, GameEngine engine, int cursor) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		mode.onSetting(engine, 0);
	}

	private static void wireNetLobby(Object mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setField(mode, "netLobby", lobby);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		field(obj.getClass(), name).set(obj, value);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
	}

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
