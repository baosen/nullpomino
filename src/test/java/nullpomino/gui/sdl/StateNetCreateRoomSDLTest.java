package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.net.NetRoomInfo;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.net.NetLobbyFrame.RoomCreateMode;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless static helpers on {@link StateNetCreateRoomSDL}:
 * the tab labels, the dropdown choice arrays, and the two private
 * static decoders that map a room's flags to its construction mode and
 * read the last-used mode out of propConfig with a fallback.
 *
 * <p>The full UI needs a NetLobbyFrame + SDL renderer; these slices
 * don't, so we exercise them via reflection.
 */
class StateNetCreateRoomSDLTest {

	@Test
	void tabLabelsAreFiveOrderedSections() throws Exception {
		// MISC was folded into the BASIC tab, leaving five sections.
		Field f = StateNetCreateRoomSDL.class.getDeclaredField("TAB_LABELS");
		f.setAccessible(true);
		assertArrayEquals(
				new String[] {"BASIC", "SPEED", "BONUS", "GARBAGE", "PRESET"},
				(String[]) f.get(null));
	}

	@Test
	void tspinTypeLabelsListThreeChoicesInTSpinEngineOrder() throws Exception {
		Field f = StateNetCreateRoomSDL.class.getDeclaredField("TSPIN_TYPE_LABELS");
		f.setAccessible(true);
		assertArrayEquals(
				new String[] {"DISABLE", "T-ONLY", "ALL SPIN"},
				(String[]) f.get(null));
	}

	@Test
	void spinCheckLabelsListTheTwoSpinDetectionAlgorithms() throws Exception {
		Field f = StateNetCreateRoomSDL.class.getDeclaredField("SPIN_CHECK_LABELS");
		f.setAccessible(true);
		assertArrayEquals(
				new String[] {"4-POINT", "IMMOBILE"},
				(String[]) f.get(null));
	}

	@Test
	void modeTypeLabelsListMultiplayerAndSinglePlayerInEnumOrder() throws Exception {
		Field f = StateNetCreateRoomSDL.class.getDeclaredField("MODE_TYPE_LABELS");
		f.setAccessible(true);
		String[] labels = (String[]) f.get(null);

		// RATED (RoomCreateMode ordinal 2) is intentionally omitted — preset-based
		// rated rooms are a dedicated-server concept P2P room sessions can't create.
		assertArrayEquals(
				new String[] {"MULTIPLAYER", "SINGLE PLAYER"},
				labels);
		assertEquals(2, labels.length);
	}

	@Test
	void modeFromRoomInfoReturnsMultiplayerForNullRoom() throws Exception {
		// Defensive null-handling: caller passes the resolved source which can
		// still be null when no room is selected and we're creating fresh.
		assertEquals(RoomCreateMode.MULTIPLAYER, invokeModeFromRoomInfo(null));
	}

	@Test
	void modeFromRoomInfoReturnsRatedForServerSideRatedRoom() throws Exception {
		NetRoomInfo r = new NetRoomInfo();
		r.rated = true;
		r.customRated = false;
		r.singleplayer = false;

		assertEquals(RoomCreateMode.RATED, invokeModeFromRoomInfo(r));
	}

	@Test
	void modeFromRoomInfoFallsBackToMultiplayerForCustomRated() throws Exception {
		// A custom-rated room is rated by user request, not by the server's
		// preset list — it should NOT round-trip back into the RATED branch
		// because the preset dropdown won't have anything to bind to.
		NetRoomInfo r = new NetRoomInfo();
		r.rated = true;
		r.customRated = true;

		assertEquals(RoomCreateMode.MULTIPLAYER, invokeModeFromRoomInfo(r));
	}

	@Test
	void modeFromRoomInfoReturnsSinglePlayerForOnePlayerRoom() throws Exception {
		NetRoomInfo r = new NetRoomInfo();
		r.singleplayer = true;
		r.rated = false;

		assertEquals(RoomCreateMode.SINGLE_PLAYER, invokeModeFromRoomInfo(r));
	}

	@Test
	void modeFromRoomInfoReturnsMultiplayerForPlainRoom() throws Exception {
		NetRoomInfo r = new NetRoomInfo();
		// All flags default false -> plain multiplayer.

		assertEquals(RoomCreateMode.MULTIPLAYER, invokeModeFromRoomInfo(r));
	}

