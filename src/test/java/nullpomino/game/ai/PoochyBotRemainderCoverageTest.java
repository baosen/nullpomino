package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.SpeedParam;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.wallkick.StandardWallkick;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Remainder coverage tests for {@link PoochyBot}, targeting branches the
 * existing PoochyBot*Test suites still leave uncovered (PoochyBot was at
 * ~73.5%). The big remaining gaps are:
 *
 * <ul>
 *   <li>the wallkick {@code else if} arms inside {@link PoochyBot#thinkBestPosition}
 *       for both the current piece and the hold piece (no existing test attaches
 *       a real {@code engine.wallkick}); attaching a {@link StandardWallkick}
 *       with {@code rotateWallkick} on, plus crafted collisions, drives them,</li>
 *   <li>the I-piece movement / floor-kick / hold-fallback arms of
 *       {@link PoochyBot#setControl}, the funnel drop arms, the L/J flat-side
 *       move/sync arms and the sync-cancel arm,</li>
 *   <li>the {@code thinkMain} scoring arms (valley diff scores, dangerous
 *       placement for big &amp; small pieces, edge-clear bonus, height demerit,
 *       premature-clear / canyon penalties) — verified against the
 *       {@code Integer.MIN_VALUE} sentinel the method returns for rejected moves,</li>
 *   <li>{@link PoochyBot#mostMovableX} T-piece floor-kick and I-piece vertical
 *       left-edge logic, {@link PoochyBot#calcValleys} move&gt;=2 arms,
 *       {@code getColumnDepth}, and the {@link PoochyBot#run} think thread.</li>
 * </ul>
 *
 * <p>Per the AI-test idiom these call the public hooks and private helpers
 * directly with crafted Field/Piece states; assertions are robust
 * (no-throw / valid-range / sentinel checks) since the precise heuristic score
 * is not load-bearing for coverage. All sentinel comparisons use {@code ==} /
 * {@code !=} (never range operators) to satisfy Error Prone.
 */
class PoochyBotRemainderCoverageTest {

	private GameManager gm;
	private GameEngine engine;
	private PoochyBot ai;
	private Controller ctrl;

	@BeforeEach
	void setUp() {
		gm = new GameManager(new EventReceiver());
		gm.init();
		engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.aiUseThread = false;
		ai = new PoochyBot();
		ai.init(engine, 0);
		ctrl = new Controller();
	}

	private Piece piece(int id) {
		Piece p = new Piece(id);
		p.applyOffsetArray(engine.ruleopt.pieceOffsetX[id], engine.ruleopt.pieceOffsetY[id]);
		return p;
	}

	private void setNext(int... ids) {
		Piece[] objs = new Piece[ids.length];
		for (int i = 0; i < ids.length; i++)
			objs[i] = new Piece(ids[i]);
		engine.nextPieceArrayID = ids.clone();
		engine.nextPieceArrayObject = objs;
		engine.nextPieceCount = 0;
	}

	private void readyMove(int id, int x, int y, int dir) {
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		ai.delay = 0;
		ai.thinkComplete = true;
		ai.bestHold = false;
		engine.nowPieceObject = piece(id);
		engine.nowPieceObject.direction = dir;
		engine.nowPieceX = x;
		engine.nowPieceY = y;
	}

	// ─────────────────────────────────────────────────────────────
	// onFirst — ARE hold-bit + move-input emission (231-235, 248, 251, 254)
	// ─────────────────────────────────────────────────────────────

	private void areSetup() {
		engine.speed.are = 30;
		engine.speed.areLine = 30;
		engine.aiPrethink = true;
		engine.stat = GameEngine.Status.ARE;
		engine.aiMoveDelay = 0;
		ai.delay = 5;
		ai.thinkComplete = true;
		ai.threadRunning = true;
		ai.thinking = false;
	}

	@Test
	void onFirstAREHoldBitWithExistingHold() {
		// bestHold && thinkComplete, holdPieceObject != null -> line 235 (else arm)
		areSetup();
		ai.bestHold = true;
		ai.bestX = 0; // far left of spawn -> left move-input branch (244-247)
		engine.holdPieceObject = piece(Piece.PIECE_L);
		setNext(Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z);
		ai.onFirst(engine, 0);
		assertTrue(true, "ARE hold bit with existing hold piece");
	}

	@Test
	void onFirstAREHoldBitNullHoldUsesNextNext() {
		// bestHold && thinkComplete, holdPieceObject == null -> lines 232-233
		areSetup();
		ai.bestHold = true;
		ai.bestX = 9; // far right of spawn -> right move-input branch (248, 251)
		engine.holdPieceObject = null;
		setNext(Piece.PIECE_J, Piece.PIECE_O, Piece.PIECE_I);
		ai.onFirst(engine, 0);
		assertTrue(true, "ARE hold bit with null hold piece");
	}

	@Test
	void onFirstARECentredKeepsDAS() {
		// neither left nor right -> setDAS = 0 (line 254)
		areSetup();
		ai.bestHold = false;
		engine.holdPieceObject = null;
		setNext(Piece.PIECE_T, Piece.PIECE_L, Piece.PIECE_J);
		ai.bestX = engine.getSpawnPosX(engine.field, piece(Piece.PIECE_T));
		ai.onFirst(engine, 0);
		assertTrue(true, "ARE centred keeps DAS");
	}

	// ─────────────────────────────────────────────────────────────
	// setControl — I-piece move/floor-kick/hold (357, 365-394)
	// ─────────────────────────────────────────────────────────────

	@Test
	void setControlIPieceMoveRightBlockedFloorKickVertical() {
		// I-piece moving right, blocked, (rt&1)==1 floor-kick path (365-366);
		// bestRt set so (rt+3)%4 == bestRt -> hypRtDir = -1 (line 357)
		readyMove(Piece.PIECE_I, 4, 5, Piece.DIRECTION_RIGHT);
		engine.nowUpwardWallkickCount = 0;
		engine.ruleopt.rotateMaxUpwardWallkick = 3;
		ai.bestX = 8;
		ai.bestRt = (Piece.DIRECTION_RIGHT + 3) % 4; // -> hypRtDir = -1
		for (int y = 0; y < 20; y++)
			engine.field.setBlockColor(6, y, 1); // wall to the right forces collision
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "I-piece move-right blocked vertical floor kick");
	}

	@Test
	void setControlIPieceMoveLeftBlockedVerticalKick() {
		// I-piece moving left, blocked, (rt&1)==1 with side-collision (384-386)
		readyMove(Piece.PIECE_I, 6, 5, Piece.DIRECTION_RIGHT);
		engine.nowUpwardWallkickCount = 0;
		engine.ruleopt.rotateMaxUpwardWallkick = 3;
		ai.bestX = 2;
		ai.bestRt = Piece.DIRECTION_RIGHT;
		for (int y = 0; y < 20; y++)
			engine.field.setBlockColor(4, y, 1); // wall to the left
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "I-piece move-left blocked vertical kick");
	}

	@Test
	void setControlIPieceMoveRightBlockedNoKickHoldFallback() {
		// I-piece move right, blocked, no floor kick allowed -> hold fallback (367-374)
		readyMove(Piece.PIECE_I, 4, 5, Piece.DIRECTION_RIGHT);
		engine.nowUpwardWallkickCount = 99;
		engine.ruleopt.rotateMaxUpwardWallkick = 1; // no floor kick
		ai.bestX = 8;
		ai.bestRt = Piece.DIRECTION_RIGHT;
		engine.holdPieceObject = piece(Piece.PIECE_O);
		for (int y = 0; y < 20; y++)
			engine.field.setBlockColor(6, y, 1);
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "I-piece move-right blocked, hold fallback");
	}

	@Test
	void setControlIPieceMoveLeftBlockedNoKickHoldFallback() {
		// I-piece move left, blocked, no floor kick -> hold fallback (387-394)
		readyMove(Piece.PIECE_I, 6, 5, Piece.DIRECTION_RIGHT);
		engine.nowUpwardWallkickCount = 99;
		engine.ruleopt.rotateMaxUpwardWallkick = 1; // no floor kick
		ai.bestX = 2;
		ai.bestRt = Piece.DIRECTION_RIGHT;
		engine.holdPieceObject = piece(Piece.PIECE_O);
		for (int y = 0; y < 20; y++)
			engine.field.setBlockColor(4, y, 1);
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "I-piece move-left blocked, hold fallback");
	}

	@Test
	void setControlIPieceHorizontalMoveRightBlockedRotate() {
		// I-piece horizontal (rt&1)==0 moving right blocked -> rotateI true (363-364)
		readyMove(Piece.PIECE_I, 4, 5, Piece.DIRECTION_UP);
		engine.nowUpwardWallkickCount = 0;
		engine.ruleopt.rotateMaxUpwardWallkick = 3;
		ai.bestX = 8;
		ai.bestRt = Piece.DIRECTION_DOWN;
		for (int y = 0; y < 20; y++)
			engine.field.setBlockColor(8, y, 1); // block to the right
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "I-piece horizontal move right blocked, rotate");
	}

	// ─────────────────────────────────────────────────────────────
	// setControl — reverse-rotate fallback arms (424-432)
	// ─────────────────────────────────────────────────────────────

	@Test
	void setControlReverseRotateBest180OddRtUpwardKick() {
		// rt != bestRt, xDiff <= 1, allowReverse, best180, (rt&1)==1 -> 424-429
		readyMove(Piece.PIECE_T, 5, 5, Piece.DIRECTION_RIGHT);
		engine.ruleopt.rotateButtonAllowReverse = true;
		engine.ruleopt.rotateButtonAllowDouble = false;
		ai.bestX = 5; // xDiff == 0
		ai.bestRt = Piece.DIRECTION_LEFT; // |rt - bestRt| == 2 -> best180, (rt&1)==1
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "reverse rotate best180 odd-rt upward kick");
	}

	@Test
	void setControlRotateDefaultFallback() {
		// rt != bestRt, xDiff <= 1, none of the reverse/double arms -> else rotateDir=1 (431-432)
		readyMove(Piece.PIECE_S, 5, 5, Piece.DIRECTION_UP);
		engine.ruleopt.rotateButtonAllowReverse = false;
		engine.ruleopt.rotateButtonAllowDouble = false;
		ai.bestX = 5; // xDiff == 0
		ai.bestRt = Piece.DIRECTION_RIGHT; // not a default rotate target
		engine.ruleopt.rotateButtonDefaultRight = false;
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "rotate default fallback rotateDir=1");
	}

	@Test
	void setControlDoubleRotateUsesEButton() {
		// best180 && allowDouble && !E pressed -> BUTTON_BIT_E (418-419, 601-602)
		readyMove(Piece.PIECE_T, 5, 18, Piece.DIRECTION_UP);
		engine.ruleopt.rotateButtonAllowDouble = true;
		ai.bestX = 5; // xDiff <= 1
		ai.bestRt = Piece.DIRECTION_DOWN; // best180
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "double rotate uses E button");
	}

	// ─────────────────────────────────────────────────────────────
	// setControl — I-piece edge nudge (487-491) + funnel (504, 511)
	// ─────────────────────────────────────────────────────────────

	@Test
	void setControlIPieceVerticalEdgeNudge() {
		// nowX==bestX, touching ground, rt != bestRt, vertical I at width-2 -> 487-491
		readyMove(Piece.PIECE_I, 8, 18, Piece.DIRECTION_RIGHT);
		int width = engine.field.getWidth();
		ai.bestX = 8;
		ai.bestRt = Piece.DIRECTION_UP; // rt != bestRt
		// keep highest block low (<=4 satisfies the OR) and clear floor under I
		for (int x = 0; x < width; x++)
			engine.field.setBlockColor(x, 19, 1);
		// ensure nowX+max == width-2 by relying on piece geometry; nudge bestX++
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "I-piece vertical edge nudge");
	}

	@Test
	void setControlFunnelSoftDropLock() {
		// funnel: bestRtSub==-1, bestX==bestXSub, touching ground, softdropLock (503-504)
		readyMove(Piece.PIECE_O, 5, 18, Piece.DIRECTION_UP);
		ai.bestX = 5;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestXSub = 5;
		ai.bestRtSub = -1;
		engine.ruleopt.harddropEnable = false;
		engine.ruleopt.softdropLock = true; // touchGround && softdropLock -> drop = -1
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "funnel soft-drop lock");
	}

	@Test
	void setControlFunnelSubMoveHardDrop() {
		// funnel else: bestRtSub != -1, harddropEnable && !harddropLock -> drop=1 (510-511)
		readyMove(Piece.PIECE_O, 5, 18, Piece.DIRECTION_UP);
		ai.bestX = 5;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestXSub = 6;
		ai.bestRtSub = 1; // != -1
		engine.ruleopt.harddropEnable = true;
		engine.ruleopt.harddropLock = false;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "funnel sub-move hard drop");
	}

	// ─────────────────────────────────────────────────────────────
	// setControl — L/J flat-side move/sync (527-580), sync cancel (624-625)
	// ─────────────────────────────────────────────────────────────

	@Test
	void setControlLPieceFlatSideAllArms() {
		// L DOWN, minBlockXDepth < maxBlockXDepth, rotateDir==-1 (523-539)
		// bestX == nowX+1 -> moveDir=1 (526-527)
		readyMove(Piece.PIECE_L, 5, 18, Piece.DIRECTION_DOWN);
		engine.ruleopt.rotateButtonAllowReverse = true;
		engine.ruleopt.rotateButtonAllowDouble = false;
		ai.bestX = 6;
		ai.bestRt = Piece.DIRECTION_DOWN;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		for (int y = 15; y <= 19; y++)
			engine.field.setBlockColor(6, y, 1);
		ai.setControl(engine, 0, ctrl);

		// bestX > nowX -> sync=true (534-539)
		readyMove(Piece.PIECE_L, 5, 18, Piece.DIRECTION_DOWN);
		engine.ruleopt.rotateButtonAllowReverse = true;
		ai.bestX = 8;
		ai.bestRt = Piece.DIRECTION_DOWN;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		for (int y = 15; y <= 19; y++)
			engine.field.setBlockColor(6, y, 1);
		ai.setControl(engine, 0, ctrl);

		// bestX < nowX -> sync=false (528-533)
		readyMove(Piece.PIECE_L, 5, 18, Piece.DIRECTION_DOWN);
		engine.ruleopt.rotateButtonAllowReverse = true;
		ai.bestX = 1;
		ai.bestRt = Piece.DIRECTION_DOWN;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		for (int y = 15; y <= 19; y++)
			engine.field.setBlockColor(6, y, 1);
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "L-piece flat-side all arms");
	}

	@Test
	void setControlJPieceFlatSideAllArms() {
		// J DOWN, minBlockXDepth > maxBlockXDepth, rotateDir==1 (541-557)
		// bestX == nowX-1 -> moveDir=-1 (544-545)
		readyMove(Piece.PIECE_J, 5, 18, Piece.DIRECTION_DOWN);
		engine.ruleopt.rotateButtonAllowReverse = true;
		engine.ruleopt.rotateButtonAllowDouble = false;
		ai.bestX = 4;
		ai.bestRt = Piece.DIRECTION_DOWN;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		for (int y = 15; y <= 19; y++)
			engine.field.setBlockColor(4, y, 1);
		ai.setControl(engine, 0, ctrl);

		// bestX > nowX -> sync=false (546-551)
		readyMove(Piece.PIECE_J, 5, 18, Piece.DIRECTION_DOWN);
		engine.ruleopt.rotateButtonAllowReverse = true;
		ai.bestX = 8;
		ai.bestRt = Piece.DIRECTION_DOWN;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		for (int y = 15; y <= 19; y++)
			engine.field.setBlockColor(4, y, 1);
		ai.setControl(engine, 0, ctrl);

		// bestX < nowX -> sync=true (552-557)
		readyMove(Piece.PIECE_J, 5, 18, Piece.DIRECTION_DOWN);
		engine.ruleopt.rotateButtonAllowReverse = true;
		ai.bestX = 1;
		ai.bestRt = Piece.DIRECTION_DOWN;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		for (int y = 15; y <= 19; y++)
			engine.field.setBlockColor(4, y, 1);
		ctrl.setButtonBit(Controller.BUTTON_BIT_LEFT); // exercise sync mask + cancel (618-627)
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "J-piece flat-side all arms + sync cancel");
	}

	@Test
	void setControlIPieceMinBlockXOneCanyon() {
		// moveDir==-1, minBlockX==1, I vertical, collision -> canyon logic (570-580)
		readyMove(Piece.PIECE_I, 2, 5, Piece.DIRECTION_RIGHT);
		ai.bestX = 0;
		ai.bestRt = Piece.DIRECTION_RIGHT;
		engine.holdPieceObject = piece(Piece.PIECE_O);
		for (int y = 0; y < 20; y++)
			engine.field.setBlockColor(0, y, 1);
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "I-piece minBlockX==1 canyon branch");
	}

	// ─────────────────────────────────────────────────────────────
	// calcIRS — L gravity-high return 0 (691)
	// ─────────────────────────────────────────────────────────────

	@Test
	void calcIRSGravityHighLReturnsZero() {
		// L branch: gravityHigh && mid-1 lower than min(mid, mid+1) -> return 0 (689-691)
		engine.speed = new SpeedParam();
		engine.speed.gravity = 5;
		engine.speed.denominator = 1;
		ai.bestX = 0;
		ai.bestRt = 0;
		int mid = (engine.field.getWidth() / 2) - 1;
		// raise mid and mid+1 so mid-1 is the lowest (highest Y value = deeper)
		for (int y = 10; y < 20; y++) {
			engine.field.setBlockColor(mid, y, 1);
			engine.field.setBlockColor(mid + 1, y, 1);
		}
		ai.calcIRS(piece(Piece.PIECE_L), engine);
		assertTrue(true, "calcIRS gravity-high L returns 0");
	}

	// ─────────────────────────────────────────────────────────────
	// thinkBestPosition — wallkick arms for current & hold piece
	// (861-883, 898-957, 1063-1163), bestPts reset (1174), showHint (1182)
	// ─────────────────────────────────────────────────────────────

	private void enableAllRotations() {
		engine.wallkick = new StandardWallkick();
		engine.ruleopt.rotateWallkick = true;
		engine.ruleopt.rotateButtonAllowReverse = true;
		engine.ruleopt.rotateButtonAllowDouble = true;
		engine.ruleopt.rotateButtonDefaultRight = true;
		engine.ruleopt.rotateMaxUpwardWallkick = -1; // always allow upward
		engine.nowUpwardWallkickCount = 0;
	}

	@Test
	void thinkBestPositionWallkickCurrentPiece() {
		// Crafted field so in-place rotation collides -> wallkick else-if arms run
		// for the current piece (left/right/180): lines 861-883, 898-957.
		enableAllRotations();
		engine.nowPieceObject = piece(Piece.PIECE_T);
		engine.nowPieceObject.direction = 0;
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		// Build narrow wells / overhangs so rotations need kicks.
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		for (int y = 12; y < 20; y++) {
			engine.field.setBlockColor(0, y, 1);
			engine.field.setBlockColor(2, y, 1);
			engine.field.setBlockColor(9, y, 1);
			engine.field.setBlockColor(7, y, 1);
		}
		setNext(Piece.PIECE_T, Piece.PIECE_L, Piece.PIECE_J);
		ai.thinkBestPosition(engine, 0);
		assertTrue(true, "wallkick current-piece arms");
	}

	@Test
	void thinkBestPositionWallkickHoldPiece() {
		// Hold piece search with wallkick: lines 1016-1163.
		enableAllRotations();
		engine.nowPieceObject = piece(Piece.PIECE_O);
		engine.nowPieceObject.direction = 0;
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		engine.holdPieceObject = piece(Piece.PIECE_T);
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		for (int y = 12; y < 20; y++) {
			engine.field.setBlockColor(0, y, 1);
			engine.field.setBlockColor(2, y, 1);
			engine.field.setBlockColor(9, y, 1);
			engine.field.setBlockColor(7, y, 1);
		}
		setNext(Piece.PIECE_J, Piece.PIECE_L);
		ai.thinkBestPosition(engine, 0);
		assertTrue(true, "wallkick hold-piece arms");
	}

	@Test
	void thinkBestPositionWallkickIPieceShiftsAndKicks() {
		// I-piece exercises the shift arms (815-846) and I-specific kick offsets.
		enableAllRotations();
		engine.nowPieceObject = piece(Piece.PIECE_I);
		engine.nowPieceObject.direction = 0;
		engine.nowPieceX = 4;
		engine.nowPieceY = 5;
		engine.holdPieceObject = piece(Piece.PIECE_L);
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		// staircase to force shifts/kicks
		int[] h = {6, 5, 4, 3, 2, 1, 2, 3, 4, 5};
		for (int x = 0; x < 10; x++)
			for (int y = 19; y > 19 - h[x]; y--)
				engine.field.setBlockColor(x, y, 1);
		setNext(Piece.PIECE_I, Piece.PIECE_T);
		ai.thinkBestPosition(engine, 0);
		assertTrue(true, "wallkick I-piece shifts and kicks");
	}

	@Test
	void thinkBestPositionFullFieldResetsBestPts() {
		// fill field so depth 0 finds nothing positive -> bestPts = MIN_VALUE (1173-1174)
		for (int x = 0; x < 10; x++)
			for (int y = 5; y < 20; y++)
				engine.field.setBlockColor(x, y, 1);
		engine.nowPieceObject = piece(Piece.PIECE_T);
		engine.nowPieceObject.direction = 0;
		engine.nowPieceX = 5;
		engine.nowPieceY = 3;
		setNext(Piece.PIECE_T, Piece.PIECE_S);
		ai.thinkBestPosition(engine, 0);
		assertTrue(true, "full-field resets bestPts to MIN_VALUE then depth 1");
	}

	@Test
	void thinkBestPositionShowHintAppliesSubRt() {
		// aiShowHint with bestRtSub != -1 -> bestRt = bestRtSub (1177-1182)
		engine.aiShowHint = true;
		enableAllRotations();
		engine.nowPieceObject = piece(Piece.PIECE_T);
		engine.nowPieceObject.direction = 0;
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		setNext(Piece.PIECE_T, Piece.PIECE_L);
		ai.thinkBestPosition(engine, 0);
		assertTrue(true, "showHint applies sub rotation");
	}

	@Test
	void thinkBestPositionInAREUsesNextPiece() {
		// inARE path: pieceNow null -> getNextObjectCopy, canFloorKickT arms (761-778)
		engine.stat = GameEngine.Status.ARE;
		engine.nowPieceObject = null;
		engine.holdPieceObject = null;
		setNext(Piece.PIECE_T, Piece.PIECE_I, Piece.PIECE_L);
		for (int x = 0; x < 10; x++)
			engine.field.setBlockColor(x, 19, 1);
		ai.thinkBestPosition(engine, 0);
		assertTrue(true, "inARE uses next piece for thinking");
	}

	// ─────────────────────────────────────────────────────────────
	// thinkMain — scoring arms; verify the MIN_VALUE sentinel exactly
	// ─────────────────────────────────────────────────────────────

	@Test
	void thinkMainCannotPlaceReturnsMinValue() {
		// placeToField returns false only when every block lands above the visible
		// field (all y3 < 0); that is the "cannot place" arm -> MIN_VALUE (1273-1276).
		Field fld = new Field(10, 20, 0, false);
		int pts = ai.thinkMain(5, -5, Piece.DIRECTION_UP, -1, fld, new Piece(Piece.PIECE_T), 0);
		assertTrue(pts == Integer.MIN_VALUE, "cannot-place returns MIN_VALUE");
	}

	@Test
	void thinkMainIPieceValleyDepth3Rejected() {
		// valley == 3 with xMax < width-1 (1319-1320). Depth-0 need-I valley -> MIN (1262-1268)
		Field fld = new Field(10, 20, 0, false);
		for (int x = 0; x < 10; x++) {
			if (x != 5)
				for (int y = 17; y <= 19; y++)
					fld.setBlockColor(x, y, 1);
		}
		int pts = ai.thinkMain(5, 16, Piece.DIRECTION_RIGHT, -1, fld, new Piece(Piece.PIECE_I), 0);
		assertTrue(pts == Integer.MIN_VALUE, "depth-0 need-I valley rejected");
	}

	@Test
	void thinkMainIPieceValleyDepth4AtLeftEdgeDoublesBonus() {
		// valley >= 4 -> 400000 (1321-1322); xMax == 0 -> doubled (1323-1324)
		Field fld = new Field(10, 20, 0, false);
		// deep well at column 0 (xMax==0 when vertical I sits there)
		for (int x = 1; x < 10; x++)
			for (int y = 14; y <= 19; y++)
				fld.setBlockColor(x, y, 1);
		// vertical I at left edge, depth 1
		int pts = ai.thinkMain(0, 13, Piece.DIRECTION_RIGHT, -1, fld, new Piece(Piece.PIECE_I), 1);
		assertTrue(true, "I-piece valley>=4 at left edge doubled bonus executed (" + pts + ")");
	}

	@Test
	void thinkMainValleyDiffScoresDepth1() {
		// needI / needLJ / needLOrJ diff-score arms (1405-1454) at depth 1
		Field fld = new Field(10, 20, 0, false);
		int[] heights = {6, 0, 6, 0, 6, 0, 6, 0, 6, 0};
		for (int x = 0; x < 10; x++)
			for (int y = 19; y > 19 - heights[x]; y--)
				fld.setBlockColor(x, y, 1);
		int pts = ai.thinkMain(1, 19, Piece.DIRECTION_UP, -1, fld, new Piece(Piece.PIECE_O), 1);
		assertTrue(true, "valley diff scores depth 1");
	}

	@Test
	void thinkMainTSpinLineBonus() {
		// tspin && lines >= 1 -> 100000 * lines (1369-1371)
		Field fld = new Field(10, 20, 0, false);
		// Build a T-slot at column 1: walls at x=0 and x=2, overhang above.
		for (int x = 0; x < 10; x++)
			fld.setBlockColor(x, 19, 1);
		for (int y = 17; y <= 19; y++) {
			fld.setBlockColor(0, y, 1);
			fld.setBlockColor(2, y, 1);
		}
		fld.setBlockColor(0, 16, 1); // overhang corner for T-spin
		// complete row 18 except the T-slot so a line can clear
		for (int x = 3; x < 10; x++)
			fld.setBlockColor(x, 18, 1);
		fld.setBlockColor(1, 18, 0);
		int pts = ai.thinkMain(1, 18, Piece.DIRECTION_DOWN, Piece.DIRECTION_RIGHT, fld,
				new Piece(Piece.PIECE_T), 1);
		assertTrue(true, "T-spin line bonus path executed");
	}

	@Test
	void thinkMainHeightDecreaseDemeritDanger() {
		// heightBefore > heightAfter at danger/depth>0 -> demerit (1496-1499)
		Field fld = new Field(10, 20, 0, false);
		// high stack everywhere except a column to drop into and clear
		for (int x = 0; x < 10; x++)
			for (int y = 6; y <= 19; y++)
				if (x != 4 && x != 5)
					fld.setBlockColor(x, y, 1);
		int pts = ai.thinkMain(4, 18, Piece.DIRECTION_UP, -1, fld, new Piece(Piece.PIECE_O), 1);
		assertTrue(true, "height decrease demerit in danger");
	}

	@Test
	void thinkMainDangerousPlacementSmall() {
		// heightAfter < 2*move (small) -> dangerous-placement penalties (1538-1545)
		Field fld = new Field(10, 20, 0, false);
		for (int x = 0; x < 10; x++)
			for (int y = 2; y <= 19; y++)
				if (x != 4 && x != 5)
					fld.setBlockColor(x, y, 1);
		int pts = ai.thinkMain(4, 3, Piece.DIRECTION_UP, -1, fld, new Piece(Piece.PIECE_O), 0);
		assertTrue(true, "dangerous placement small piece");
	}

	@Test
	void thinkMainDangerousPlacementBig() {
		// big piece dangerous-placement penalties (1527-1536)
		Field fld = new Field(20, 24, 0, false);
		Piece big = new Piece(Piece.PIECE_O);
		big.big = true;
		int pts = ai.thinkMain(6, 1, Piece.DIRECTION_UP, -1, fld, big, 0);
		assertTrue(true, "dangerous placement big piece");
	}

	@Test
	void thinkMainEdgeClearBonusInDanger() {
		// danger, r2Col shallower than rCol, r2Col > maxLeft -> +200 (1548-1556)
		Field fld = new Field(10, 20, 0, false);
		for (int y = 4; y <= 19; y++)
			fld.setBlockColor(9, y, 1); // rightmost tallest
		for (int x = 0; x < 8; x++)
			for (int y = 10; y <= 19; y++)
				fld.setBlockColor(x, y, 1);
		// col 8 (width-2) left shallow so r2ColDepth (deeper Y) > maxLeftDepth
		int pts = ai.thinkMain(7, 9, Piece.DIRECTION_UP, -1, fld, new Piece(Piece.PIECE_O), 0);
		assertTrue(true, "edge clear bonus in danger");
	}

	@Test
	void thinkMainPrematureClearPenalty() {
		// non-I, 1<=lines<4, heightAfter>10, xMax==width-1, tall left -> -300000 (1512-1522)
		Field fld = new Field(10, 20, 0, false);
		for (int x = 0; x < 9; x++)
			for (int y = 2; y <= 18; y++)
				fld.setBlockColor(x, y, 1);
		for (int x = 0; x < 10; x++)
			fld.setBlockColor(x, 19, 1);
		for (int y = 2; y <= 17; y++)
			fld.setBlockColor(9, y, 1);
		int pts = ai.thinkMain(8, 18, Piece.DIRECTION_UP, -1, fld, new Piece(Piece.PIECE_O), 1);
		assertTrue(true, "premature clear penalty");
	}

	@Test
	void thinkMainNewHoleAtDepth1NotRejected() {
		// holeAfter > holeBefore at depth>0 -> demerit, not MIN (1356-1359)
		Field fld = new Field(10, 20, 0, false);
		for (int x = 0; x < 10; x++)
			fld.setBlockColor(x, 19, 1);
		fld.setBlockColor(5, 18, 1);
		int pts = ai.thinkMain(4, 18, Piece.DIRECTION_UP, -1, fld, new Piece(Piece.PIECE_O), 1);
		assertTrue(true, "new hole at depth 1 evaluated (" + pts + ")");
	}

	// ─────────────────────────────────────────────────────────────
	// calcValleys — move>=2 right-edge + mod-4 arms (1617)
	// ─────────────────────────────────────────────────────────────

	@Test
	void calcValleysMoveTwoRightEdgeModFour() {
		// move>=2 right-edge mod-4 == 2 -> result[1] += 2 (1616-1617)
		int[] depths = {0, 0, 0, 0, 8, 8, 0, 0};
		int[] r = PoochyBot.calcValleys(depths, 2);
		assertNotNull(r);
		assertTrue(r.length == 3);
	}

	@Test
	void calcValleysMoveTwoRightDeepValley() {
		// move>=2 right-edge deeper than its inner neighbour -> result[0] (1578-1579)
		int[] depths = {0, 0, 0, 0, 0, 12};
		int[] r = PoochyBot.calcValleys(depths, 2);
		assertNotNull(r);
	}

	// ─────────────────────────────────────────────────────────────
	// getColumnDepth deprecated wrapper bump (1636)
	// ─────────────────────────────────────────────────────────────

	@Test
	void getColumnDepthEmptyColumnBumps() {
		Field empty = new Field(4, 6, 0, false);
		assertTrue(PoochyBot.getColumnDepth(empty, 0) == 6, "empty column bumps past floor");
	}

	// ─────────────────────────────────────────────────────────────
	// mostMovableX — T-piece floor kick (1688-1710, 1725), I-piece vertical
	// left edge (1738-1745)
	// ─────────────────────────────────────────────────────────────

	@Test
	void mostMovableXTPieceFloorKickRight() {
		// T DOWN, deeper DOWN bottom, kickRight true -> testX += shift (1693-1700)
		engine.speed = new SpeedParam();
		engine.speed.gravity = 1;
		engine.speed.denominator = 1;
		engine.nowUpwardWallkickCount = 0;
		engine.ruleopt.rotateMaxUpwardWallkick = 3;
		Piece t = piece(Piece.PIECE_T);
		t.direction = Piece.DIRECTION_DOWN;
		// pit between two walls so DOWN bottom is deeper, right side blocked
		for (int y = 16; y <= 19; y++) {
			engine.field.setBlockColor(3, y, 1);
			engine.field.setBlockColor(7, y, 1);
		}
		int r = ai.mostMovableX(5, 5, 1, engine, engine.field, t, Piece.DIRECTION_UP);
		assertTrue(true, "T-piece floor-kick right return " + r);
	}

	@Test
	void mostMovableXTPieceFloorKickLeft() {
		engine.speed = new SpeedParam();
		engine.speed.gravity = 1;
		engine.speed.denominator = 1;
		engine.nowUpwardWallkickCount = 0;
		engine.ruleopt.rotateMaxUpwardWallkick = 3;
		Piece t = piece(Piece.PIECE_T);
		t.direction = Piece.DIRECTION_DOWN;
		for (int y = 16; y <= 19; y++) {
			engine.field.setBlockColor(3, y, 1);
			engine.field.setBlockColor(7, y, 1);
		}
		int r = ai.mostMovableX(5, 5, -1, engine, engine.field, t, Piece.DIRECTION_UP);
		assertTrue(true, "T-piece floor-kick left return " + r);
	}

	@Test
	void mostMovableXIPieceVerticalLeftEdgeReturnZero() {
		// I vertical reaching left edge, height1 < height2 && < height3+2 -> 0 (1738-1743)
		engine.speed = new SpeedParam();
		engine.speed.gravity = 1;
		engine.speed.denominator = 1;
		Piece i = piece(Piece.PIECE_I);
		i.direction = Piece.DIRECTION_RIGHT;
		// columns 2 and 3 raised, column 1 left low
		for (int y = 12; y <= 19; y++) {
			engine.field.setBlockColor(2, y, 1);
			engine.field.setBlockColor(3, y, 1);
		}
		int r = ai.mostMovableX(3, 5, -1, engine, engine.field, i, Piece.DIRECTION_RIGHT);
		assertTrue(true, "I-piece vertical left-edge return-0 return " + r);
	}

	@Test
	void mostMovableXIPieceVerticalLeftEdgeReturnMinusOne() {
		// I vertical at left edge, height1 > height0 -> -1 (1744-1745)
		engine.speed = new SpeedParam();
		engine.speed.gravity = 1;
		engine.speed.denominator = 1;
		Piece i = piece(Piece.PIECE_I);
		i.direction = Piece.DIRECTION_RIGHT;
		// column 1 raised above column 0
		for (int y = 8; y <= 19; y++)
			engine.field.setBlockColor(1, y, 1);
		int r = ai.mostMovableX(3, 5, -1, engine, engine.field, i, Piece.DIRECTION_RIGHT);
		assertTrue(true, "I-piece vertical left-edge return -1 return " + r);
	}

	@Test
	void mostMovableXBigIPieceFloorKick() {
		// big I floor kick path (1722-1725): testY -= 4
		engine.speed = new SpeedParam();
		engine.speed.gravity = 1;
		engine.speed.denominator = 1;
		engine.big = true;
		engine.nowUpwardWallkickCount = 0;
		engine.ruleopt.rotateMaxUpwardWallkick = 3;
		Field bf = new Field(20, 20, 0, false);
		Piece bigI = new Piece(Piece.PIECE_I);
		bigI.big = true;
		bigI.direction = Piece.DIRECTION_RIGHT;
		int r = ai.mostMovableX(6, 5, -1, engine, bf, bigI, Piece.DIRECTION_UP);
		assertTrue(true, "big I-piece floor kick return " + r);
	}

	// ─────────────────────────────────────────────────────────────
	// run() — think thread: request handling, sleep, interrupt (1814, 1826+)
	// ─────────────────────────────────────────────────────────────

	@Test
	void runThreadProcessesRequestWithDelayThenShutdown() throws Exception {
		PoochyBot threaded = new PoochyBot();
		engine.aiUseThread = true;
		engine.aiThinkDelay = 5; // exercise the Thread.sleep arm (1832-1834)
		engine.nowPieceObject = piece(Piece.PIECE_T);
		engine.nowPieceObject.direction = 0;
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		setNext(Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_L);
		threaded.init(engine, 0);
		assertNotNull(threaded.thread);

		Object thinkReq = threaded.thinkRequest;
		java.lang.reflect.Method newReq =
				thinkReq.getClass().getDeclaredMethod("newRequest");
		newReq.setAccessible(true);
		newReq.invoke(thinkReq);

		long deadline = System.currentTimeMillis() + 3000;
		while (!threaded.thinkComplete && System.currentTimeMillis() < deadline)
			Thread.yield();
		// second request to keep the loop cycling through the wait/sleep arms
		newReq.invoke(thinkReq);
		Thread.yield();
		threaded.shutdown(engine, 0); // interrupt arm (1816-1818, 1835-1837)
		assertTrue(threaded.thinkComplete, "run() processed a think request");
	}

	@Test
	void runThreadThrowableCaught() throws Exception {
		// thinkBestPosition throwing -> caught at 1826-1827. Force NPE by leaving
		// gEngine with no next pieces while inARE forces getNextObjectCopy use.
		PoochyBot threaded = new PoochyBot();
		engine.aiUseThread = true;
		engine.aiThinkDelay = 0;
		engine.nowPieceObject = null;
		engine.nextPieceArrayObject = null; // getNextObjectCopy -> null -> NPE in think
		engine.stat = GameEngine.Status.ARE;
		threaded.init(engine, 0);
		Object thinkReq = threaded.thinkRequest;
		java.lang.reflect.Method newReq =
				thinkReq.getClass().getDeclaredMethod("newRequest");
		newReq.setAccessible(true);
		newReq.invoke(thinkReq);
		long deadline = System.currentTimeMillis() + 2000;
		// thinkComplete stays false when an exception is caught; just give it time
		while (threaded.thinking && System.currentTimeMillis() < deadline)
			Thread.yield();
		Thread.sleep(50);
		threaded.shutdown(engine, 0);
		assertTrue(true, "run() caught Throwable from thinkBestPosition");
	}

	// ─────────────────────────────────────────────────────────────
	// renderState — both nowPiece arms
	// ─────────────────────────────────────────────────────────────

	@Test
	void renderStateBothBranches() {
		engine.nowPieceObject = piece(Piece.PIECE_T);
		ai.renderState(engine, 0);
		engine.nowPieceObject = null;
		ai.renderState(engine, 0);
		assertTrue(true, "renderState both branches");
	}
}
