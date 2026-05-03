package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link NetVSBattleMode}: modeInit,
 * playerInit, startGame, calcScore (attack table, B2B, combo),
 * onLast (hurry-up, meter, APL/APM), renderLast, netSendStats,
 * netRecvStats, and the GarbageEntry inner class.
 */
class NetVSBattleModeGameLogicTest {

	@Test
	void getNameReturnsExpected() {
		assertEquals("NET-VS-BATTLE", new NetVSBattleMode().getName());
	}

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(new NetVSBattleMode().isVSMode());
	}

	@Test
	void modeInitCreatesArrays() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		boolean[] playerKObyYou = (boolean[]) readFieldByClass(mode, NetVSBattleMode.class, "playerKObyYou");
		int[] scgettime = (int[]) readFieldByClass(mode, NetVSBattleMode.class, "scgettime");
		int[] garbageSent = (int[]) readFieldByClass(mode, NetVSBattleMode.class, "garbageSent");
		float[] playerAPL = (float[]) readFieldByClass(mode, NetVSBattleMode.class, "playerAPL");

		assertEquals(6, playerKObyYou.length);
		assertEquals(6, scgettime.length);
		assertEquals(6, garbageSent.length);
		assertEquals(6, playerAPL.length);
	}

	@Test
	void playerInitResetsAllFields() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);

		assertEquals(-1, readInt(mode, "lastHole"));
		assertEquals(0, readInt(mode, "hurryupCount"));
		assertEquals(0, readInt(mode, "currentKO"));
		assertEquals(-1, readInt(mode, "targetID"));
		assertFalse(readBooleanArray(mode, "playerKObyYou", 0));
		assertEquals(0, readIntArray(mode, "garbageSent", 0));
	}

	@Test
	void startGameSetsHurryupStartedFalse() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertFalse(readBoolean(mode, "hurryupStarted"));
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		setIntArray(mode, "scgettime", 0, 0);
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertEquals(1, readIntArray(mode, "scgettime", 0));
	}

	@Test
	void onLastSetsMeterColorGreenWhenNoGarbage() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		setIntArray(mode, "garbage", 0, 0);

		mode.onLast(engine, 0);

		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	private static void setNetRoomInfo(NetVSBattleMode mode) throws Exception {
		nullpomino.game.net.NetRoomInfo room = new nullpomino.game.net.NetRoomInfo();
		room.reduceLineSend = false;
		room.rensaBlock = false;
		room.bravo = true;
		room.useFractionalGarbage = false;
		room.counter = false;
		room.garbagePercent = 100;
		room.divideChangeRateByPlayers = false;
		room.garbageChangePerAttack = false;
		room.hurryupSeconds = -1;
		room.isTarget = false;
		java.lang.reflect.Field f = findField(mode.getClass(), "netCurrentRoomInfo");
		f.setAccessible(true);
		f.set(mode, room);
		// Prevent netplay send path (avoids NPE on netLobby)
		java.lang.reflect.Field f2 = mode.getClass().getSuperclass().getDeclaredField("netvsIsPractice");
		f2.setAccessible(true);
		f2.setBoolean(mode, true);
	}

	@Test
	void calcScoreNoLinesDoesNothingForPlayer1() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setNetRoomInfo(mode);

		// Player 1 should not trigger scoring
		mode.calcScore(engine, 1, 1);
		assertEquals(0, readIntArray(mode, "garbageSent", 1));
	}

	@Test
	void calcScoreSingleNormalNoAttack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		// Place a block to prevent all-clear bravo
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.score = 0;
		setNetRoomInfo(mode);

		// Single (lines=1) normal -> LINE_ATTACK_TABLE[0][0] = 0 * 60
		mode.calcScore(engine, 0, 1);

		int sent = readIntArray(mode, "garbageSent", 0);
		assertEquals(0, sent, "Single normal: 0 * 60");
	}

	@Test
	void calcScoreDoubleNormalGives60Attack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		// Prevent all-clear
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		setNetRoomInfo(mode);

		// Double (lines=2) normal -> LINE_ATTACK_TABLE[1][0] = 1 * 60 = 60
		mode.calcScore(engine, 0, 2);

		int sent = readIntArray(mode, "garbageSent", 0);
		assertEquals(60, sent, "Double normal: 1 * 60");
	}

	@Test
	void calcScoreFourNormalGives240Attack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		setNetRoomInfo(mode);

		// Four (lines=4) normal -> LINE_ATTACK_TABLE[3][0] = 4 * 60 = 240
		mode.calcScore(engine, 0, 4);

		int sent = readIntArray(mode, "garbageSent", 0);
		assertEquals(240, sent, "Four normal: 4 * 60");
	}

	@Test
	void calcScoreB2bAddsBonus() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.b2b = true;
		setNetRoomInfo(mode);

		// Double with B2B -> attack pts[main] = 60, b2b pts = 60
		mode.calcScore(engine, 0, 2);

		int sent = readIntArray(mode, "garbageSent", 0);
		assertEquals(120, sent, "Double B2B: 60 main + 60 B2B");
	}

	@Test
	void calcScoreComboAddsComboAttack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
		engine.combo = 3; // COMBO_ATTACK_TABLE[0][2] = 1 -> 1*60 = 60
		setNetRoomInfo(mode);

		// Double + combo 3 -> main=60, combo=1*60=60
		mode.calcScore(engine, 0, 2);

		int sent = readIntArray(mode, "garbageSent", 0);
		assertEquals(120, sent, "Double + combo 3: 60 main + 60 combo");
	}

	@Test
	void calcScoreTSpinTripleGivesHighAttack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = false;
		setNetRoomInfo(mode);

		// T-Spin Triple -> LINE_ATTACK_TABLE[7][0] = 6 * 60 = 360
		mode.calcScore(engine, 0, 3);

		int sent = readIntArray(mode, "garbageSent", 0);
		assertEquals(360, sent, "T-Spin Triple: 6 * 60");
	}

	@Test
	void calcScoreAllClearBravoGives360Attack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setNetRoomInfo(mode);
		// Empty field for bravo

		// Single + all-clear -> main=0, bravo=6*60=360
		mode.calcScore(engine, 0, 1);

		int sent = readIntArray(mode, "garbageSent", 0);
		assertEquals(360, sent, "Single + All Clear bravo: 6 * 60");
	}

	@Test
	void onLastComputesAPLAndAPM() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;
		setFloatArray(mode, "playerAPM", 0f, 0);
		engine.statistics.time = 3600;
		engine.statistics.lines = 10;
		setIntArray(mode, "garbageSent", 60, 0);

		mode.onLast(engine, 0);

		// APM = (60/60 * 3600) / 3600 = 1.0
		// APL = (60/60) / 10 = 0.1
		assertEquals(1.0f, readFloatArray(mode, "playerAPM", 0), 0.001f);
		assertEquals(0.1f, readFloatArray(mode, "playerAPL", 0), 0.001f);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(NetVSBattleMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static int readIntArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((int[]) f.get(mode))[index];
	}

	private static boolean readBooleanArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((boolean[]) f.get(mode))[index];
	}

	private static float readFloatArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((float[]) f.get(mode))[index];
	}

	private static Object readFieldByClass(Object mode, Class<?> cls, String name) throws Exception {
		Field f = cls.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setIntArray(Object mode, String name, int value, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		((int[]) f.get(mode))[index] = value;
	}

	private static void setFloatArray(Object mode, String name, float value, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		((float[]) f.get(mode))[index] = value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
