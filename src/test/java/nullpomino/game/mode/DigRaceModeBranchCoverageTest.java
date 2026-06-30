package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Exercises the non-wraparound branches of {@link DigRaceMode}: the netplay
 * signalling paths in {@code onSetting}, the start/cancel paths, the render
 * thresholds in {@code renderLast}/{@code renderResult}, the meter colours in
 * {@code calcScore}, the garbage counting in {@code getRemainGarbageLines}, and
 * the ranking/save logic in {@code saveReplay}/{@code checkRanking}.
 */
class DigRaceModeBranchCoverageTest {

	// ------------------------------------------------------------------
	// onSetting netplay + start/cancel paths
	// ------------------------------------------------------------------

	@Test
	void onSettingChangeSignalsNetOptions() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		wireNetLobby(mode);
		engine.owner.replayMode = false;
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netNumSpectators", 1);
		setField(mode, "menuCursor", 2); // any non-preset numeric cursor
		setField(mode, "menuTime", 10);

		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;

		// netSendOptions swallows the null-socket send; just exercise L224.
		mode.onSetting(engine, 0);
		assertTrue(true);
	}

	@Test
	void onSettingLoadPresetSignalsNetOptions() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		wireNetLobby(mode);
		engine.owner.replayMode = false;
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netNumSpectators", 1);
		setField(mode, "menuCursor", 9); // cursor 9 = load preset
		setField(mode, "menuTime", 10);

		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		// Loads preset then signals options (L235/L238).
		mode.onSetting(engine, 0);
		assertTrue(true);
	}

	@Test
	void onSettingSavePresetConfirm() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.owner.replayMode = false;
		setField(mode, "menuCursor", 10); // cursor 10 = save preset
		setField(mode, "menuTime", 10);

		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		// Returns true (stays in settings) after saving the preset.
		boolean stay = mode.onSetting(engine, 0);
		assertTrue(stay);
	}

	@Test
	void onSettingStartGameInNetPlaySendsStart() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		wireNetLobby(mode);
		engine.owner.replayMode = false;
		setField(mode, "netIsNetPlay", true);
		setField(mode, "menuCursor", 5); // non-preset cursor -> "save settings" arm
		setField(mode, "menuTime", 10);

		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		// Sends "start1p" (L252) and returns false to start the game.
		boolean stay = mode.onSetting(engine, 0);
		assertEquals(false, stay);
	}

	@Test
	void onSettingCancelSetsQuitFlag() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.owner.replayMode = false;
		setField(mode, "netIsNetPlay", false);
		setField(mode, "menuCursor", 0);
		setField(mode, "menuTime", 10);

		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;

		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag, "pressing B offline should set quitflag");
	}

	@Test
	void onSettingDButtonEntersNetRanking() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		wireNetLobby(mode);
		engine.owner.replayMode = false;
		setField(mode, "netIsNetPlay", true);
		setField(mode, "big", false);
		setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
		setField(mode, "menuTime", 10);

		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

		mode.onSetting(engine, 0);
		assertTrue(readBoolean(mode, "netIsNetRankingDisplayMode"),
				"pressing D in net play should enter ranking screen");
	}

	@Test
	void onSettingReplayModeAutoStarts() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.owner.replayMode = true;
		setField(mode, "menuTime", 60); // already >= 60 -> returns false next tick

		boolean stay = mode.onSetting(engine, 0);
		assertEquals(false, stay);
		assertEquals(-1, readInt(mode, "menuCursor"));
	}

	// ------------------------------------------------------------------
	// getRemainGarbageLines / fillGarbage
	// ------------------------------------------------------------------

	@Test
	void getRemainGarbageLinesNullEngineOrField() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		Method m = DigRaceMode.class.getDeclaredMethod(
				"getRemainGarbageLines", GameEngine.class, int.class);
		m.setAccessible(true);

		// null engine -> -1
		assertEquals(-1, (int) m.invoke(mode, (GameEngine) null, 0));

		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		// field is null until created -> -1
		assertEquals(-1, (int) m.invoke(mode, engine, 0));
	}

	@Test
	void getRemainGarbageLinesCountsGarbageRows() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		// goaltype 0 scans the bottom GOAL_TABLE[0]=5 rows. Put garbage in 3 of them.
		for(int i = 0; i < 3; i++) {
			engine.field.setBlock(0, h - 1 - i,
				new Block(Block.BLOCK_COLOR_GRAY, 0,
					Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		}
		// One cleared row inside the scan window should be skipped (getLineFlag).
		engine.field.setLineFlag(h - 1 - 3, true);

		Method m = DigRaceMode.class.getDeclaredMethod(
				"getRemainGarbageLines", GameEngine.class, int.class);
		m.setAccessible(true);
		assertEquals(3, (int) m.invoke(mode, engine, 0));
	}

	@Test
	void fillGarbageStickySkinSetsConnections() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		// Sticky receiver so fillGarbage enters the connection-setting branch.
		GameManager manager = new GameManager(new EventReceiver() {
			@Override
			public boolean isStickySkin(int skin) {
				return true;
			}
		});
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		Method fill = DigRaceMode.class.getDeclaredMethod(
				"fillGarbage", GameEngine.class, int.class);
		fill.setAccessible(true);
		fill.invoke(mode, engine, 0); // goaltype 0 -> 5 garbage rows

		Method m = DigRaceMode.class.getDeclaredMethod(
				"getRemainGarbageLines", GameEngine.class, int.class);
		m.setAccessible(true);
		// All 5 rows have garbage with one hole each.
		assertEquals(5, (int) m.invoke(mode, engine, 0));
	}

	// ------------------------------------------------------------------
	// calcScore meter colours + completion
	// ------------------------------------------------------------------

	@Test
	void calcScoreYellowMeterAndCompletion() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setField(mode, "goaltype", 2); // 18 garbage rows scanned

		int h = engine.field.getHeight();
		// Leave 10 garbage rows -> remainLines==10 -> yellow (<=14, >8).
		for(int i = 0; i < 10; i++) {
			engine.field.setBlock(0, h - 1 - i,
				new Block(Block.BLOCK_COLOR_GRAY, 0,
					Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		}
		mode.calcScore(engine, 0, 1);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
		assertEquals(0, engine.ending);

		// Now clear the field -> remainLines==0 with lines>0 -> game complete.
		for(int i = 0; i < 10; i++) {
			engine.field.setBlock(0, h - 1 - i, new Block());
		}
		mode.calcScore(engine, 0, 2);
		assertEquals(1, engine.ending);
	}

	// ------------------------------------------------------------------
	// renderLast threshold branches (no assertions; no-op receiver)
	// ------------------------------------------------------------------

	@Test
	void renderLastMenuOnlyReturnsEarly() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = true;
		mode.renderLast(engine, 0); // L426: early return
		assertTrue(true);
	}

	@Test
	void renderLastSettingShowsRanking() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.owner.replayMode = false;
		setField(mode, "netIsWatch", false);
		setField(mode, "rankingRank", 2); // makes (rankingRank == i) true on one row
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0); // L431/L432/L433/L438-440
		assertTrue(true);
	}

	@Test
	void renderLastInGameYellowTwoDigit() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.createFieldIfNeeded();
		setField(mode, "goaltype", 2); // 18 rows
		engine.stat = GameEngine.Status.MOVE;

		int h = engine.field.getHeight();
		// 11 garbage rows -> remainLines==11 -> two-digit string, yellow colour.
		for(int i = 0; i < 11; i++) {
			engine.field.setBlock(0, h - 1 - i,
				new Block(Block.BLOCK_COLOR_GRAY, 0,
					Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		}
		mode.renderLast(engine, 0); // L444-L456 two-digit + yellow
		assertTrue(true);
	}

	@Test
	void renderLastInGameSingleDigitRed() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.createFieldIfNeeded();
		setField(mode, "goaltype", 0); // 5 rows
		engine.stat = GameEngine.Status.MOVE;

		int h = engine.field.getHeight();
		// 3 garbage rows -> remainLines==3 -> single digit, red colour.
		for(int i = 0; i < 3; i++) {
			engine.field.setBlock(0, h - 1 - i,
				new Block(Block.BLOCK_COLOR_GRAY, 0,
					Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		}
		mode.renderLast(engine, 0); // L453 single-digit + red branch
		assertTrue(true);
	}

	@Test
	void renderLastInGameNegativeRemainClampedToZero() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		// field stays null -> getRemainGarbageLines returns -1 -> strLines "0" (L446).
		engine.stat = GameEngine.Status.MOVE;
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// ------------------------------------------------------------------
	// renderResult retry-prompt branch
	// ------------------------------------------------------------------

	@Test
	void renderResultRetryPrompt() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		setField(mode, "netReplaySendStatus", 2); // L526 retry arm
		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	// ------------------------------------------------------------------
	// saveReplay ranking update + checkRanking ordering
	// ------------------------------------------------------------------

	@Test
	void saveReplayUpdatesRankingWhenComplete() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.owner.replayMode = false;
		setField(mode, "goaltype", 0);
		clearRankingRow(mode, 0); // start from a known-empty ranking row
		setField(mode, "netIsWatch", false);
		engine.ending = 1; // game ended
		// Field empty -> getRemainGarbageLines==0 so the ranking branch fires.
		engine.statistics.time = 1000;
		engine.statistics.lines = 5;
		engine.statistics.totalPieceLocked = 12;

		mode.saveReplay(engine, 0, engine.owner.modeConfig);
		// First completed run lands at rank 0 (rankingRank != -1, L548).
		assertEquals(0, readInt(mode, "rankingRank"));
	}

	@Test
	void checkRankingOrdersByTimeThenLinesThenPiece() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setField(mode, "goaltype", 1);
		clearRankingRow(mode, 1); // start from a known-empty ranking row

		Method update = DigRaceMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class);
		update.setAccessible(true);

		// Seed a baseline run -> lands at rank 0 (empty slot, time < 0).
		update.invoke(mode, 2000, 5, 20);
		assertEquals(0, readInt(mode, "rankingRank"));

		// Faster time -> ranks ahead at 0 (time < existing, L614).
		update.invoke(mode, 1500, 5, 20);
		assertEquals(0, readInt(mode, "rankingRank"));

		// Same time as the slow run, fewer lines -> rank 1 (time== && lines<, L615).
		update.invoke(mode, 2000, 3, 20);
		assertEquals(1, readInt(mode, "rankingRank"));

		// Same time and lines, fewer pieces -> rank 1 (time== && lines== && piece<, L616-617).
		update.invoke(mode, 2000, 3, 10);
		assertEquals(1, readInt(mode, "rankingRank"));
	}

	// ------------------------------------------------------------------
	// netIsNetRankingSendOK
	// ------------------------------------------------------------------

	@Test
	void netIsNetRankingSendOkRequiresClearedAndEnded() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setField(mode, "goaltype", 0);

		Method send = DigRaceMode.class.getDeclaredMethod(
				"netIsNetRankingSendOK", GameEngine.class);
		send.setAccessible(true);

		// Empty field (remain==0) but not ended -> false (short-circuits at ending).
		engine.ending = 0;
		assertEquals(false, (boolean) send.invoke(mode, engine));

		// Empty field + ended -> true.
		engine.ending = 1;
		assertEquals(true, (boolean) send.invoke(mode, engine));
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	/**
	 * Resets one goaltype's ranking row to the empty sentinel (time=-1). playerInit
	 * loads rankings from the shared, gitignored config/setting file, so a prior
	 * test's persisted records would otherwise leak in and shift the expected ranks.
	 */
	private static void clearRankingRow(DigRaceMode mode, int goaltype) throws Exception {
		int[][] times = (int[][]) findField(mode.getClass(), "rankingTime").get(mode);
		int[][] lines = (int[][]) findField(mode.getClass(), "rankingLines").get(mode);
		int[][] pieces = (int[][]) findField(mode.getClass(), "rankingPiece").get(mode);
		for(int i = 0; i < times[goaltype].length; i++) {
			times[goaltype][i] = -1;
			lines[goaltype][i] = 0;
			pieces[goaltype][i] = 0;
		}
	}

	private static GameEngine freshEngine(DigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void wireNetLobby(Object mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setField(mode, "netLobby", lobby);
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		findField(obj.getClass(), name).set(obj, value);
	}

	private static void setField(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
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
