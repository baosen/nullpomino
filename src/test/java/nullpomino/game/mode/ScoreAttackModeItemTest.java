package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

class ScoreAttackModeItemTest {

	@Test
	void levelThresholdsTagTheNextQueuedPiece() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nextPieceArrayObject = new Piece[] {
			new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_I), new Piece(Piece.PIECE_O)
		};
		mode.startGame(engine, 0);

		engine.statistics.level = 100;
		invokeLevelUp(mode, engine);
		assertPieceItem(engine.nextPieceArrayObject[0], Block.BLOCK_ITEM_NONE);
		assertPieceItem(engine.nextPieceArrayObject[1], Block.BLOCK_ITEM_FREE_FALL);
		assertEquals(200, readInt(mode, "nextItemLevel"));

		engine.nextPieceCount = 1;
		engine.statistics.level = 200;
		invokeLevelUp(mode, engine);
		assertPieceItem(engine.nextPieceArrayObject[2], Block.BLOCK_ITEM_DEL_EVEN);
		assertEquals(-1, readInt(mode, "nextItemLevel"));
	}

	@Test
	void startLevelBoundariesAwardTheirSectionItem() throws Exception {
		// Items disabled: nothing is awarded regardless of level.
		ScoreAttackMode disabledMode = new ScoreAttackMode();
		GameEngine disabledEngine = freshEngine(disabledMode);
		disabledMode.playerInit(disabledEngine, 0);
		setBoolean(disabledMode, "enableitem", false);
		disabledEngine.nextPieceArrayObject = new Piece[] {new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_I)};
		disabledMode.startGame(disabledEngine, 0);
		disabledEngine.statistics.level = 100;
		invokeLevelUp(disabledMode, disabledEngine);
		assertPieceItem(disabledEngine.nextPieceArrayObject[1], Block.BLOCK_ITEM_NONE);

		// Start at level 100: startGame schedules the level-100 FREE_FALL immediately
		// (the boundary levelUp that natural play relies on never fires), then DEL_EVEN at 200.
		ScoreAttackMode startAtOneHundred = new ScoreAttackMode();
		GameEngine startAtOneHundredEngine = freshEngine(startAtOneHundred);
		startAtOneHundred.playerInit(startAtOneHundredEngine, 0);
		setInt(startAtOneHundred, "startlevel", 1);
		startAtOneHundredEngine.nextPieceArrayObject = new Piece[] {
			new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_I), new Piece(Piece.PIECE_O)
		};
		startAtOneHundred.startGame(startAtOneHundredEngine, 0);
		assertPieceItem(startAtOneHundredEngine.nextPieceArrayObject[1], Block.BLOCK_ITEM_FREE_FALL);
		assertEquals(200, readInt(startAtOneHundred, "nextItemLevel"));
		startAtOneHundredEngine.nextPieceCount = 1;
		startAtOneHundredEngine.statistics.level = 200;
		invokeLevelUp(startAtOneHundred, startAtOneHundredEngine);
		assertPieceItem(startAtOneHundredEngine.nextPieceArrayObject[2], Block.BLOCK_ITEM_DEL_EVEN);
		assertEquals(-1, readInt(startAtOneHundred, "nextItemLevel"));

		// Start at level 200: startGame schedules the level-200 DEL_EVEN immediately.
		ScoreAttackMode startAtTwoHundred = new ScoreAttackMode();
		GameEngine startAtTwoHundredEngine = freshEngine(startAtTwoHundred);
		startAtTwoHundred.playerInit(startAtTwoHundredEngine, 0);
		setInt(startAtTwoHundred, "startlevel", 2);
		startAtTwoHundredEngine.nextPieceArrayObject = new Piece[] {new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_I)};
		startAtTwoHundred.startGame(startAtTwoHundredEngine, 0);
		assertPieceItem(startAtTwoHundredEngine.nextPieceArrayObject[1], Block.BLOCK_ITEM_DEL_EVEN);
		assertEquals(-1, readInt(startAtTwoHundred, "nextItemLevel"));
	}

	@Test
	void legacyReplayVersionStillSkipsBoundaryItems() throws Exception {
		// A pre-fix replay (version below ITEM_START_LEVEL_FIX_VERSION) with items
		// enabled must reproduce the old behavior: starting on a boundary skips it.
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1);
		setInt(mode, "startlevel", 2);
		engine.nextPieceArrayObject = new Piece[] {new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_I)};
		mode.startGame(engine, 0);
		assertEquals(-1, readInt(mode, "nextItemLevel"));
		invokeLevelUp(mode, engine);
		assertPieceItem(engine.nextPieceArrayObject[1], Block.BLOCK_ITEM_NONE);
	}

	@Test
	void version3ReplaySkipsBoundaryItems() throws Exception {
		// Version 3 is the last version before ITEM_START_LEVEL_FIX_VERSION (4):
		// the accuracy fix is already in effect, but starting on a boundary must
		// still skip that section's item. Guards the exact boundary threshold so
		// dropping the constant to 3 would fail here (the v1 test wouldn't catch it).
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 3);
		setInt(mode, "startlevel", 2);
		engine.nextPieceArrayObject = new Piece[] {new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_I)};
		mode.startGame(engine, 0);
		assertEquals(-1, readInt(mode, "nextItemLevel"));
		invokeLevelUp(mode, engine);
		assertPieceItem(engine.nextPieceArrayObject[1], Block.BLOCK_ITEM_NONE);
	}

	@Test
	void clearedItemUsesHighestThenRightmostPriorityAndCancelsAllTags() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		fillLine(engine, 2, Block.BLOCK_COLOR_RED);
		fillLine(engine, 4, Block.BLOCK_COLOR_BLUE);
		engine.field.getBlock(2, 2).item = Block.BLOCK_ITEM_FREE_FALL;
		engine.field.getBlock(8, 2).item = Block.BLOCK_ITEM_DEL_EVEN;
		engine.field.getBlock(4, 4).item = Block.BLOCK_ITEM_FREE_FALL;
		engine.nextPieceArrayObject = new Piece[] {new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_I)};
		setPieceItem(engine.nextPieceArrayObject[0], Block.BLOCK_ITEM_FREE_FALL);
		setPieceItem(engine.nextPieceArrayObject[1], Block.BLOCK_ITEM_DEL_EVEN);
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.holdPieceObject = new Piece(Piece.PIECE_S);
		setPieceItem(engine.nowPieceObject, Block.BLOCK_ITEM_FREE_FALL);
		setPieceItem(engine.holdPieceObject, Block.BLOCK_ITEM_DEL_EVEN);
		engine.lineClearing = 2;

		mode.onLineClear(engine, 0);

		assertEquals(Block.BLOCK_ITEM_DEL_EVEN, readInt(mode, "pendingItemEffect"));
		assertFieldHasNoItems(engine);
		assertPieceItem(engine.nextPieceArrayObject[0], Block.BLOCK_ITEM_NONE);
		assertPieceItem(engine.nextPieceArrayObject[1], Block.BLOCK_ITEM_NONE);
		assertPieceItem(engine.nowPieceObject, Block.BLOCK_ITEM_NONE);
		assertPieceItem(engine.holdPieceObject, Block.BLOCK_ITEM_NONE);
	}

	@Test
	void freeFallActivatesOnAreWithoutChangingScoringStatistics() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		fillLine(engine, 0, Block.BLOCK_COLOR_RED);
		engine.field.getBlock(5, 0).item = Block.BLOCK_ITEM_FREE_FALL;
		engine.field.setBlockColor(1, 5, Block.BLOCK_COLOR_CYAN);
		engine.lineClearing = 1;
		engine.statistics.score = 12345;
		engine.statistics.lines = 67;
		engine.statistics.level = 42;

		mode.onLineClear(engine, 0);
		clearMarkedLine(engine);
		mode.onARE(engine, 0);

		assertEquals(Block.BLOCK_COLOR_CYAN, engine.field.getBlockColor(1, engine.field.getHeight() - 1));
		assertEquals(12345, engine.statistics.score);
		assertEquals(67, engine.statistics.lines);
		assertEquals(42, engine.statistics.level);
	}

	@Test
	void freeFallImmediatelyRemovesCompletedRowsWithoutScoring() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		prepareFreeFallCompletedLine(engine);
		setInt(mode, "pendingItemEffect", Block.BLOCK_ITEM_FREE_FALL);
		engine.statistics.score = 12345;
		engine.statistics.lines = 67;
		engine.statistics.level = 42;

		mode.onARE(engine, 0);

		assertEquals(0, engine.field.checkLineNoFlag());
		assertTrue(engine.field.isEmpty());
		assertEquals(12345, engine.statistics.score);
		assertEquals(67, engine.statistics.lines);
		assertEquals(42, engine.statistics.level);
	}

	@Test
	void version4ReplayKeepsFreeFallCompletedRowsUntilNextLock() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 4);
		engine.createFieldIfNeeded();
		prepareFreeFallCompletedLine(engine);
		setInt(mode, "pendingItemEffect", Block.BLOCK_ITEM_FREE_FALL);

		mode.onARE(engine, 0);

		assertEquals(1, engine.field.checkLineNoFlag());
		assertFalse(engine.field.getLineFlag(engine.field.getHeightWithoutHurryupFloor() - 1));
	}

	@Test
	void delEvenRemovesVisibleEvenRowsAndLeavesStatisticsUntouched() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		fillLine(engine, 0, Block.BLOCK_COLOR_RED);
		engine.field.getBlock(5, 0).item = Block.BLOCK_ITEM_DEL_EVEN;
		engine.field.setBlockColor(0, 1, Block.BLOCK_COLOR_RED);
		engine.field.setBlockColor(0, 2, Block.BLOCK_COLOR_GREEN);
		engine.field.setBlockColor(0, 3, Block.BLOCK_COLOR_BLUE);
		engine.lineClearing = 1;
		engine.statistics.score = 54321;
		engine.statistics.lines = 12;
		engine.statistics.level = 77;

		mode.onLineClear(engine, 0);
		clearMarkedLine(engine);
		mode.onARE(engine, 0);

		assertEquals(Block.BLOCK_COLOR_RED, engine.field.getBlockColor(0, 10));
		assertEquals(Block.BLOCK_COLOR_BLUE, engine.field.getBlockColor(0, 11));
		assertFalse(fieldContainsColor(engine, Block.BLOCK_COLOR_GREEN));
		assertEquals(54321, engine.statistics.score);
		assertEquals(12, engine.statistics.lines);
		assertEquals(77, engine.statistics.level);
	}

	@Test
	void itemSettingDefaultsPersistsAndLegacyReplaysDisableItems() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		assertTrue(readBoolean(mode, "enableitem"));
		assertEquals(5, readInt(mode, "version"));
		assertTrue(engine.rainbowAnimate);

		setBoolean(mode, "enableitem", false);
		CustomProperties saved = new CustomProperties();
		invokeSaveSetting(mode, saved);
		assertFalse(saved.getProperty("scoreattack.enableitem", true));

		ScoreAttackMode replayMode = new ScoreAttackMode();
		GameManager replayManager = new GameManager(new EventReceiver());
		replayManager.replayMode = true;
		replayManager.replayProp = new CustomProperties();
		replayManager.mode = replayMode;
		replayManager.init();
		replayManager.engine[0].init();
		replayMode.playerInit(replayManager.engine[0], 0);
		assertEquals(0, readInt(replayMode, "version"));
		assertFalse(readBoolean(replayMode, "enableitem"));
		assertFalse(replayManager.engine[0].rainbowAnimate);
	}

	@Test
	void itemMenuToggleReloadsItsRecordSetAndMarksTheLeaderboard() throws Exception {
		RecordingReceiver receiver = new RecordingReceiver();
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode, receiver);
		String ruleName = engine.ruleopt.strRuleName;
		engine.owner.modeConfig.setProperty("scoreattack.ranking.item." + ruleName + ".score.0", 900);
		engine.owner.modeConfig.setProperty("scoreattack.ranking." + ruleName + ".score.0", 100);
		mode.playerInit(engine, 0);
		assertEquals(900, ((int[]) readField(mode, "rankingScore"))[0]);

		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
		assertTrue(receiver.text.contains("SCORE+ITEM TIME"));

		setInt(mode, "menuCursor", 5);
		setInt(mode, "menuTime", 5);
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		mode.onSetting(engine, 0);

		assertFalse(readBoolean(mode, "enableitem"));
		assertFalse(engine.rainbowAnimate);
		assertEquals(100, ((int[]) readField(mode, "rankingScore"))[0]);
		receiver.text.clear();
		mode.renderLast(engine, 0);
		assertTrue(receiver.text.contains("SCORE  TIME"));
	}

	private static GameEngine freshEngine(ScoreAttackMode mode) {
		return freshEngine(mode, new EventReceiver());
	}

	private static GameEngine freshEngine(ScoreAttackMode mode, EventReceiver receiver) {
		GameManager manager = new GameManager(receiver);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void fillLine(GameEngine engine, int y, int color) {
		for(int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlockColor(x, y, color);
		}
	}

	private static void clearMarkedLine(GameEngine engine) {
		assertEquals(1, engine.field.checkLine());
		engine.field.clearLine();
		engine.field.downFloatingBlocks();
	}

	private static void prepareFreeFallCompletedLine(GameEngine engine) {
		int bottom = engine.field.getHeightWithoutHurryupFloor() - 1;
		for(int x = 1; x < engine.field.getWidth(); x++) {
			engine.field.setBlockColor(x, bottom, Block.BLOCK_COLOR_RED);
		}
		engine.field.setBlockColor(0, bottom - 1, Block.BLOCK_COLOR_BLUE);
	}

	private static boolean fieldContainsColor(GameEngine engine, int color) {
		for(int y = 0; y < engine.field.getHeight(); y++) {
			for(int x = 0; x < engine.field.getWidth(); x++) {
				if(engine.field.getBlockColor(x, y) == color) return true;
			}
		}
		return false;
	}

	private static void assertFieldHasNoItems(GameEngine engine) {
		for(int y = -engine.field.getHiddenHeight(); y < engine.field.getHeight(); y++) {
			for(int x = 0; x < engine.field.getWidth(); x++) {
				assertEquals(Block.BLOCK_ITEM_NONE, engine.field.getBlock(x, y).item);
			}
		}
	}

	private static void setPieceItem(Piece piece, int item) {
		for(Block block : piece.block) block.item = item;
	}

	private static void assertPieceItem(Piece piece, int item) {
		for(Block block : piece.block) assertEquals(item, block.item);
	}

	private static void invokeLevelUp(ScoreAttackMode mode, GameEngine engine) throws Exception {
		Method method = ScoreAttackMode.class.getDeclaredMethod("levelUp", GameEngine.class);
		method.setAccessible(true);
		method.invoke(mode, engine);
	}

	private static void invokeSaveSetting(ScoreAttackMode mode, CustomProperties prop) throws Exception {
		Method method = ScoreAttackMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		method.setAccessible(true);
		method.invoke(mode, prop);
	}

	private static int readInt(ScoreAttackMode mode, String name) throws Exception {
		return findField(name).getInt(mode);
	}

	private static boolean readBoolean(ScoreAttackMode mode, String name) throws Exception {
		return findField(name).getBoolean(mode);
	}

	private static Object readField(ScoreAttackMode mode, String name) throws Exception {
		return findField(name).get(mode);
	}

	private static void setInt(ScoreAttackMode mode, String name, int value) throws Exception {
		findField(name).setInt(mode, value);
	}

	private static void setBoolean(ScoreAttackMode mode, String name, boolean value) throws Exception {
		findField(name).setBoolean(mode, value);
	}

	private static Field findField(String name) throws Exception {
		Class<?> type = ScoreAttackMode.class;
		while(type != null) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch(NoSuchFieldException e) {
				type = type.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static final class RecordingReceiver extends EventReceiver {
		private final List<String> text = new ArrayList<String>();

		@Override
		public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
			text.add(str);
		}
	}
}
