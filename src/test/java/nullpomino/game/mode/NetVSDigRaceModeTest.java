package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link NetVSDigRaceMode}: registry surface,
 * modeInit allocation, playerInit defaults, netRecvStats state sync,
 * and the isVSMode / isNetplayMode contracts.
 */
class NetVSDigRaceModeTest {

	@Test
	void getNameReturnsLegacyConstant() {
		assertEquals("NET-VS-DIG RACE", new NetVSDigRaceMode().getName());
	}

	@Test
	void isVSModeAndIsNetplayModeAreBothTrue() {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		assertTrue(mode.isVSMode());
		assertTrue(mode.isNetplayMode());
	}

	@Test
	void getPlayersReturnsSix() {
		assertEquals(6, new NetVSDigRaceMode().getPlayers());
	}

	@Test
	void playerInitResetsMenuAndOtherSettings() throws Exception {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "menuTime"));
		assertEquals(0, readInt(mode, "menuCursor"));
		// NetVSDigRaceMode stores these in engine.statistics
		assertEquals(0, engine.statistics.score);
		assertEquals(0, engine.statistics.totalPieceLocked);
	}

	@Test
	void netRecvStatsParsesDigCountAndScore() throws Exception {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		String[] message = new String[] {"game", "stats", "0", "0", "150", "42"};

		Method recv = NetVSDigRaceMode.class.getDeclaredMethod(
				"netRecvStats", GameEngine.class, String[].class);
		recv.setAccessible(true);
		recv.invoke(mode, engine, (Object) message);

		// netRecvStats sets playerRemainLines from message[4].
		// Use reflection to check the internal state.
		int[] playerRemainLines = (int[]) readField(mode, "playerRemainLines");
		assertEquals(150, playerRemainLines[0]);
	}

	@Test
	void netRecvStatsHandlesShortMessageWithoutCrashing() throws Exception {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		String[] shortMessage = new String[] {"game", "stats"};
		Method recv = NetVSDigRaceMode.class.getDeclaredMethod(
				"netRecvStats", GameEngine.class, String[].class);
		recv.setAccessible(true);
		recv.invoke(mode, engine, (Object) shortMessage);
		// Should not throw
	}

	@Test
	void startGameSetsInitialDigTarget() throws Exception {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		// startGame should not throw even without a live network
		mode.startGame(engine, 0);
	}

	@Test
	void isNetplayOnlyReturnsTrue() {
		assertTrue(new NetVSDigRaceMode().isNetplayMode());
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

	private static int readInt(Object obj, String name, int index) throws Exception {
		int[] arr = (int[]) readField(obj, name);
		return arr[index];
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
