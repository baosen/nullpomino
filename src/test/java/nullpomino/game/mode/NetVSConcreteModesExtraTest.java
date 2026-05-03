package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Extra coverage for concrete net VS modes.
 */
class NetVSConcreteModesExtraTest {

	// ================================================================
	//  NetVSBattleMode
	// ================================================================

	private void setRoomInfoForBattle(NetVSBattleMode mode) {
		try {
			java.lang.reflect.Field f = NetDummyMode.class.getDeclaredField("netCurrentRoomInfo");
			f.setAccessible(true);
			nullpomino.game.net.NetRoomInfo ri = new nullpomino.game.net.NetRoomInfo();
			ri.bravo = false;
			ri.counter = false;
			ri.useFractionalGarbage = false;
			ri.garbagePercent = 100;
			ri.divideChangeRateByPlayers = false;
			ri.garbageChangePerAttack = false;
			ri.rensaBlock = false;
			ri.hurryupSeconds = -1;
			ri.isTarget = false;
			ri.reduceLineSend = false;
			f.set(mode, ri);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void battleCalcScoreSingle() {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.nowPieceObject = new Piece(Piece.PIECE_T);
		eng.playerID = 0;
		mode.playerInit(eng, 0);
		mode.netvsIsPractice = true;
		setRoomInfoForBattle(mode);
		assertDoesNotThrow(() -> mode.calcScore(eng, 0, 1));
	}

	@Test
	void battleCalcScoreTspin() {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.nowPieceObject = new Piece(Piece.PIECE_T);
		eng.tspin = true;
		eng.tspinmini = false;
		eng.tspinez = false;
		eng.playerID = 0;
		mode.playerInit(eng, 0);
		mode.netvsIsPractice = true;
		setRoomInfoForBattle(mode);
		assertDoesNotThrow(() -> mode.calcScore(eng, 0, 3));
	}

	@Test
	void battleRenderResult() {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.isVisible = true;
		eng.statistics.lines = 40;
		eng.statistics.totalPieceLocked = 80;
		eng.statistics.lpm = 30.0f;
		eng.statistics.pps = 1.5f;
		eng.statistics.time = 60000;
		setRoomInfoForBattle(mode);
		assertDoesNotThrow(() -> mode.renderResult(eng, 0));
	}

	@Test
	void battleRenderLast() {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.isVisible = true;
		mode.netvsPlayerExist[0] = true;
		setRoomInfoForBattle(mode);
		assertDoesNotThrow(() -> mode.renderLast(eng, 0));
	}

	@Test
	void battleOnLast() {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.playerID = 0;
		mode.playerInit(eng, 0);
		mode.onLast(eng, 0);
	}

	// ================================================================
	//  NetVSLineRaceMode
	// ================================================================

	@Test
	void lineRaceCalcScorePractice() {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.statistics.lines = 80;
		eng.playerID = 0;
		mode.netvsIsPractice = true;
		mode.calcScore(eng, 0, 1);
		assertEquals(GameEngine.Status.EXCELLENT, eng.stat);
	}

	@Test
	void lineRaceRenderLast() {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.isVisible = true;
		mode.netvsPlayerExist[0] = true;
		mode.netvsIsGameActive = true;
		eng.stat = GameEngine.Status.MOVE;
		assertDoesNotThrow(() -> mode.renderLast(eng, 0));
	}

	@Test
	void lineRaceRenderResult() {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.isVisible = true;
		eng.statistics.lines = 40;
		eng.statistics.totalPieceLocked = 80;
		eng.statistics.lpm = 30.0f;
		eng.statistics.pps = 1.5f;
		eng.statistics.time = 60000;
		assertDoesNotThrow(() -> mode.renderResult(eng, 0));
	}

	@Test
	void lineRaceNetRecvStats() {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		String[] msg = new String[] {"game", "0", "0", "stats", "25", "1.2", "28.0"};
		mode.netRecvStats(eng, msg);
		assertEquals(25, eng.statistics.lines);
	}

	// ================================================================
	//  NetVSDigRaceMode
	// ================================================================

	@Test
	void digRaceOnReadyFillsGarbage() {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.statc[0] = 0;
		mode.netvsPlayerExist[0] = true;
		// Set up room info via reflection for the garbage fill
		try {
			java.lang.reflect.Field f = NetDummyMode.class.getDeclaredField("netCurrentRoomInfo");
			f.setAccessible(true);
			nullpomino.game.net.NetRoomInfo ri = new nullpomino.game.net.NetRoomInfo();
			ri.useMap = false;
			ri.garbagePercent = 100;
			f.set(mode, ri);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		mode.onReady(eng, 0);
	}

	@Test
	void digRaceRenderLast() {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.isVisible = true;
		mode.netvsPlayerExist[0] = true;
		mode.netvsIsGameActive = true;
		eng.stat = GameEngine.Status.MOVE;
		// Set private field via reflection
		try {
			java.lang.reflect.Field f = NetVSDigRaceMode.class.getDeclaredField("playerRemainLines");
			f.setAccessible(true);
			f.set(mode, new int[]{10, 0, 0, 0, 0, 0});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		assertDoesNotThrow(() -> mode.renderLast(eng, 0));
	}

	@Test
	void digRaceRenderResult() {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		eng.ruleopt.fieldWidth = 10;
		eng.ruleopt.fieldHeight = 20;
		eng.ruleopt.fieldHiddenHeight = 4;
		eng.createFieldIfNeeded();
		eng.isVisible = true;
		eng.statistics.lines = 18;
		eng.statistics.totalPieceLocked = 30;
		eng.statistics.lpm = 20.0f;
		eng.statistics.pps = 1.0f;
		eng.statistics.time = 90000;
		assertDoesNotThrow(() -> mode.renderResult(eng, 0));
	}

	@Test
	void digRaceNetRecvStats() {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mode.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine eng = mgr.engine[0];
		String[] msg = new String[] {"game", "0", "0", "stats", "12"};
		mode.netRecvStats(eng, msg);
	}

	@Test
	void digRaceGetName() {
		assertEquals("NET-VS-DIG RACE", new NetVSDigRaceMode().getName());
	}

	@Test
	void lineRaceGetName() {
		assertEquals("NET-VS-LINE RACE", new NetVSLineRaceMode().getName());
	}

	@Test
	void battleGetName() {
		assertEquals("NET-VS-BATTLE", new NetVSBattleMode().getName());
	}
}
