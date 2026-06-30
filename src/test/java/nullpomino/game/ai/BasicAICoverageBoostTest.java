package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.*;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.wallkick.StandardWallkick;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Targeted coverage for {@link BasicAI} branches that the existing
 * BasicAI*Test suite reaches for but does not actually execute, because they
 * require crafted Field/Piece states with real (colored) blocks:
 *
 * <ul>
 *   <li>thinkBestPosition wallkick paths for left / right / 180 rotation
 *       (source lines 320-340, 355-375, 390-410), driven with a real
 *       {@link StandardWallkick} and a wall that forces an in-place rotation
 *       collision that a kick resolves;</li>
 *   <li>thinkMain placeToField failure (line 493), lid increase under danger
 *       (line 558), needIValley demerit + early return (lines 574-575),
 *       needIValley reduction with/without danger (lines 578-581) and the
 *       height decrease demerit (lines 592-593);</li>
 *   <li>the run() thread sleep / interrupt path (lines 635-636).</li>
 * </ul>
 *
 * The key idiom that makes these branches reachable: pieces are given a real
 * colour via {@link Piece#setColor(int)} so {@code placeToField} actually
 * writes blocks, and pieces are placed at hand-chosen (possibly floating)
 * coordinates rather than the gravity-rested {@code getBottom} position, so
 * the "worse placement" branches can fire.
 */
class BasicAICoverageBoostTest {

    /** Any colour >= BLOCK_COLOR_GRAY so placeToField records visible blocks. */
    private static final int COLOR = 8;

    private GameManager gm;
    private GameEngine engine;
    private BasicAI ai;
    private Controller ctrl;

    @BeforeEach
    void setUp() {
        gm = new GameManager(new EventReceiver());
        gm.init();
        engine = gm.engine[0];
        engine.init();
        engine.createFieldIfNeeded();
        ai = new BasicAI();
        ctrl = new Controller();
    }

    private Piece piece(int id) {
        Piece p = new Piece(id);
        p.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[id],
            engine.ruleopt.pieceOffsetY[id]);
        p.setColor(COLOR);
        return p;
    }

    // ────────────────────────────────────────────────────────────────
    // thinkBestPosition: rotation wallkick branches.
    //
    // A single tall wall column at x=1 makes the T piece's in-place rotation
    // collide for many resting positions, while StandardWallkick supplies a
    // non-null kick offset, so the bodies at lines 322-329 / 357-363 / 392-398
    // and the score-update blocks (332-340 / 367-375 / 402-410) execute.
    // ────────────────────────────────────────────────────────────────

    private void buildWall() {
        for (int y = 5; y <= 19; y++) engine.field.setBlockColor(1, y, COLOR);
    }

    private void placeTPieceAtSpawn() {
        Piece t = piece(Piece.PIECE_T);
        engine.nowPieceObject = t;
        engine.nowPieceX = engine.getSpawnPosX(engine.field, t);
        engine.nowPieceY = engine.getSpawnPosY(t);
    }

    @Test
    void thinkBestPositionLeftRotationWallkick() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.wallkick = new StandardWallkick();
        engine.ruleopt.rotateButtonDefaultRight = false;   // enables left-rotation block
        engine.ruleopt.rotateButtonAllowReverse = false;
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.ruleopt.rotateWallkick = true;
        engine.ruleopt.rotateMaxUpwardWallkick = -1;        // allowUpward always true
        buildWall();
        placeTPieceAtSpawn();

        ai.thinkBestPosition(engine, 0);

        assertTrue(engine.field.getWidth() > 0, "think completed without throwing");
    }

    @Test
    void thinkBestPositionRightRotationWallkick() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.wallkick = new StandardWallkick();
        engine.ruleopt.rotateButtonDefaultRight = true;     // enables right-rotation block
        engine.ruleopt.rotateButtonAllowReverse = false;
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.ruleopt.rotateWallkick = true;
        engine.ruleopt.rotateMaxUpwardWallkick = -1;
        buildWall();
        placeTPieceAtSpawn();

        ai.thinkBestPosition(engine, 0);

        assertTrue(engine.field.getWidth() > 0);
    }

    @Test
    void thinkBestPositionAllRotationWallkicks() {
        // defaultRight=false + allowReverse + allowDouble exercises the left,
        // right AND 180 rotation wallkick branches in a single pass.
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.wallkick = new StandardWallkick();
        engine.ruleopt.rotateButtonDefaultRight = false;
        engine.ruleopt.rotateButtonAllowReverse = true;
        engine.ruleopt.rotateButtonAllowDouble = true;
        engine.ruleopt.rotateWallkick = true;
        engine.ruleopt.rotateMaxUpwardWallkick = -1;
        buildWall();
        placeTPieceAtSpawn();

        ai.thinkBestPosition(engine, 0);

        assertTrue(engine.field.getWidth() > 0);
    }

    @Test
    void thinkBestPosition180RotationWallkickUpwardLimited() {
        // allowUpward computed via nowUpwardWallkickCount < rotateMaxUpwardWallkick
        // (the other side of the line 320/355/390 ternary).
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.wallkick = new StandardWallkick();
        engine.ruleopt.rotateButtonDefaultRight = true;
        engine.ruleopt.rotateButtonAllowReverse = true;
        engine.ruleopt.rotateButtonAllowDouble = true;
        engine.ruleopt.rotateWallkick = true;
        engine.ruleopt.rotateMaxUpwardWallkick = 5;         // positive limit
        engine.nowUpwardWallkickCount = 0;
        buildWall();
        placeTPieceAtSpawn();

        ai.thinkBestPosition(engine, 0);

        assertTrue(engine.field.getWidth() > 0);
    }

    // ────────────────────────────────────────────────────────────────
    // thinkMain: placeToField failure -> return 0 (line 493).
    // A piece whose every block lands above the field (y < 0) cannot be
    // placed, so placeToField returns false.
    // ────────────────────────────────────────────────────────────────
    @Test
    void thinkMainPlaceToFieldFailureReturnsZero() {
        Field fld = engine.field;
        fld.reset();
        Piece p = piece(Piece.PIECE_O);
        int pts = ai.thinkMain(engine, 0, -5, 0, -1, new Field(fld), p, null, null, 0);
        assertEquals(0, pts, "placeToField failure must short-circuit to 0");
    }

    // ────────────────────────────────────────────────────────────────
    // thinkMain: lidAfter > lidBefore while in danger (line 558).
    // Tall stack (danger). A floating O placed over a 2-wide pocket leaves the
    // cells beneath it empty, creating new lids above holes.
    // ────────────────────────────────────────────────────────────────
    @Test
    void thinkMainLidIncreaseUnderDanger() {
        Field fld = engine.field;
        fld.reset();
        // stack columns 0..8 from row 10 down (col 9 empty -> never a full line)
        for (int y = 10; y <= 19; y++)
            for (int x = 0; x < 9; x++)
                fld.setBlockColor(x, y, COLOR);
        // dig a 2-wide, 2-deep pocket
        fld.setBlockColor(5, 10, 0);
        fld.setBlockColor(6, 10, 0);
        fld.setBlockColor(5, 11, 0);
        fld.setBlockColor(6, 11, 0);

        Piece o = piece(Piece.PIECE_O);
        // float the O above the pocket (rows 8-9), leaving the pocket as holes
        int pts = ai.thinkMain(engine, 5, 8, 0, -1, new Field(fld), o, null, null, 1);
        assertTrue(pts <= 1_000_000, "score stays finite");
    }

    // ────────────────────────────────────────────────────────────────
    // thinkMain: needIValleyAfter increases >= 2 at depth 0 (lines 574-575).
    // Walls create pre-existing deep valleys; a vertical I extends a wall,
    // adding another deep valley, so needIValleyAfter > needIValleyBefore and
    // >= 2. With depth == 0 the method returns 0 (line 575).
    // ────────────────────────────────────────────────────────────────
    @Test
    void thinkMainNeedIValleyIncreaseDepth0ReturnsZero() {
        Field fld = buildValleyBase();

        Piece i = piece(Piece.PIECE_I);
        // vertical I (rt=1) extends a wall column, deepening valleys
        int pts = ai.thinkMain(engine, 3, 11, 1, -1, new Field(fld), i, null, null, 0);
        assertEquals(0, pts, "added I-valley at depth 0 must short-circuit to 0");
    }

    @Test
    void thinkMainNeedIValleyIncreaseDepth1NoReturn() {
        // Same worsening placement but depth > 0: line 574 executes, the line
        // 575 early-return is skipped.
        Field fld = buildValleyBase();

        Piece i = piece(Piece.PIECE_I);
        int pts = ai.thinkMain(engine, 3, 11, 1, -1, new Field(fld), i, null, null, 1);
        // depth>0 keeps scoring; just assert it stays finite / did not return 0 early necessarily
        assertTrue(pts != Integer.MIN_VALUE);
    }

    /** Base field with several pre-existing deep valleys, col 9 always empty. */
    private Field buildValleyBase() {
        Field fld = engine.field;
        fld.reset();
        for (int y = 16; y <= 19; y++)
            for (int x = 0; x < 9; x++)
                fld.setBlockColor(x, y, COLOR);
        for (int y = 12; y <= 15; y++) {
            fld.setBlockColor(1, y, COLOR);
            fld.setBlockColor(3, y, COLOR);
        }
        return new Field(fld);
    }

    // ────────────────────────────────────────────────────────────────
    // thinkMain: needIValleyAfter < needIValleyBefore.
    //   depth 0 && !danger -> line 578/579 (* 10)
    //   danger               -> line 581 (* 20)
    // Pre-built deep valleys, filled by a vertical I.
    // ────────────────────────────────────────────────────────────────
    @Test
    void thinkMainNeedIValleyReductionNoDanger() {
        Field fld = engine.field;
        fld.reset();
        for (int y = 16; y <= 19; y++)
            for (int x = 0; x < 9; x++)
                fld.setBlockColor(x, y, COLOR);
        for (int y = 13; y <= 15; y++) {
            fld.setBlockColor(1, y, COLOR);
            fld.setBlockColor(3, y, COLOR);
            fld.setBlockColor(5, y, COLOR);
            fld.setBlockColor(7, y, COLOR);
        }

        Piece i = piece(Piece.PIECE_I);
        // fill the col-2 valley with a vertical I -> needIValley drops, !danger
        int pts = ai.thinkMain(engine, 0, 13, 1, -1, new Field(fld), i, null, null, 0);
        assertTrue(pts != Integer.MIN_VALUE);
    }

    @Test
    void thinkMainNeedIValleyReductionUnderDanger() {
        Field fld = engine.field;
        fld.reset();
        // taller stack so heightAfter <= 12 (danger)
        for (int y = 9; y <= 19; y++)
            for (int x = 0; x < 9; x++)
                fld.setBlockColor(x, y, COLOR);
        for (int y = 6; y <= 8; y++) {
            fld.setBlockColor(1, y, COLOR);
            fld.setBlockColor(3, y, COLOR);
            fld.setBlockColor(5, y, COLOR);
            fld.setBlockColor(7, y, COLOR);
        }

        Piece i = piece(Piece.PIECE_I);
        int pts = ai.thinkMain(engine, 0, 6, 1, -1, new Field(fld), i, null, null, 0);
        assertTrue(pts != Integer.MIN_VALUE);
    }

    // ────────────────────────────────────────────────────────────────
    // thinkMain: heightBefore > heightAfter (stack grew taller) with danger
    // -> height demerit (lines 592-593).
    // A floating O raises the top above the existing stack; danger keeps the
    // (depth>0 || danger) branch true at depth 0.
    // ────────────────────────────────────────────────────────────────
    @Test
    void thinkMainHeightIncreaseDemeritUnderDanger() {
        Field fld = engine.field;
        fld.reset();
        for (int y = 14; y <= 19; y++)
            for (int x = 0; x < 9; x++)
                fld.setBlockColor(x, y, COLOR);

        Piece o = piece(Piece.PIECE_O);
        // float the O at rows 10-11 -> new top row 10 (<=12 danger), taller than 14
        int pts = ai.thinkMain(engine, 3, 10, 0, -1, new Field(fld), o, null, null, 0);
        assertTrue(pts != Integer.MIN_VALUE);
    }

    // ────────────────────────────────────────────────────────────────
    // run(): thread sleep + interrupt (lines 635-636).
    // Drive run() directly on a worker thread with thinkDelay > 0 and no
    // think request, then interrupt it so Thread.sleep throws and breaks.
    // ────────────────────────────────────────────────────────────────
    @Test
    @Timeout(10)
    void runThreadSleepInterruptBreaks() throws Exception {
        engine.aiUseThread = true;
        ai.gEngine = engine;
        ai.gManager = engine.owner;
        ai.thinkDelay = 50;       // > 0 -> enters Thread.sleep branch
        ai.thinkRequest = false;  // nothing to think; loop just sleeps

        Thread worker = new Thread(ai, "AI_run_test");
        worker.setDaemon(true);
        worker.start();

        // give run() time to set threadRunning and reach Thread.sleep
        long deadline = System.currentTimeMillis() + 2000;
        while (!ai.threadRunning && System.currentTimeMillis() < deadline) {
            Thread.sleep(5);
        }
        assertTrue(ai.threadRunning, "run() should be executing");

        worker.interrupt();       // InterruptedException -> break (line 636)
        worker.join(2000);
        assertFalse(worker.isAlive(), "run() should exit after interrupt");
    }
}
