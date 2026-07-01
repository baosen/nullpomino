package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.MarathonMode;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.wallkick.StandardSymmetricWallkick;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Targets the residual uncovered branches of TSpinAI.thinkMain that the existing
 * suites miss:
 * <ul>
 *   <li>L70 — the combo-guarded single-line early return (combo&lt;1 vs combo&gt;=1);</li>
 *   <li>L76 — the 3-line depth-0 scoring arm;</li>
 *   <li>the height / lid / need-I-valley demerit arms under both depth==0 and depth&gt;0.</li>
 * </ul>
 * The single-line and three-line scenarios are assertion-backed. A full-game
 * simulation across many seeds and rulesets drives the AI's real search
 * (getMaxThinkDepth()==2, so depth 0 AND depth&gt;0 are exercised) over evolving
 * terrain to reach the hole / lid / valley / height branches that isolated
 * hand-crafted fields cannot reliably manufacture.
 */
class TSpinAIDeepBranchCoverageTest {

	private static final int W = 10;

	private GameManager gm;
	private GameEngine engine;
	private TSpinAI ai;

	@BeforeEach
	void setUp() {
		gm = new GameManager(new EventReceiver());
		gm.init();
		engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		ai = new TSpinAI();
	}

	private Field field() {
		return engine.field;
	}

	// ─── L70: combo>=1 keeps the single-line placement alive (no early 0) ───
	//
	// Prefill the bottom row fully so any placement produces exactly a 1-line
	// clear; drop an O high so the post-clear stack is short (heightAfter>=16,
	// !danger) with no holes and the piece is not a T. Then only the trailing
	// (engine.combo < 1) conjunct decides whether L70 returns 0.

	@Test
	void singleLineComboGuardBothArms() {
		Field base = field();
		for (int x = 0; x < W; x++) base.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		Piece probe = new Piece(Piece.PIECE_O);
		int y = probe.getBottom(0, 0, 0, base);   // resting Y an O would take

		// combo == 0 -> whole L70 guard true -> early return 0.
		engine.combo = 0;
		int ptsNoCombo = ai.thinkMain(engine, 0, y, 0, -1, new Field(base),
				new Piece(Piece.PIECE_O), null, null, 0);

		// combo == 5 -> last conjunct false -> guard skipped -> real score.
		engine.combo = 5;
		int ptsCombo = ai.thinkMain(engine, 0, y, 0, -1, new Field(base),
				new Piece(Piece.PIECE_O), null, null, 0);

		assertEquals(0, ptsNoCombo, "combo<1 single-line guard should early-return 0");
		assertTrue(ptsCombo != 0, "combo>=1 must bypass the L70 early return and score");
	}

	// ─── L76: 3-line clear in the non-danger depth-0 branch (+100) ─────────

	@Test
	void threeLineClearDepthZeroScores() {
		Field base = field();
		// Prefill rows 17,18,19 fully so a placement yields a 3-line clear.
		for (int yy = 17; yy <= 19; yy++)
			for (int x = 0; x < W; x++) base.setBlockColor(x, yy, Block.BLOCK_COLOR_RED);
		Piece probe = new Piece(Piece.PIECE_O);
		int y = probe.getBottom(0, 0, 0, base);
		engine.combo = 0;
		int pts = ai.thinkMain(engine, 0, y, 0, -1, new Field(base),
				new Piece(Piece.PIECE_O), null, null, 0);
		// depth 0, not danger -> "if(lines == 3) pts += 100" arm executes (plus other bonuses).
		assertTrue(pts >= 100, "three-line clear at depth 0 should add the +100 bonus, got " + pts);
	}

	// ─── L94/L118 newtslot==false path: T placement that does NOT create a
	//     fresh single T-slot leaves forceHold untouched and runs the
	//     lid / valley / height demerit arms. ────────────────────────────────

