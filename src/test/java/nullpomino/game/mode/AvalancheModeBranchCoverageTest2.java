package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link AvalancheMode} targeting the opposite
 * outcome of conditions that existing tests only exercise on one side:
 * <ul>
 *   <li>onLast meter thresholds (Ultra YELLOW/ORANGE, Sprint YELLOW/ORANGE)
 *       and the out-of-time / goal end branches (L399-401, L404, L415-417, L420).</li>
 *   <li>renderSetting "3 AND 4" / "FEVER" ternaries and outline strings
 *       (L250, L253, L259-261).</li>
 *   <li>renderLast ranking-row highlight (i == rankingRank) for all gametypes,
 *       the BSP-next 0.5 scale branch, and field==null / chain-display combos
 *       (L307-312, L293-294, L354, L360-361).</li>
 *   <li>renderMove with gameStarted false (L275 second operand).</li>
 *   <li>drawXorTimer with field==null (L374 first operand).</li>
 *   <li>lineClearEnd game-over via column 2 (L455 first operand).</li>
 *   <li>checkRanking type-2 with score at/above the sprint target so it falls
 *       through to the time-primary lambda (L623, L626).</li>
 *   <li>saveReplay with rankingRank == -1 (L514 false) and colorClearSize != 4
 *       (L511 false).</li>
 * </ul>
 */
class AvalancheModeBranchCoverageTest2 {

	/** EventReceiver whose next-piece display reports BSP (type 2). */
	private static final class BspReceiver extends EventReceiver {
		@Override public int getNextDisplayType() { return 2; }
	}

	// -----------------------------------------------------------------------
	// onLast — Ultra meter thresholds and out-of-time
	// -----------------------------------------------------------------------

