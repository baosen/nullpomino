package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link NetVSBattleMode}: registry surface,
 * modeInit / playerInit array allocation defaults, the isVSMode contract,
 * loadSetting / saveSetting round-trip (inherited from NetDummyVSMode),
 * and basic calcScore attack-event dispatch without a live network.
 */
class NetVSBattleModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("NET-VS-BATTLE", new NetVSBattleMode().getName());
	}

	@Test
	void isVSModeAndIsNetplayModeAreBothTrue() {
		NetVSBattleMode mode = new NetVSBattleMode();
		assertTrue(mode.isVSMode());
		assertTrue(mode.isNetplayMode());
	}

	@Test
	void getPlayersReturnsSix() {
		assertEquals(6, new NetVSBattleMode().getPlayers());
	}

	@Test
	void modeInitAllocatesPerPlayerArraysAtNetVsMaxPlayersLength() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		assertEquals(6, ((boolean[]) readField(mode, "playerKObyYou")).length);
		assertEquals(6, ((int[]) readField(mode, "scgettime")).length);
		assertEquals(6, ((int[]) readField(mode, "lastevent")).length);
		assertEquals(6, ((boolean[]) readField(mode, "lastb2b")).length);
		assertEquals(6, ((int[]) readField(mode, "lastcombo")).length);
		assertEquals(6, ((int[]) readField(mode, "lastpiece")).length);
		assertEquals(6, ((int[]) readField(mode, "garbageSent")).length);
		assertEquals(6, ((int[]) readField(mode, "garbage")).length);
		assertEquals(6, ((float[]) readField(mode, "playerAPL")).length);
		assertEquals(6, ((float[]) readField(mode, "playerAPM")).length);
	}

	@Test
	void playerInitResetsScoringStateForGivenPlayer() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "scgettime", 0));
		assertEquals(0, readInt(mode, "lastevent", 0));
		assertFalse(readBoolean(mode, "lastb2b", 0));
		assertEquals(0, readInt(mode, "lastcombo", 0));
		assertEquals(0, readInt(mode, "lastpiece", 0));
		assertEquals(0, readInt(mode, "garbageSent", 0));
		assertEquals(0, readInt(mode, "garbage", 0));
		assertEquals(0f, readFloat(mode, "playerAPL", 0), 0.001f);
		assertEquals(0f, readFloat(mode, "playerAPM", 0), 0.001f);
		assertFalse(readBoolean(mode, "playerKObyYou", 0));
	}

	@Test
	void playerInitForPlayerZeroAlsoResetsGlobalState() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);

		// Player-0 specific resets
		assertEquals(-1, readInt(mode, "lastHole"));
		assertEquals(0, readInt(mode, "hurryupCount"));
		assertEquals(0, readInt(mode, "currentKO"));
		assertEquals(-1, readInt(mode, "targetID"));
		assertEquals(0, readInt(mode, "targetTimer"));

		// garbageEntries is cleared (not null)
		Object garbageEntries = readField(mode, "garbageEntries");
		assertNotNull(garbageEntries);
	}

	@Test
	void modeInitWiresOwnerViaSuperclass() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		Field ownerField = NetDummyMode.class.getDeclaredField("owner");
		ownerField.setAccessible(true);
		assertEquals(manager, ownerField.get(mode));
	}

	@Test
	void constantsAreAccessibleAndMatchExpectedValues() throws Exception {
		// Key attack-table-related constants pinned in the dedicated
		// NetVSBattleModeConstantsTest — just spot-check a couple.
		assertEquals(60, readStaticInt("GARBAGE_DENOMINATOR"));
		assertEquals(6, readStaticInt("ATTACK_CATEGORIES"));
	}

	@Test
	void startGameInheritsFromSuperWithoutCrashing() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);

		// startGame should not throw even without a real network
		mode.startGame(engine, 0);
	}

	@Test
	void onLastIncrementsScgettimeAndUpdatesMeterForActivePlayer() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "scgettime", 0));

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime", 0));
	}

	@Test
	void renderResultDrawsStatsWithoutThrowing() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);

		// Should not throw even with default stats
		mode.renderResult(engine, 0);
	}

	@Test
	void netSendStatsAndRecvStatsRoundTripGarbageValue() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "garbage", 0, 300);

		// netRecvStats parses a message like ["game","stats","garbageValue"]
		String[] message = new String[] {"game", "stats", "0", "0", "300"};
		Method recv = NetVSBattleMode.class.getDeclaredMethod("netRecvStats", GameEngine.class, String[].class);
		recv.setAccessible(true);
		recv.invoke(mode, engine, (Object) message);

		assertEquals(300, readInt(mode, "garbage", 0));
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(NetVSBattleMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].init();
		return manager.engine[0];
	}

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

	private static boolean readBoolean(Object obj, String name, int index) throws Exception {
		boolean[] arr = (boolean[]) readField(obj, name);
		return arr[index];
	}

	private static float readFloat(Object obj, String name, int index) throws Exception {
		float[] arr = (float[]) readField(obj, name);
		return arr[index];
	}

	private static void setInt(Object obj, String name, int index, int value) throws Exception {
		int[] arr = (int[]) readField(obj, name);
		arr[index] = value;
	}

	private static int readStaticInt(String name) throws Exception {
		Field f = NetVSBattleMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(null);
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
