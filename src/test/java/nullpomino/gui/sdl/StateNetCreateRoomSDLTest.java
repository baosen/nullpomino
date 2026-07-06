package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
	void tabLabelsAreSixOrderedSections() throws Exception {
		Field f = StateNetCreateRoomSDL.class.getDeclaredField("TAB_LABELS");
		f.setAccessible(true);
		assertArrayEquals(
				new String[] {"BASIC", "SPEED", "BONUS", "GARBAGE", "MISC", "PRESET"},
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
}
