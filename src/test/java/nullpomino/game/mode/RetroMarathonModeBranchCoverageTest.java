package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch coverage for uncovered paths in {@link RetroMarathonMode}.
 * Targets calcScore with B-Type meter/ending/score cap, Arrange type
 * meter, setSpeed arrange branch, startGame big flag, onReady fillGarbage,
 * saveReplay with AI, renderSetting, and the fillGarbage method.
 */
class RetroMarathonModeBranchCoverageTest {

	@Test
	void calcScoreBTypeMeterRed() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 1);
		e.statistics.lines = 20;
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));
		m.calcScore(e, 0, 1);
		assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
	}

	@Test
	void calcScoreBTypeEnding() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 1);
		setInt(m, "startheight", 3);
		e.statistics.level = 0;
		// engine.statistics.lines is total lines; B-Type triggers at lines >= 25
		e.statistics.lines = 25;
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));
		m.calcScore(e, 0, 0);
		assertEquals(1, e.ending);
	}

	@Test
	void calcScoreScoreCapNonArrange() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 0);
		e.statistics.score = 999990;
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));
		m.calcScore(e, 0, 1);
		assertEquals(999999, e.statistics.score);
	}

	@Test
	void calcScoreArrangeNoCap() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 2);
		e.statistics.score = 999990;
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));
		m.calcScore(e, 0, 1);
		// Arrange has no cap
		assertTrue(e.statistics.score > 999999);
	}

	@Test
	void calcScoreArrangeLevelUp() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 2);
		setInt(m, "levellines", 10);
		e.statistics.lines = 10;
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));
		m.calcScore(e, 0, 1);
		assertEquals(1, e.statistics.level);
	}

	@Test
	void calcScoreLevelOver255Wraps() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 0);
		setInt(m, "levellines", 10);
		e.statistics.lines = 10;
		e.statistics.level = 255;
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));
		m.calcScore(e, 0, 1);
		assertEquals(0, e.statistics.level);
	}

	@Test
	void setSpeedArrange() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		setInt(m, "gametype", 2);
		e.statistics.level = 0;
		Method me = RetroMarathonMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		me.setAccessible(true);
		me.invoke(m, e);
		assertEquals(1, e.speed.gravity);
		assertEquals(48, e.speed.denominator);
	}

	@Test
	void setSpeedArrangeHighLevel() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		setInt(m, "gametype", 2);
		e.statistics.level = 999;
		Method me = RetroMarathonMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		me.setAccessible(true);
		me.invoke(m, e);
		assertTrue(e.speed.denominator > 0);
	}

	@Test
	void setSpeedNonArrange() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		setInt(m, "gametype", 0);
		e.statistics.level = 0;
		Method me = RetroMarathonMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		me.setAccessible(true);
		me.invoke(m, e);
		assertEquals(1, e.speed.gravity);
		assertEquals(48, e.speed.denominator);
	}

	@Test
	void startGameBig() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		setBool(m, "big", true);
		setInt(m, "startlevel", 3);
		m.startGame(e, 0);
		assertTrue(e.big);
		assertEquals(3, e.statistics.level);
	}

	@Test
	void onReadyFillGarbage() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.createFieldIfNeeded();
		setInt(m, "startheight", 3);
		setInt(m, "version", 2);
		m.onReady(e, 0);
		// Fill garbage should produce some garbage blocks
		assertNotNull(e.field.getBlock(0, e.field.getHeight() - 1));
	}

	@Test
	void saveReplaySkipsRankingWhenBig() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		setBool(m, "big", true);
		CustomProperties prop = new CustomProperties();
		m.saveReplay(e, 0, prop);
	}

	@Test
	void saveReplaySkipsRankingWhenAi() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.ai = new nullpomino.game.ai.DummyAI();
		CustomProperties prop = new CustomProperties();
		m.saveReplay(e, 0, prop);
	}

	@Test
	void renderSettingShowsMenu() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		m.renderSetting(e, 0);
	}

	@Test
	void onSettingReplayMode() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = true;
		setInt(m, "menuTime", 60);
		assertFalse(m.onSetting(e, 0));
	}

	@Test
	void onSettingChangeGametype() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 0);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		m.onSetting(e, 0);
	}

	@Test
	void onSettingChangeStartlevel() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 1);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		m.onSetting(e, 0);
	}

	@Test
	void onSettingChangeHeight() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 2);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		m.onSetting(e, 0);
	}

	@Test
	void loadSettingFromCustomProperties() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		CustomProperties p = new CustomProperties();
		p.setProperty("retromarathon.gametype", 2);
		p.setProperty("retromarathon.startlevel", 7);
		p.setProperty("retromarathon.startheight", 3);
		p.setProperty("retromarathon.big", true);
		p.setProperty("retromarathon.version", 2);
		m.loadSetting(p);
		assertEquals(2, getInt(m, "gametype"));
		assertEquals(7, getInt(m, "startlevel"));
		assertTrue(getBool(m, "big"));
	}

	@Test
	void saveSettingRoundTrip() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		setInt(m, "gametype", 1);
		CustomProperties p = new CustomProperties();
		m.saveSetting(p);
		assertEquals(1, p.getProperty("retromarathon.gametype", -1));
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		m.onLast(e, 0);
		assertEquals(1, getInt(m, "scgettime"));
	}

	@Test
	void renderResultDisplays() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		m.renderResult(e, 0);
	}

	@Test
	void calcScoreLinesZero() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		e.field.setBlock(0, e.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));
		m.calcScore(e, 0, 0);
		assertEquals(0, getInt(m, "lastscore"));
	}

	// ---- helpers ----

	private static GameEngine fresh(RetroMarathonMode m) {
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = m;
		mgr.init();
		mgr.engine[0].init();
		mgr.engine[0].ruleopt.fieldWidth = 10;
		mgr.engine[0].ruleopt.fieldHeight = 20;
		mgr.engine[0].ruleopt.fieldHiddenHeight = 4;
		return mgr.engine[0];
	}

	private static int getInt(Object o, String n) throws Exception {
		return findField(o.getClass(), n).getInt(o);
	}

	private static boolean getBool(Object o, String n) throws Exception {
		return findField(o.getClass(), n).getBoolean(o);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		findField(o.getClass(), n).setInt(o, v);
	}

	private static void setBool(Object o, String n, boolean v) throws Exception {
		findField(o.getClass(), n).setBoolean(o, v);
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws Exception {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try { java.lang.reflect.Field f = c.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException e) { /* continue */ }
		}
		throw new NoSuchFieldException(name);
	}
}