	@Test
	void readLastModeFromConfigDefaultsToMultiplayerWhenKeyMissing() throws Exception {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propConfig = new CustomProperties();

		assertEquals(RoomCreateMode.MULTIPLAYER, invokeReadLastModeFromConfig(nl));
	}

	@Test
	void readLastModeFromConfigReadsAnyEnumName() throws Exception {
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propConfig = new CustomProperties();
		nl.propConfig.setProperty("createroom.lastMode", "RATED");
		assertEquals(RoomCreateMode.RATED, invokeReadLastModeFromConfig(nl));

		nl.propConfig.setProperty("createroom.lastMode", "SINGLE_PLAYER");
		assertEquals(RoomCreateMode.SINGLE_PLAYER, invokeReadLastModeFromConfig(nl));

		nl.propConfig.setProperty("createroom.lastMode", "MULTIPLAYER");
		assertEquals(RoomCreateMode.MULTIPLAYER, invokeReadLastModeFromConfig(nl));
	}

	@Test
	void readLastModeFromConfigFallsBackToMultiplayerOnGarbageValue() throws Exception {
		// A typo or a removed enum value must not crash the screen — fall
		// back to MULTIPLAYER and let the user pick again.
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propConfig = new CustomProperties();
		nl.propConfig.setProperty("createroom.lastMode", "TOTALLY_UNKNOWN_MODE");

		assertEquals(RoomCreateMode.MULTIPLAYER, invokeReadLastModeFromConfig(nl));
	}

	@Test
	void loadDefaultsFromConfigAppliesHardcodedDefaultsWhenConfigIsEmpty() throws Exception {
		// Pins every hardcoded "createroom.default*" fallback used the first
		// time a user opens CREATE ROOM (before anything has been saved).
		// In particular, MAX PLAYERS defaults to 2 (most rooms are 1v1), not
		// the historical 6.
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propConfig = new CustomProperties();
		NetRoomInfo r = new NetRoomInfo();

		invokeLoadDefaultsFromConfig(nl, r);

		assertEquals(2, r.maxPlayers);
		assertEquals(15, r.autoStartSeconds);
		assertEquals(1, r.gravity);
		assertEquals(60, r.denominator);
		assertEquals(0, r.are);
		assertEquals(0, r.areLine);
		assertEquals(0, r.lineDelay);
		assertEquals(30, r.lockDelay);
		assertEquals(11, r.das);
		assertEquals(180, r.hurryupSeconds);
		assertEquals(5, r.hurryupInterval);
		assertEquals(90, r.garbagePercent);
		assertEquals(60, r.targetTimer);
		assertFalse(r.ruleLock);
		assertEquals(1, r.tspinEnableType);
		assertEquals(0, r.spinCheckType);
		assertFalse(r.tspinEnableEZ);
		assertTrue(r.b2b);
		assertTrue(r.combo);
		assertTrue(r.rensaBlock);
		assertTrue(r.counter);
		assertTrue(r.bravo);
		assertTrue(r.reduceLineSend);
		assertTrue(r.garbageChangePerAttack);
		assertFalse(r.divideChangeRateByPlayers);
		assertFalse(r.b2bChunk);
		assertFalse(r.useFractionalGarbage);
		assertFalse(r.isTarget);
		assertFalse(r.autoStartTNET2);
		assertFalse(r.disableTimerAfterSomeoneCancelled);
		assertFalse(r.useMap);
	}