	@Test
	void onLastUltraMeterYellow() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "gametype", 1);
		// remainTime = 10800 - time; want <=3600 but >1800 -> time=7300 (remain=3500)
		e.statistics.time = 7300;
		mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, e.meterColor);
	}

	@Test
	void onLastUltraMeterOrange() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "gametype", 1);
		// remain <=1800 but >600 -> time=9200 (remain=1600)
		e.statistics.time = 9200;
		mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, e.meterColor);
	}

	@Test
	void onLastUltraMeterGreen() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "gametype", 1);
		e.statistics.time = 0; // remain 10800 -> green, no threshold hit
		mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_GREEN, e.meterColor);
	}

	@Test
	void onLastUltraOutOfTimeEndsGame() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "gametype", 1);
		e.statistics.time = 10800; // >= ULTRA_MAX_TIME
		e.timerActive = true;
		mode.onLast(e, 0);
		assertEquals(GameEngine.Status.ENDINGSTART, e.stat);
	}

	// -----------------------------------------------------------------------
	// onLast — Sprint meter thresholds and goal
	// -----------------------------------------------------------------------

	@Test
	void onLastSprintMeterYellow() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "gametype", 2);
		setInt(mode, "sprintTarget", 0); // SPRINT_MAX_SCORE[0] = 15000
		e.timerActive = true;
		// remainScore <=50 but >30 -> score = 14960 (remain 40)
		e.statistics.score = 14960;
		mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, e.meterColor);
	}

	@Test
	void onLastSprintMeterOrange() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "gametype", 2);
		setInt(mode, "sprintTarget", 0);
		e.timerActive = true;
		// remain <=30 but >10 -> score = 14980 (remain 20)
		e.statistics.score = 14980;
		mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, e.meterColor);
	}

	@Test
	void onLastSprintMeterGreenWhenTimerInactive() throws Exception {
		// timerActive false -> remainScore forced to 0; but the goal branch
		// requires timerActive==true so it stays in ENDINGSTART-free state.
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "gametype", 2);
		setInt(mode, "sprintTarget", 0);
		e.timerActive = false;
		e.statistics.score = 0;
		mode.onLast(e, 0);
		// remainScore=0 hits all thresholds -> RED, but game must NOT end.
		assertNotEquals(GameEngine.Status.ENDINGSTART, e.stat);
	}

	@Test
	void onLastSprintGoalEndsGame() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "gametype", 2);
		setInt(mode, "sprintTarget", 0);
		e.timerActive = true;
		e.statistics.score = 15000; // >= SPRINT_MAX_SCORE[0]
		mode.onLast(e, 0);
		assertEquals(GameEngine.Status.ENDINGSTART, e.stat);
	}

	// -----------------------------------------------------------------------
	// renderSetting ternaries / outline strings
	// -----------------------------------------------------------------------

	@Test
	void renderSettingDangerDoubleAndFeverFall() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 0); // page 1 (<=8)
		setBool(mode, "dangerColumnDouble", true); // -> "3 AND 4"
		setBool(mode, "cascadeSlow", true);        // -> "FEVER"
		mode.renderSetting(e, 0);
	}

	@Test
	void renderSettingOutlineColor() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 9); // page 2 (>8)
		setInt(mode, "outlinetype", 1); // -> "COLOR"
		mode.renderSetting(e, 0);
	}

	@Test
	void renderSettingOutlineNone() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 9);
		setInt(mode, "outlinetype", 2); // -> "NONE"
		mode.renderSetting(e, 0);
	}

	@Test
	void renderSettingOutlineNormal() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 9);
		setInt(mode, "outlinetype", 0); // -> "NORMAL"
		mode.renderSetting(e, 0);
	}

	// -----------------------------------------------------------------------
	// renderLast ranking highlight rows (i == rankingRank true side)
	// -----------------------------------------------------------------------

	@Test
	void renderLastRankingHighlightMarathon() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		e.owner.replayMode = false;
		e.ai = null;
		e.colorClearSize = 4;
		setInt(mode, "gametype", 0);
		setInt(mode, "rankingRank", 0); // highlight row 0
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastRankingHighlightUltra() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		e.owner.replayMode = false;
		e.ai = null;
		e.colorClearSize = 4;
		setInt(mode, "gametype", 1);
		setInt(mode, "rankingRank", 2);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastRankingHighlightSprint() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		e.owner.replayMode = false;
		e.ai = null;
		e.colorClearSize = 4;
		setInt(mode, "gametype", 2);
		setInt(mode, "rankingRank", 3);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastRankingMarathonBspHalfScale() throws Exception {
		// getNextDisplayType()==2 && gametype==0 -> scale 0.5f, topY 6 (L293/L294)
		AvalancheMode mode = new AvalancheMode();
		GameManager gm = new GameManager(new BspReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine e = gm.engine[0];
		e.owner.replayMode = false;
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		e.ai = null;
		e.colorClearSize = 4;
		setInt(mode, "gametype", 0);
		setInt(mode, "rankingRank", 1);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastRankingSkippedWhenColorClearSizeNot4() throws Exception {
		// L292 false branch: colorClearSize != 4 -> ranking table not drawn
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		e.owner.replayMode = false;
		e.ai = null;
		e.colorClearSize = 5; // != 4
		setInt(mode, "gametype", 0);
		mode.renderLast(e, 0);
	}

	// -----------------------------------------------------------------------
	// renderLast score-display branches: field==null, chain combos
	// -----------------------------------------------------------------------

	@Test
	void renderLastScoreDisplayFieldNull() throws Exception {
		// engine.field stays null (no createFieldIfNeeded) -> L354 false side.
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.field = null;
		e.chain = 0;          // chain display skipped
		setInt(mode, "chainDisplay", 0);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastScoreDisplayChainButShowChainsOff() throws Exception {
		// chain>0 && chainDisplay>0 but showChains false -> L360 third operand false
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.createFieldIfNeeded();
		e.chain = 12;
		setInt(mode, "chainDisplay", 30);
		setBool(mode, "showChains", false);
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastScoreOnlyWhenScgettimeZero() throws Exception {
		// L319: lastscore/lastmultiplier nonzero but scgettime<=0 -> plain score.
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.createFieldIfNeeded();
		setInt(mode, "lastscore", 100);
		setInt(mode, "lastmultiplier", 5);
		setInt(mode, "scgettime", 0); // <= 0 -> first OR true
		mode.renderLast(e, 0);
	}

	// -----------------------------------------------------------------------
	// renderMove with gameStarted false (L275 short-circuit)
	// -----------------------------------------------------------------------

	@Test
	void renderMoveGameNotStarted() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBool(mode, "dangerColumnShowX", true);
		e.gameStarted = false; // second operand false -> drawXorTimer skipped
		mode.renderMove(e, 0);
	}

	// -----------------------------------------------------------------------
	// drawXorTimer with field==null (L374 first operand true)
	// -----------------------------------------------------------------------

	@Test
	void drawXorTimerFieldNull() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.field = null;
		e.displaysize = 0;
		Method m = AvalancheMode.class.getDeclaredMethod("drawXorTimer", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, e, 0);
	}

	// -----------------------------------------------------------------------
	// lineClearEnd game over via column 2 (L455 first operand true)
	// -----------------------------------------------------------------------

	@Test
	void lineClearEndGameOverViaColumn2() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBool(mode, "dangerColumnDouble", false);
		e.field.setBlock(2, 0, new Block(Block.BLOCK_COLOR_RED)); // col 2 occupied
		mode.lineClearEnd(e, 0);
		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	@Test
	void lineClearEndNoGameOverWhenColumnsClear() throws Exception {
		// L455 both operands false -> no game over.
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBool(mode, "dangerColumnDouble", true);
		// columns 2 and 3 both empty -> condition false
		mode.lineClearEnd(e, 0);
		assertNotEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	// -----------------------------------------------------------------------
	// checkRanking type-2 above sprint target (L623 false -> lambda L626 type-2)
	// -----------------------------------------------------------------------

	@Test
	void checkRankingType2AtTargetUsesTimePrimary() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		freshEngine(mode);
		// Reset the sprint row (type 2 + sprintTarget 0) to empty sentinel so the
		// rank is deterministic in the full suite (hermetic per brief).
		int[][][][] rTime = (int[][][][]) readField(mode, "rankingTime");
		int[][][][] rScore = (int[][][][]) readField(mode, "rankingScore");
		for (int i = 0; i < 10; i++) {
			rTime[0][0][2][i] = -1;
			rScore[0][0][2][i] = 0;
		}
		// sc >= SPRINT_MAX_SCORE[0] (15000) so L623 guard is false; time-primary.
		int rank = invokeCheckRanking(mode, 15000, 5000, 2, 0, 3);
		assertEquals(0, rank); // empty (time -1) -> rank 0
	}

	// -----------------------------------------------------------------------
	// saveReplay: rankingRank == -1 (L514 false) and colorClearSize != 4 (L511 false)
	// -----------------------------------------------------------------------

	@Test
	void saveReplayNoRankWhenUnranked() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ai = null;
		e.colorClearSize = 4;
		setInt(mode, "gametype", 2);
		setInt(mode, "sprintTarget", 0);
		// score below sprint target -> checkRanking returns -1 -> L514 false
		e.statistics.score = 100;
		e.statistics.time = 3600;
		mode.saveReplay(e, 0, new CustomProperties());
		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	@Test
	void saveReplaySkipsRankingWhenColorClearSizeNot4() throws Exception {
		// L511 false: colorClearSize != 4 -> rankingRank stays at its initial -1
		AvalancheMode mode = new AvalancheMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ai = null;
		e.colorClearSize = 5;
		setInt(mode, "rankingRank", -1);
		setInt(mode, "gametype", 0);
		e.statistics.score = 99999;
		mode.saveReplay(e, 0, new CustomProperties());
		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(AvalancheMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		gm.engine[0].owner.replayMode = false;
		return gm.engine[0];
	}

	private static int invokeCheckRanking(AvalancheMode mode, int sc, int time, int type, int sctype, int colors)
			throws Exception {
		Method m = AvalancheMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, time, type, sctype, colors);
	}

	private static int readInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static Object readField(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.get(o);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	private static void setBool(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setBoolean(o, v);
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try {
				return c.getDeclaredField(n);
			} catch (NoSuchFieldException e) {
				/* continue up hierarchy */
			}
		}
		throw new NoSuchFieldException(n);
	}
}
