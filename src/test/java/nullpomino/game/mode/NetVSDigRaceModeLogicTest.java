package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetVSDigRaceMode} logic that does not require a live
 * netplay session: registry-facing constants, the private
 * {@code getRemainGarbageLines} counting method, the private
 * {@code getNowPlayerPlace} ranking method, and the private
 * {@code updateMeter} progress-bar method.
 */
class NetVSDigRaceModeLogicTest {

	private NetVSDigRaceMode mode;
	private GameManager manager;

	@BeforeEach
	void setUp() {
		mode = new NetVSDigRaceMode();
		manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		// Initialise all engines so their statistics objects exist and
		// field references are ready for createFieldIfNeeded.
		for (int i = 0; i < manager.engine.length; i++) {
			manager.engine[i].init();
		}
		// modeInit allocates per-player arrays and seeds goalLines=18.
		mode.modeInit(manager);
	}

	// ========================
	// Registry constants
	// ========================

	@Test
	void getNameReturnsNetVSDigRace() {
		assertEquals("NET-VS-DIG RACE", mode.getName());
	}

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(mode.isVSMode());
	}

	@Test
	void isNetplayModeReturnsTrue() {
		assertTrue(mode.isNetplayMode());
	}

	@Test
	void getPlayersReturnsSix() {
		assertEquals(6, mode.getPlayers());
	}

	// ========================
	// getRemainGarbageLines (private, tested via reflection)
	// ========================

	@Test
	void getRemainGarbageLinesReturnsMinusOneForNullEngine() throws Exception {
		assertEquals(-1, callGetRemainGarbageLines(null, 0));
	}

	@Test
	void getRemainGarbageLinesReturnsMinusOneForNullField() throws Exception {
		GameEngine engine = manager.engine[0];
		engine.field = null;
		assertEquals(-1, callGetRemainGarbageLines(engine, 0));
	}

	@Test
	void getRemainGarbageLinesReturnsZeroWhenNoGemBlocks() throws Exception {
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		int w = engine.field.getWidth();
		int h = engine.field.getHeight();

		// Fill bottom goalLines rows with garbage blocks but NO gem blocks.
		// goalLines defaults to 18 after modeInit, so fill rows 2..19.
		for (int y = h - 18; y < h; y++) {
			for (int x = 0; x < w; x++) {
				engine.field.setBlock(x, y, new Block(
						Block.BLOCK_COLOR_GRAY, 0,
						Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
			}
		}

		// No gem blocks anywhere in goalLines range => return 0.
		assertEquals(0, callGetRemainGarbageLines(engine, 0));
	}

	@Test
	void getRemainGarbageLinesReturnsCountOfGarbageRowsWithGems() throws Exception {
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		int w = engine.field.getWidth();
		int h = engine.field.getHeight();

		// Fill bottom 3 rows (17, 18, 19) with garbage blocks, each row
		// containing one gem block at x=0 to satisfy hasGemBlock.
		for (int y = h - 3; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int color = (x == 0) ? Block.BLOCK_COLOR_GEM_RED : Block.BLOCK_COLOR_GRAY;
				engine.field.setBlock(x, y, new Block(
						color, 0,
						Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
			}
		}

		// 3 rows each have at least one garbage block => 3.
		assertEquals(3, callGetRemainGarbageLines(engine, 0));
	}

	@Test
	void getRemainGarbageLinesSkipsLineFlaggedRows() throws Exception {
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		int w = engine.field.getWidth();
		int h = engine.field.getHeight();

		// Fill bottom 3 rows.
		for (int y = h - 3; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int color = (x == 0) ? Block.BLOCK_COLOR_GEM_RED : Block.BLOCK_COLOR_GRAY;
				engine.field.setBlock(x, y, new Block(
						color, 0,
						Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
			}
		}

		// Mark row 18 (h-2) as cleared so it gets skipped.
		engine.field.setLineFlag(h - 2, true);

		// Rows 17 and 19 still count => 2 garbage lines.
		assertEquals(2, callGetRemainGarbageLines(engine, 0));
	}

	@Test
	void getRemainGarbageLinesOnlyCountsRowsWithGarbageAttribute() throws Exception {
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		int w = engine.field.getWidth();
		int h = engine.field.getHeight();

		// Row 19: garbage + gem block.
		for (int x = 0; x < w; x++) {
			int color = (x == 0) ? Block.BLOCK_COLOR_GEM_RED : Block.BLOCK_COLOR_GRAY;
			engine.field.setBlock(x, h - 1, new Block(
					color, 0,
					Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		}

		// Row 18: gem block present but NO garbage attribute => should not
		// count as a garbage line, but still satisfies hasGemBlock.
		for (int x = 0; x < w; x++) {
			int color = (x == 0) ? Block.BLOCK_COLOR_GEM_RED : Block.BLOCK_COLOR_GRAY;
			engine.field.setBlock(x, h - 2, new Block(
					color, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));
		}

		assertEquals(1, callGetRemainGarbageLines(engine, 0));
	}

	@Test
	void getRemainGarbageLinesReturnsZeroWhenOnlyGemsWithoutGarbage() throws Exception {
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		int w = engine.field.getWidth();
		int h = engine.field.getHeight();

		// Fill bottom rows with gem blocks but NO garbage attribute.
		for (int y = h - 3; y < h; y++) {
			for (int x = 0; x < w; x++) {
				engine.field.setBlock(x, y, new Block(
						Block.BLOCK_COLOR_GEM_RED, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));
			}
		}

		// Gem blocks exist but no garbage attribute => 0.
		assertEquals(0, callGetRemainGarbageLines(engine, 0));
	}

	// ========================
	// getNowPlayerPlace (private, tested via reflection)
	// ========================

	@Test
	void getNowPlayerPlaceReturnsMinusOneForNonExistentPlayer() throws Exception {
		setPlayerExists(0, false);
		assertEquals(-1, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceReturnsMinusOneForDeadPlayer() throws Exception {
		setPlayerExists(0, true);
		setPlayerDead(0, true);
		assertEquals(-1, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceReturnsZeroForOnlyAlivePlayer() throws Exception {
		setPlayerExists(0, true);
		manager.engine[0].createFieldIfNeeded();
		assertEquals(0, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceReturnsZeroWhenLeadingByRemainLines() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();
		setPlayerRemainLines(0, 5);   // few remain => winning
		setPlayerRemainLines(1, 15);  // more remain => trailing
		assertEquals(0, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceReturnsOneWhenTrailingByRemainLines() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();
		setPlayerRemainLines(0, 15);  // more remain => trailing
		setPlayerRemainLines(1, 5);   // few remain => winning
		assertEquals(1, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceTiebreakByHighestBlockY() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		setPlayerRemainLines(0, 10);
		setPlayerRemainLines(1, 10);

		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();

		// Player 0: block at y=15 => highestBlockY = 15.
		manager.engine[0].field.setBlock(0, 15, new Block(Block.BLOCK_COLOR_GRAY));

		// Player 1: block at y=18 => highestBlockY = 18 (more cleared).
		manager.engine[1].field.setBlock(0, 18, new Block(Block.BLOCK_COLOR_GRAY));

		// Same remainLines, but player 0's highestY (15) < player 1's (18)
		// => player 1 is ahead.
		assertEquals(1, callGetNowPlayerPlace(manager.engine[0], 0));
		assertEquals(0, callGetNowPlayerPlace(manager.engine[1], 1));
	}

	@Test
	void getNowPlayerPlaceTiebreakAvoidsNullOpponentField() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		setPlayerRemainLines(0, 10);
		setPlayerRemainLines(1, 10);

		// Player 0 has a field, player 1 has null field.
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].field.setBlock(0, 15, new Block(Block.BLOCK_COLOR_GRAY));
		manager.engine[1].field = null;

		// Opponent with null field is skipped in tiebreak loop.
		assertEquals(0, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceSkipsDeadAndNonExistentPlayers() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		setPlayerDead(1, true);
		setPlayerExists(2, true);
		manager.engine[0].createFieldIfNeeded();
		manager.engine[2].createFieldIfNeeded();
		setPlayerRemainLines(0, 20);
		setPlayerRemainLines(2, 10);

		// Player 1 is dead, so only 0 and 2 compete.
		// Player 0 has more remain => behind player 2.
		assertEquals(1, callGetNowPlayerPlace(manager.engine[0], 0));
		assertEquals(0, callGetNowPlayerPlace(manager.engine[2], 2));
	}

	@Test
	void getNowPlayerPlaceMultiplePlayers() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		setPlayerExists(2, true);
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();
		manager.engine[2].createFieldIfNeeded();
		setPlayerRemainLines(0, 5);   // best
		setPlayerRemainLines(1, 10);  // middle
		setPlayerRemainLines(2, 15);  // worst

		assertEquals(0, callGetNowPlayerPlace(manager.engine[0], 0));
		assertEquals(1, callGetNowPlayerPlace(manager.engine[1], 1));
		assertEquals(2, callGetNowPlayerPlace(manager.engine[2], 2));
	}

	// ========================
	// updateMeter (private, tested via reflection)
	// ========================

	@Test
	void updateMeterNormalModeSetsGreenAbove14() throws Exception {
		GameEngine engine = manager.engine[0];
		setPlayerRemainLines(0, 15);
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	@Test
	void updateMeterNormalModeSetsYellowAt14() throws Exception {
		GameEngine engine = manager.engine[0];
		setPlayerRemainLines(0, 14);
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void updateMeterNormalModeSetsYellowBelow14() throws Exception {
		GameEngine engine = manager.engine[0];
		setPlayerRemainLines(0, 10);
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void updateMeterNormalModeSetsOrangeAt8() throws Exception {
		GameEngine engine = manager.engine[0];
		setPlayerRemainLines(0, 8);
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void updateMeterNormalModeSetsOrangeBelow8() throws Exception {
		GameEngine engine = manager.engine[0];
		setPlayerRemainLines(0, 6);
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void updateMeterNormalModeSetsRedAt4() throws Exception {
		GameEngine engine = manager.engine[0];
		setPlayerRemainLines(0, 4);
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void updateMeterNormalModeSetsRedAt0() throws Exception {
		GameEngine engine = manager.engine[0];
		setPlayerRemainLines(0, 0);
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void updateMeterNormalModeCalculatesMeterValue() throws Exception {
		GameEngine engine = manager.engine[0];
		setPlayerRemainLines(0, 10);
		callUpdateMeter(engine);
		// meterValue = remainLines * blockGraphicsHeight
		// blockGraphicsHeight for displaySize=0 => 16
		assertEquals(10 * 16, engine.meterValue);
	}

	@Test
	void updateMeterNormalModeMeterValueZeroWhenNoRemain() throws Exception {
		GameEngine engine = manager.engine[0];
		setPlayerRemainLines(0, 0);
		callUpdateMeter(engine);
		assertEquals(0, engine.meterValue);
	}

	@Test
	void updateMeterMapModeGreenWhenRemainAboveHalf() throws Exception {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.useMap = true;
		setReflectField(mode, "netCurrentRoomInfo", roomInfo);

		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();
		setPlayerStartGems(0, 100);

		// Place 60 gems (> half of 100).
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 6; y++) {
				engine.field.setBlock(x, 19 - y, new Block(Block.BLOCK_COLOR_GEM_RED));
			}
		}
		// 60 remaining > 100/2=50 => green.
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	@Test
	void updateMeterMapModeYellowAtHalf() throws Exception {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.useMap = true;
		setReflectField(mode, "netCurrentRoomInfo", roomInfo);

		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();
		setPlayerStartGems(0, 100);

		// Place exactly 50 gems (= half).
		for (int x = 0; x < 10; x++) {
			for (int y = 0; y < 5; y++) {
				engine.field.setBlock(x, 19 - y, new Block(Block.BLOCK_COLOR_GEM_RED));
			}
		}
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void updateMeterMapModeOrangeAtThird() throws Exception {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.useMap = true;
		setReflectField(mode, "netCurrentRoomInfo", roomInfo);

		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();
		setPlayerStartGems(0, 30);

		// Place 10 gems (= 1/3 of 30).
		for (int x = 0; x < 10; x++) {
			engine.field.setBlock(x, 19, new Block(Block.BLOCK_COLOR_GEM_RED));
		}
		// 10 <= 30/3=10 => orange.
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void updateMeterMapModeRedAtQuarter() throws Exception {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.useMap = true;
		setReflectField(mode, "netCurrentRoomInfo", roomInfo);

		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();
		setPlayerStartGems(0, 40);

		// Place 7 gems (= < 40/4=10).
		for (int x = 0; x < 7; x++) {
			engine.field.setBlock(x, 19, new Block(Block.BLOCK_COLOR_GEM_RED));
		}
		callUpdateMeter(engine);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void updateMeterMapModeDoesNothingWhenNoField() throws Exception {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.useMap = true;
		setReflectField(mode, "netCurrentRoomInfo", roomInfo);

		GameEngine engine = manager.engine[0];
		engine.field = null;
		setPlayerStartGems(0, 100);
		engine.meterColor = -999;
		engine.meterValue = -999;

		// Map branch checks (engine.field != null) and bails out.
		callUpdateMeter(engine);
		assertEquals(-999, engine.meterColor);
		assertEquals(-999, engine.meterValue);
	}

	@Test
	void updateMeterMapModeDoesNothingWhenNoStartGems() throws Exception {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.useMap = true;
		setReflectField(mode, "netCurrentRoomInfo", roomInfo);

		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();
		setPlayerStartGems(0, 0);
		engine.meterColor = -999;
		engine.meterValue = -999;

		// Map branch checks playerStartGems[playerID] > 0 and bails out.
		callUpdateMeter(engine);
		assertEquals(-999, engine.meterColor);
		assertEquals(-999, engine.meterValue);
	}

	// ========================
	// Reflection helpers
	// ========================

	private void setPlayerExists(int playerID, boolean exists) throws Exception {
		boolean[] arr = (boolean[]) getReflectField(mode, "netvsPlayerExist");
		arr[playerID] = exists;
	}

	private void setPlayerDead(int playerID, boolean dead) throws Exception {
		boolean[] arr = (boolean[]) getReflectField(mode, "netvsPlayerDead");
		arr[playerID] = dead;
	}

	private void setPlayerRemainLines(int playerID, int value) throws Exception {
		int[] arr = (int[]) getReflectField(mode, "playerRemainLines");
		arr[playerID] = value;
	}

	private void setPlayerStartGems(int playerID, int value) throws Exception {
		int[] arr = (int[]) getReflectField(mode, "playerStartGems");
		arr[playerID] = value;
	}

	private static void setReflectField(Object instance, String name, Object value)
			throws Exception {
		java.lang.reflect.Field f = resolveField(instance.getClass(), name);
		f.setAccessible(true);
		f.set(instance, value);
	}

	private static Object getReflectField(Object instance, String name) throws Exception {
		java.lang.reflect.Field f = resolveField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
	}

	private int callGetRemainGarbageLines(GameEngine engine, int playerID) throws Exception {
		Method m = NetVSDigRaceMode.class.getDeclaredMethod(
				"getRemainGarbageLines", GameEngine.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, engine, playerID);
	}

	private int callGetNowPlayerPlace(GameEngine engine, int playerID) throws Exception {
		Method m = NetVSDigRaceMode.class.getDeclaredMethod(
				"getNowPlayerPlace", GameEngine.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, engine, playerID);
	}

	private void callUpdateMeter(GameEngine engine) throws Exception {
		Method m = NetVSDigRaceMode.class.getDeclaredMethod("updateMeter", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	/**
	 * Walk the class hierarchy to find a declared field, supporting fields
	 * defined in parent classes (e.g. netvsPlayerExist on NetDummyVSMode,
	 * netCurrentRoomInfo on NetDummyMode).
	 */
	private static java.lang.reflect.Field resolveField(Class<?> clazz, String name)
			throws NoSuchFieldException {
		Class<?> c = clazz;
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
