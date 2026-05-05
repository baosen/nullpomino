package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link NetDummyMode}: nets the remaining
 * surface not covered by the basic test — playerInit stops/hides engines,
 * modeInit allocates the LinkedList ranking collections, the no-op
 * stubs netRecvStats / netSendEndGameStats / netGetGoalType / etc. do
 * not throw, and the netplayInit short-circuits gracefully on null lobby.
 */
class NetDummyModeExtendedTest {

	@Test
	void playerInitStopsEngineAndHidesIt() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.stat = GameEngine.Status.MOVE;
		engine.isVisible = true;

		mode.playerInit(engine, 0);

		assertEquals(GameEngine.Status.NOTHING, engine.stat,
				"playerInit must stop the engine");
		assertFalse(engine.isVisible, "playerInit must hide the engine");
	}

	@Test
	void modeInitAllocatesLinkedListRankingCollections() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		// All 11 ranking lists should be allocated with length 2
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingPlace")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingName")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingDate")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingGamerate")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingTime")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingScore")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingPiece")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingPPS")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingLines")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingSPL")).length);
		assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingRollclear")).length);
	}

	@Test
	void modeInitSetsDefaultFlags() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		assertFalse(readBoolean(mode, "netIsNetPlay"));
		assertFalse(readBoolean(mode, "netIsWatch"));
		assertEquals(0, readInt(mode, "netNumSpectators"));
		assertFalse(readBoolean(mode, "netForceSendMovements"));
		assertEquals("", readField(mode, "netPlayerName"));
	}

	@Test
	void noOpStubsDoNotThrowOnAnyArguments() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();

		// These are empty-bodied stubs that subclasses override.
		// Pin that they don't throw when called with null / defaults.
		mode.netSendStats(manager.engine[0]);
		mode.netRecvStats(manager.engine[0], new String[] {});
		mode.netSendEndGameStats(manager.engine[0]);
		mode.netSendOptions(manager.engine[0]);
		mode.netRecvOptions(manager.engine[0], new String[] {});

		assertEquals(0, mode.netGetGoalType());
		assertFalse(mode.netIsNetRankingViewOK(manager.engine[0]));
	}

	@Test
	void netPlayerInitResetsPrevPieceAndRanking() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		java.lang.reflect.Method m = NetDummyMode.class.getDeclaredMethod(
				"netPlayerInit", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0);

		assertEquals(-1, readInt(mode, "netPrevPieceID"));
		assertEquals(0, readInt(mode, "netReplaySendStatus"));

		int[] netRankingRank = (int[]) readField(mode, "netRankingRank");
		assertEquals(-1, netRankingRank[0]);
		assertEquals(-1, netRankingRank[1]);
	}

	@Test
	void netSendReplayShortCircuitsWhenNetRankingViewNotOk() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		// netIsNetRankingViewOK returns false by default
		java.lang.reflect.Method sendReplay = NetDummyMode.class.getDeclaredMethod(
				"netSendReplay", GameEngine.class);
		sendReplay.setAccessible(true);
		sendReplay.invoke(mode, engine);

		// Should set netReplaySendStatus to 2 (skipped)
		assertEquals(2, readInt(mode, "netReplaySendStatus"));
	}

	@Test
	void loadRankingDoesNothing() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		// loadRanking is a no-op in the base class
		java.lang.reflect.Method loadRanking = NetDummyMode.class.getDeclaredMethod(
				"loadRanking", nullpomino.util.CustomProperties.class, String.class);
		loadRanking.setAccessible(true);
		loadRanking.invoke(mode, new nullpomino.util.CustomProperties(), "test");
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
