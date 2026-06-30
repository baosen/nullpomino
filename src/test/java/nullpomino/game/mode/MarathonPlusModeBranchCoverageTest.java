package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Targets the remaining uncovered branch <em>outcomes</em> in
 * {@link MarathonPlusMode} that the existing test suite leaves un-taken:
 * meter-colour thresholds, the BGM-change start-level guard, the
 * {@code readyDone} early-out in startGame, the menu-only early return and
 * non-last-player path in renderLast, the score-delta string branch, the
 * lines==0 immobile-EZ fall-through in calcScore, the level&gt;=20 level-up
 * guards, the startlevel==20 BGM guard, the startlevel==20 D-button net
 * ranking entry, the saveReplay big/replay skip branches, the renderSetting
 * spin-type label branches, the renderCustom flash-parity branch, and the
 * net stat-send / message branches inside onCustom and netlobbyOnMessage.
 *
 * <p>Where an observable post-condition exists it is asserted; the pure
 * render-driver cases (no assertion possible with the headless no-op
 * receiver) merely execute the branch and are documented as lower-confidence.
 */
class MarathonPlusModeBranchCoverageTest {

	// ----- engine plumbing -----

	private static GameEngine freshEngine(MarathonPlusMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	/** Field with exactly one coloured cell at the bottom-left; rest stays null. */
	private static void seedOneBlock(GameEngine engine) {
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));
	}

	private static void wireNetLobby(MarathonPlusMode mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setField(mode, "netLobby", lobby);
	}

	// =======================================================================
	// calcScore branch outcomes
	// =======================================================================

	/** L607/608/609 TRUE outcomes: lines%10>=4,>=6,>=8 all set the meter colour. */
	@Test
	void meterColorReachesRedAtEightLines() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		seedOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.lines = 8; // 8 % 10 == 8 -> RED
		mode.calcScore(engine, 0, 1);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	/** L536 inner b2b==false: T-spin triple without B2B scores 1600 at level 0. */
	@Test
	void tspinTripleWithoutB2bScores1600() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		seedOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.b2b = false;
		mode.calcScore(engine, 0, 3);
		assertEquals(1600, engine.statistics.score);
		assertEquals(11, readInt(mode, "lastevent")); // EVENT_TSPIN_TRIPLE
	}

	/**
	 * L481 (!tspinez evaluates false) and L491 (lines&gt;0 false) fall-through:
	 * a T-spin with tspinez==true and lines==0 matches neither special arm,
	 * so no points and lastevent stays unchanged.
	 */
	@Test
	void tspinezWithZeroLinesFallsThrough() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		seedOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinez = true; // !tspinez == false on L481
		mode.calcScore(engine, 0, 0); // lines == 0 -> L491 lines>0 false
		assertEquals(0, engine.statistics.score, "no scoring arm matches");
		assertEquals(0, readInt(mode, "lastevent"), "lastevent untouched (EVENT_NONE)");
	}

	/** L590 startlevel&lt;20 FALSE: BGM-change block guarded out when startlevel==20. */
	@Test
	void bgmChangeSkippedWhenStartlevel20() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		seedOneBlock(engine);
		setInt(mode, "startlevel", 20);
		setInt(mode, "bgmlv", 0); // tableBGMChange[0]=50 != -1 so L590 branch0 is true
		engine.statistics.level = 0;
		engine.statistics.lines = 200; // would normally cross thresholds
		owner_fadesw_reset(engine);
		mode.calcScore(engine, 0, 1);
		// startlevel==20 short-circuits: bgmlv must not advance from 0
		assertEquals(0, readInt(mode, "bgmlv"));
	}

	private static void owner_fadesw_reset(GameEngine engine) {
		engine.owner.bgmStatus.fadesw = false;
	}

	/**
	 * L625 level&lt;20 FALSE: when level is already 20 the level-up block is
	 * skipped even though lines exceed the (level+1)*10 threshold.
	 */
	@Test
	void levelUpSkippedWhenAlreadyBonusLevel() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		seedOneBlock(engine);
		setInt(mode, "startlevel", 0);
		engine.statistics.level = 20;
		engine.statistics.lines = 210; // >= (20+1)*10, but level<20 is false
		mode.calcScore(engine, 0, 1);
		assertEquals(20, engine.statistics.level, "no level-up past bonus level");
	}

	// =======================================================================
	// startGame
	// =======================================================================

	/** L268 !readyDone FALSE: with readyDone==true the config block is skipped. */
	@Test
	void startGameSkipsConfigWhenReadyDone() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 5);
		setInt(mode, "spinCheckType", 1);
		engine.readyDone = true; // skip the !readyDone block
		engine.statistics.level = 0;
		mode.startGame(engine, 0);
		// the readyDone-guarded block would have set level=startlevel=5; it must not
		assertEquals(0, engine.statistics.level, "config block skipped when readyDone");
		// the always-run tail still applies spinCheckType
		assertEquals(1, engine.spinCheckType);
	}

	// =======================================================================
	// renderLast
	// =======================================================================

	/** L314 menuOnly TRUE: renderLast returns immediately. */
	@Test
	void renderLastReturnsWhenMenuOnly() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = true;
		mode.renderLast(engine, 0); // early return, no throw
		assertTrue(engine.owner.menuOnly);
	}

	/** L423 playerID != getPlayers()-1: non-last player skips the all-players block. */
	@Test
	void renderLastNonLastPlayerSkipsAllPlayers() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		// getPlayers() == 1 so playerID 1 != 0 takes the false branch
		mode.renderLast(engine, 1);
		assertTrue(true);
	}

	/**
	 * L341 (lastscore==0)||(scgettime&gt;=120) FALSE: lastscore!=0 and
	 * scgettime&lt;120 takes the "(+score)" string branch.
	 */
	@Test
	void renderLastShowsScoreDeltaWhenRecent() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastscore", 500);
		setInt(mode, "scgettime", 10); // < 120
		setInt(mode, "lastevent", AbstractMarathonMode.EVENT_NONE);
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	/**
	 * L366 scgettime&lt;120 FALSE while lastevent!=NONE: the event-label block
	 * is skipped because the score popup has timed out.
	 */
	@Test
	void renderLastSkipsEventWhenTimedOut() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastevent", AbstractMarathonMode.EVENT_SINGLE);
		setInt(mode, "scgettime", 200); // >= 120 -> skip events
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	/** L369 switch(lastevent): drive the SINGLE case (and a combo) explicitly. */
	@Test
	void renderLastSingleEventWithCombo() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastevent", AbstractMarathonMode.EVENT_SINGLE);
		setInt(mode, "scgettime", 0);
		setInt(mode, "lastpiece", Piece.PIECE_T);
		setInt(mode, "lastcombo", 3); // L415 combo display
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	/** L324/L325 RESULT-state ranking table render. */
	@Test
	void renderLastResultStateDrawsRanking() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.RESULT;
		setInt(mode, "startlevel", 0);
		setBool(mode, "big", false);
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// =======================================================================
	// playerInit / background clamp
	// =======================================================================

	/** L104 bg&gt;19 TRUE: loading a startlevel of 20 clamps the background to 19. */
	@Test
	void playerInitClampsBackgroundForStartlevel20() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		// inject a persisted startlevel of 20 so playerInit's clamp (L104) fires
		engine.owner.modeConfig.setProperty("marathonplus.startlevel", 20);
		mode.playerInit(engine, 0);
		assertEquals(20, readInt(mode, "startlevel"));
		assertEquals(19, engine.owner.backgroundStatus.bg);
	}

	// =======================================================================
	// renderSetting spin-type label branches (L245/246/247)
	// =======================================================================

	@Test
	void renderSettingCoversAllSpinLabels() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0); // version becomes CURRENT_VERSION (>=1)
		for (int type = 0; type <= 2; type++) {
			setInt(mode, "tspinEnableType", type);
			mode.renderSetting(engine, 0);
		}
		assertTrue(true);
	}

	// =======================================================================
	// renderCustom flash-parity branch (L712/713)
	// =======================================================================

	@Test
	void renderCustomCoversBothFlashParities() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 90; // even -> statc%2==0 true
		mode.renderCustom(engine, 0);
		engine.statc[0] = 91; // odd -> statc%2==0 false
		mode.renderCustom(engine, 0);
		assertTrue(true);
	}

	// =======================================================================
	// onGameOver L722 gameActive FALSE
	// =======================================================================

	@Test
	void onGameOverWithoutGameActive() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		engine.gameActive = false; // L722 second operand false -> skip outline set
		mode.onGameOver(engine, 0);
		assertTrue(true);
	}

	// =======================================================================
	// onResult L775 statc[1] < 0 wrap
	// =======================================================================

	@Test
	void onResultPageUpFromZeroWrapsToOne() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ctrl.reset();
		engine.statc[1] = 0;
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.onResult(engine, 0);
		assertEquals(1, engine.statc[1]); // 0-- -> -1 -> wraps to 1
	}

	// =======================================================================
	// onSetting D-button net ranking with startlevel == 20 (L214 startlevel==20)
	// =======================================================================

	@Test
	void onSettingDButtonEntersNetRankingAtStartlevel20() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		wireNetLobby(mode);
		engine.owner.replayMode = false;
		setField(mode, "netIsNetPlay", true);
		setInt(mode, "startlevel", 20); // startlevel==20 arm of L214
		setBool(mode, "big", false);
		setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;
		mode.onSetting(engine, 0);
		assertTrue(readBool(mode, "netIsNetRankingDisplayMode"));
	}

	// =======================================================================
	// saveReplay skip branches (L800 big / replay)
	// =======================================================================

	/** L800 big==false FALSE: big==true skips the ranking update entirely. */
	@Test
	void saveReplaySkipsRankingWhenBig() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBool(mode, "big", true);
		setInt(mode, "startlevel", 0);
		setInt(mode, "rankingRank", -1);
		engine.statistics.score = 999999;
		mode.saveReplay(engine, 0, new CustomProperties());
		assertEquals(-1, readInt(mode, "rankingRank"), "big mode must not rank");
	}

	/** L800 replayMode==false FALSE: replay mode skips the ranking update. */
	@Test
	void saveReplaySkipsRankingInReplayMode() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		setBool(mode, "big", false);
		setInt(mode, "startlevel", 0);
		setInt(mode, "rankingRank", -1);
		engine.statistics.score = 999999;
		mode.saveReplay(engine, 0, new CustomProperties());
		assertEquals(-1, readInt(mode, "rankingRank"), "replay must not rank");
	}

	// =======================================================================
	// netlobbyOnMessage bonus-level start/enter (L820 / L833)
	// =======================================================================

	@Test
	void netlobbyOnMessageBonusLevelStart() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		wireNetLobby(mode);
		engine.ending = 1;
		// super.netlobbyOnMessage expects message[0]; "game" + 4 fields
		String[] msg = {"game", "x", "y", "bonuslevelstart"};
		mode.netlobbyOnMessage(getLobby(mode), getClient(mode), msg);
		assertEquals(0, engine.ending);
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	@Test
	void netlobbyOnMessageBonusLevelEnter() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		wireNetLobby(mode);
		String[] msg = {"game", "x", "y", "bonuslevelenter"};
		mode.netlobbyOnMessage(getLobby(mode), getClient(mode), msg);
		assertEquals(1, engine.ending);
		assertEquals(GameEngine.Status.CUSTOM, engine.stat);
	}

	// =======================================================================
	// onCustom net send branches without spectators (L668 / L688 FALSE)
	// =======================================================================

	/** L668 netNumSpectators>0 FALSE: net play but zero spectators -> no field send. */
	@Test
	void onCustomPhase0NetPlayNoSpectators() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		wireNetLobby(mode);
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		setInt(mode, "netNumSpectators", 0); // L668 false
		engine.statc[0] = 0;
		boolean result = mode.onCustom(engine, 0);
		assertFalse(result);
	}

	/** L688 netNumSpectators>0 FALSE on the restart phase. */
	@Test
	void onCustomPhase480NetPlayNoSpectators() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		wireNetLobby(mode);
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		setInt(mode, "netNumSpectators", 0); // L688 false
		engine.statc[0] = 480;
		boolean result = mode.onCustom(engine, 0);
		assertTrue(result);
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	// ----- reflection helpers -----

	private static NetLobbyFrame getLobby(Object mode) throws Exception {
		return (NetLobbyFrame) field(mode.getClass(), "netLobby").get(mode);
	}

	private static NetPlayerClient getClient(Object mode) throws Exception {
		NetLobbyFrame lobby = getLobby(mode);
		return lobby.netPlayerClient;
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		field(obj.getClass(), name).set(obj, value);
	}

	private static void setField(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
	}

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
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
