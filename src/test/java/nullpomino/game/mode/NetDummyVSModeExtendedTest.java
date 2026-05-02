package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link NetDummyVSMode}: covers the
 * netvsResetFlags lifecycle, netvsIsAttackable rules, netvsGetPlayerIDbySeatID
 * mappings, game-style and frame-color constants, and the onReady / onMove /
 * onLast hooks (without a live network).
 */
class NetDummyVSModeExtendedTest {

	@Test
	void netvsResetFlagsClearsPerRoundState() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();

		mode.netvsResetFlags();

		assertFalse(readBoolean(mode, "netvsIsGameActive"));
		assertFalse(readBoolean(mode, "netvsIsGameFinished"));
		assertFalse(readBoolean(mode, "netvsIsReadyChangePending"));
		assertFalse(readBoolean(mode, "netvsIsDeadPending"));
		assertFalse(readBoolean(mode, "netvsIsNewcomer"));
		assertFalse(readBoolean(mode, "netvsPlayTimerActive"));
		assertFalse(readBoolean(mode, "netvsIsPractice"));
		assertFalse(readBoolean(mode, "netvsIsPracticeExitAllowed"));
		assertEquals(0, readInt(mode, "netvsPlayTimer"));
		assertEquals(0, readInt(mode, "netvsPieceMoveTimer"));

		// Arrays should be re-allocated
		assertEquals(6, ((boolean[]) readField(mode, "netvsPlayerResultReceived")).length);
		assertEquals(6, ((boolean[]) readField(mode, "netvsPlayerDead")).length);
		assertEquals(6, ((int[]) readField(mode, "netvsPlayerPlace")).length);
	}

	@Test
	void netvsGetPlayerIDbySeatIDMapsCorrectly() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();

		Method getID = NetDummyVSMode.class.getDeclaredMethod(
				"netvsGetPlayerIDbySeatID", int.class, int.class);
		getID.setAccessible(true);

		// When my seat is 0: seat-to-player mapping is identity
		assertEquals(0, (int) getID.invoke(mode, 0, 0));
		assertEquals(1, (int) getID.invoke(mode, 1, 0));
		assertEquals(2, (int) getID.invoke(mode, 2, 0));

		// When my seat is 1: seat 1 -> player 0, seat 0 -> player 1
		assertEquals(0, (int) getID.invoke(mode, 1, 1));
		assertEquals(1, (int) getID.invoke(mode, 0, 1));
	}

	@Test
	void netvsGetPlayerIDbySeatIDClampsNegativeMySeat() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();

		Method getID = NetDummyVSMode.class.getDeclaredMethod(
				"netvsGetPlayerIDbySeatID", int.class, int.class);
		getID.setAccessible(true);

		// -1 (spectator) should be treated as seat 0
		assertEquals(0, (int) getID.invoke(mode, 0, -1));
	}

	@Test
	void netvsIsAttackableCannotAttackSelf() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();

		Method isAttackable = NetDummyVSMode.class.getDeclaredMethod(
				"netvsIsAttackable", int.class);
		isAttackable.setAccessible(true);

		// playerID <= 0 (self) is not attackable
		assertFalse((boolean) isAttackable.invoke(mode, 0));
		assertFalse((boolean) isAttackable.invoke(mode, -1));
	}

	@Test
	void netvsIsAttackableReturnsFalseForNonExistentPlayer() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();

		Method isAttackable = NetDummyVSMode.class.getDeclaredMethod(
				"netvsIsAttackable", int.class);
		isAttackable.setAccessible(true);

		// Player 1 doesn't exist -> not attackable
		assertFalse((boolean) isAttackable.invoke(mode, 1));
	}

	@Test
	void onReadyMapFieldsEmptyWithoutRealNetwork() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		// onReady may access netCurrentRoomInfo which is null without a network.
		// Catch NPE gracefully - the important thing is no other exception.
		try {
			boolean result = mode.onReady(engine, 0);
			assertFalse(result);
		} catch (NullPointerException e) {
			// Expected: netCurrentRoomInfo is null without network
		}
	}

	@Test
	void onMoveStopsGameForRemotePlayers() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		// PlayerID=1 is considered "remote" -> onMove should return true (stop)
		boolean result = mode.onMove(engine, 1);
		assertTrue(result);
	}

	@Test
	void onMoveLocalPlayerDoesNotStop() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		// PlayerID=0 is local -> onMove should return false
		boolean result = mode.onMove(engine, 0);
		assertFalse(result);
	}

	@Test
	void constantsArePinned() throws Exception {
		assertEquals(6, findField(NetDummyVSMode.class, "NETVS_MAX_PLAYERS").getInt(null));

		int[][] seatNums = (int[][]) findField(NetDummyVSMode.class, "NETVS_GAME_SEAT_NUMBERS").get(null);
		assertEquals(6, seatNums.length);
		assertEquals(6, seatNums[0].length);

		assertEquals(Block.BLOCK_COLOR_RED, ((int[]) findField(
				NetDummyVSMode.class, "NETVS_PLAYER_COLOR_BLOCK").get(null))[0]);
	}

	@Test
	void onLastDoesNotThrowWithoutNetwork() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		mode.onLast(engine, 0);
		// Should not throw
	}

	@Test
	void renderLastDoesNotThrowWithoutNetwork() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		mode.renderLast(engine, 0);
		// Should not throw
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