	@Test
	void loadDefaultsFromConfigPrefersSavedConfigOverHardcodedDefaults() throws Exception {
		// Once a user has created a room, their choices are saved back under
		// these same keys (see saveDefaultsToConfig) and should win over the
		// hardcoded fallback on every subsequent visit to the form.
		NetLobbyFrame nl = new NetLobbyFrame();
		nl.propConfig = new CustomProperties();
		nl.propConfig.setProperty("createroom.defaultMaxPlayers", 4);
		nl.propConfig.setProperty("createroom.defaultAutoStartSeconds", 99);
		nl.propConfig.setProperty("createroom.defaultGravity", 5);
		nl.propConfig.setProperty("createroom.defaultDenominator", 120);
		nl.propConfig.setProperty("createroom.defaultARE", 20);
		nl.propConfig.setProperty("createroom.defaultARELine", 25);
		nl.propConfig.setProperty("createroom.defaultLineDelay", 35);
		nl.propConfig.setProperty("createroom.defaultLockDelay", 40);
		nl.propConfig.setProperty("createroom.defaultDAS", 8);
		nl.propConfig.setProperty("createroom.defaultHurryupSeconds", 60);
		nl.propConfig.setProperty("createroom.defaultHurryupInterval", 10);
		nl.propConfig.setProperty("createroom.defaultGarbagePercent", 50);
		nl.propConfig.setProperty("createroom.defaultTargetTimer", 30);
		nl.propConfig.setProperty("createroom.defaultRuleLock", true);
		nl.propConfig.setProperty("createroom.defaultTSpinEnableType", 2);
		nl.propConfig.setProperty("createroom.defaultSpinCheckType", 1);
		nl.propConfig.setProperty("createroom.defaultTSpinEnableEZ", true);
		nl.propConfig.setProperty("createroom.defaultB2B", false);
		nl.propConfig.setProperty("createroom.defaultCombo", false);
		nl.propConfig.setProperty("createroom.defaultRensaBlock", false);
		nl.propConfig.setProperty("createroom.defaultCounter", false);
		nl.propConfig.setProperty("createroom.defaultBravo", false);
		nl.propConfig.setProperty("createroom.defaultReduceLineSend", false);
		nl.propConfig.setProperty("createroom.defaultGarbageChangePerAttack", false);
		nl.propConfig.setProperty("createroom.defaultDivideChangeRateByPlayers", true);
		nl.propConfig.setProperty("createroom.defaultB2BChunk", true);
		nl.propConfig.setProperty("createroom.defaultUseFractionalGarbage", true);
		nl.propConfig.setProperty("createroom.defaultIsTarget", true);
		nl.propConfig.setProperty("createroom.defaultAutoStartTNET2", true);
		nl.propConfig.setProperty("createroom.defaultDisableTimerAfterSomeoneCancelled", true);
		nl.propConfig.setProperty("createroom.defaultUseMap", true);
		NetRoomInfo r = new NetRoomInfo();

		invokeLoadDefaultsFromConfig(nl, r);

		assertEquals(4, r.maxPlayers);
		assertEquals(99, r.autoStartSeconds);
		assertEquals(5, r.gravity);
		assertEquals(120, r.denominator);
		assertEquals(20, r.are);
		assertEquals(25, r.areLine);
		assertEquals(35, r.lineDelay);
		assertEquals(40, r.lockDelay);
		assertEquals(8, r.das);
		assertEquals(60, r.hurryupSeconds);
		assertEquals(10, r.hurryupInterval);
		assertEquals(50, r.garbagePercent);
		assertEquals(30, r.targetTimer);
		assertTrue(r.ruleLock);
		assertEquals(2, r.tspinEnableType);
		assertEquals(1, r.spinCheckType);
		assertTrue(r.tspinEnableEZ);
		assertFalse(r.b2b);
		assertFalse(r.combo);
		assertFalse(r.rensaBlock);
		assertFalse(r.counter);
		assertFalse(r.bravo);
		assertFalse(r.reduceLineSend);
		assertFalse(r.garbageChangePerAttack);
		assertTrue(r.divideChangeRateByPlayers);
		assertTrue(r.b2bChunk);
		assertTrue(r.useFractionalGarbage);
		assertTrue(r.isTarget);
		assertTrue(r.autoStartTNET2);
		assertTrue(r.disableTimerAfterSomeoneCancelled);
		assertTrue(r.useMap);
	}

	private static RoomCreateMode invokeModeFromRoomInfo(NetRoomInfo r) throws Exception {
		Method m = StateNetCreateRoomSDL.class.getDeclaredMethod(
				"modeFromRoomInfo", NetRoomInfo.class);
		m.setAccessible(true);
		return (RoomCreateMode) m.invoke(null, r);
	}

	private static RoomCreateMode invokeReadLastModeFromConfig(NetLobbyFrame nl) throws Exception {
		Method m = StateNetCreateRoomSDL.class.getDeclaredMethod(
				"readLastModeFromConfig", NetLobbyFrame.class);
		m.setAccessible(true);
		return (RoomCreateMode) m.invoke(null, nl);
	}

	private static void invokeLoadDefaultsFromConfig(NetLobbyFrame nl, NetRoomInfo r) throws Exception {
		Method m = StateNetCreateRoomSDL.class.getDeclaredMethod(
				"loadDefaultsFromConfig", NetLobbyFrame.class, NetRoomInfo.class);
		m.setAccessible(true);
		m.invoke(new StateNetCreateRoomSDL(), nl, r);
	}
}