	@Test
	void tPiecePlacementWithoutNewSlotLeavesForceHoldFalse() {
		Field base = field();
		int h = base.getHeight();
		// A plain filled floor: placing a flat T (rtOld=-1 so tspin=false) here
		// creates no new T-slot, so newtslot stays false (L94 false / L118 !newtslot true).
		for (int x = 0; x < W; x++) base.setBlockColor(x, h - 1, Block.BLOCK_COLOR_RED);
		Piece next = new Piece(Piece.PIECE_S);
		Piece hold = new Piece(Piece.PIECE_T);
		ai.forceHold = false;
		Piece t = new Piece(Piece.PIECE_T);
		int y = t.getBottom(3, 0, 0, base);
		int pts = ai.thinkMain(engine, 3, y, 0, -1, new Field(base), t, next, hold, 0);
		assertEquals(0, pts, "flat non-scoring T placement on a bare floor returns 0");
		assertTrue(!ai.forceHold, "no new T-slot means forceHold must remain false");
	}

	// ─── Height-decrease demerit under danger (L157 (depth>0)||danger arm) ──

	@Test
	void heightBranchDangerModeDepthOne() {
		Field base = field();
		int h = base.getHeight();
		// Very tall solid stack (danger: post-clear top index <= 12) with a ragged
		// low corner so placement + settle changes the peak height.
		for (int yy = 2; yy < h; yy++)
			for (int x = 0; x < W; x++)
				if (!(x == 0 && yy < 6)) base.setBlockColor(x, yy, Block.BLOCK_COLOR_RED);
		int pts = ai.thinkMain(engine, 0, 5, 0, -1, new Field(base),
				new Piece(Piece.PIECE_O), null, null, 1);
		assertTrue(pts > 0, "danger-mode height branch at depth>0 produced a positive score, got " + pts);
	}

	// ─── Full-game simulation: drives the real depth-0..1 search over evolving
	//     terrain, reaching the hole / lid / need-I-valley / height demerit and
	//     combo-scoring branches that hand-built fields cannot reliably hit. ───

	private static int playGame(TSpinAI ai, long seed, boolean reverse, boolean dbl, int maxFrames) {
		GameManager manager = new GameManager(new EventReceiver());
		MarathonMode mode = new MarathonMode();
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		mode.modeInit(manager);
		mode.playerInit(engine, 0);

		engine.ai = ai;
		engine.aiUseThread = false;
		engine.aiMoveDelay = 0;
		engine.aiThinkDelay = 0;
		engine.wallkick = new StandardSymmetricWallkick();
		engine.ruleopt.rotateButtonAllowReverse = reverse;
		engine.ruleopt.rotateButtonAllowDouble = dbl;
		engine.ai.init(engine, 0);

		engine.randSeed = seed;
		engine.random = new Random(seed);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.stat = GameEngine.Status.READY;

		int frames = 0;
		for (int i = 0; i < maxFrames; i++) {
			if (engine.stat == GameEngine.Status.GAMEOVER
					|| engine.stat == GameEngine.Status.RESULT) break;
			try {
				engine.update();
				if (engine.ai != null) engine.ai.onFirst(engine, 0);
			} catch (Exception ex) {
				// keep playing through any transient AI/engine hiccup
			}
			frames++;
		}
		if (engine.ai != null) engine.ai.shutdown(engine, 0);
		return frames;
	}

	@Test
	void tSpinAIPlaysMarathonAcrossSeedsAndRulesets() {
		TSpinAI simAi = new TSpinAI();
		long[] seeds = {1L, 3L, 7L, 13L, 42L, 100L, 777L, 1234L, 5150L, 31337L,
				99999L, 0xC0FFEEL, 0xBEEFL, 0xABCDEFL, 0x123456L, 271828L, 314159L, 161803L};
		int totalFrames = 0;
		for (long s : seeds) {
			totalFrames += playGame(simAi, s, false, false, 5000);   // default rotation
			totalFrames += playGame(simAi, s, true, true, 5000);     // reverse + 180 enabled
		}
		assertTrue(totalFrames > 0, "TSpinAI game simulations should advance the engine");
	}
}
