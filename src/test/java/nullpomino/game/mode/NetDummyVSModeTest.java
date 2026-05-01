package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link NetDummyVSMode} surface that does not need a live
 * netplay session: the registry constants (name, isVSMode,
 * isNetplayMode, getPlayers), and the modeInit + netvsResetFlags
 * defaults that initialise the per-room arrays.
 */
class NetDummyVSModeTest {

	@Test
	void getNameReturnsRegistryLiteralForTheParentClass() {
		assertEquals("NET-VS-DUMMY", new NetDummyVSMode().getName());
	}

	@Test
	void isVSModeAndIsNetplayModeAreBothTrue() {
		NetDummyVSMode mode = new NetDummyVSMode();

		assertTrue(mode.isVSMode());
		assertTrue(mode.isNetplayMode());
	}

	@Test
	void getPlayersReportsNetVsMaxPlayersDefaultOfSix() {
		assertEquals(6, new NetDummyVSMode().getPlayers());
	}

	@Test
	void modeInitAllocatesEveryPerRoomArrayAtNetVsMaxPlayersLength() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		// Per-player arrays sized to NETVS_MAX_PLAYERS = 6.
		assertEquals(6, ((boolean[]) read(mode, "netvsPlayerExist")).length);
		assertEquals(6, ((boolean[]) read(mode, "netvsPlayerReady")).length);
		assertEquals(6, ((boolean[]) read(mode, "netvsPlayerActive")).length);
		assertEquals(6, ((int[]) read(mode, "netvsPlayerSeatID")).length);
		assertEquals(6, ((int[]) read(mode, "netvsPlayerUID")).length);
		assertEquals(6, ((int[]) read(mode, "netvsPlayerWinCount")).length);
		assertEquals(6, ((int[]) read(mode, "netvsPlayerPlayCount")).length);
		assertEquals(6, ((int[]) read(mode, "netvsPlayerTeamColor")).length);
		assertEquals(6, ((String[]) read(mode, "netvsPlayerName")).length);
		assertEquals(6, ((String[]) read(mode, "netvsPlayerTeam")).length);

		// netvsPlayerSkin is initialised to -1 across the board so a
		// missing 'set skin' wire keeps the server's default rather than
		// silently picking skin 0.
		int[] skin = (int[]) read(mode, "netvsPlayerSkin");
		assertEquals(6, skin.length);
		for(int v : skin) assertEquals(-1, v);
	}

	@Test
	void modeInitInstallsFreshSeatAndCounterDefaults() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertEquals(-1, readInt(mode, "netvsMySeatID"),
				"netvsMySeatID defaults to -1 (the no-seat marker)");
		assertEquals(0, readInt(mode, "netvsNumPlayers"));
		assertEquals(0, readInt(mode, "netvsNumNowPlayers"));
		assertEquals(0, readInt(mode, "netvsNumAlivePlayers"));
		assertFalse(readBoolean(mode, "netvsAutoStartTimerActive"));
		assertEquals(0, readInt(mode, "netvsAutoStartTimer"));
		assertEquals(-1, readInt(mode, "netvsMapPreviousPracticeMap"));
		assertTrue(readBoolean(mode, "netForceSendMovements"));
	}

	@Test
	void modeInitInvokesNetvsResetFlagsToZeroEveryActiveRoundFlag() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		// Per-round state should be cleared: no active game, no finish
		// pending, ready-change cleared, dead-pending cleared, newcomer
		// cleared, play timer off, practice off.
		assertFalse(readBoolean(mode, "netvsIsGameActive"));
		assertFalse(readBoolean(mode, "netvsIsGameFinished"));
		assertFalse(readBoolean(mode, "netvsIsReadyChangePending"));
		assertFalse(readBoolean(mode, "netvsIsDeadPending"));
		assertFalse(readBoolean(mode, "netvsIsNewcomer"));
		assertFalse(readBoolean(mode, "netvsPlayTimerActive"));
		assertFalse(readBoolean(mode, "netvsIsPractice"));

		// Per-round arrays exist and are sized to NETVS_MAX_PLAYERS.
		assertNotNull(read(mode, "netvsPlayerResultReceived"));
		assertNotNull(read(mode, "netvsPlayerDead"));
		assertNotNull(read(mode, "netvsPlayerPlace"));
	}

	private static Object read(NetDummyVSMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static int readInt(NetDummyVSMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(NetDummyVSMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
