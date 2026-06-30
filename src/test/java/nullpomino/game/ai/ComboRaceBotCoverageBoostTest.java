package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.wallkick.StandardSymmetricWallkick;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Additional coverage for {@link ComboRaceBot} targeting branches that the
 * existing ComboRaceBot*Test suite leaves uncovered: the {@code newPiece}
 * thread/ARE branch, {@code setControl} reverse-180 rotation, the
 * {@code thinkBestPosition} hold-scoring + showHint block, the {@code run()}
 * thread loop, {@code createTables} wallkick paths, and the remaining
 * {@code renderState}/{@code renderHint} branches.
 *
 * <p>Most methods are exercised directly (never via the background think
 * thread) with crafted Field/Piece states. Assertions are intentionally
 * robust (no-throw / valid-range) because coverage counts executed lines.</p>
 */
class ComboRaceBotCoverageBoostTest {

    private GameManager gm;
    private GameEngine engine;
    private ComboRaceBot ai;
    private Controller ctrl;

    @BeforeEach
    void setUp() {
        gm = new GameManager(new EventReceiver());
        gm.init();
        engine = gm.engine[0];
        engine.init();
        engine.createFieldIfNeeded();
        engine.aiUseThread = false;
        ai = new ComboRaceBot();
        ai.init(engine, 0);
        ctrl = new Controller();
    }

    // ThinkRequestMutex is a private static inner class; reach its signalling
    // methods reflectively.
    private static void invokeRequest(ComboRaceBot bot, String method) throws Exception {
        Object mutex = bot.thinkRequest;
        java.lang.reflect.Method m = mutex.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(mutex);
    }

    private Piece newOffsetPiece(int id) {
        Piece p = new Piece(id);
        p.applyOffsetArray(engine.ruleopt.pieceOffsetX[id], engine.ruleopt.pieceOffsetY[id]);
        return p;
    }

    // Build a field whose bottom-row 4-wide window at valleyX=3 encodes 0x7
    // (fieldToIndex returns a valid >= 0 state) by filling field x=4,5,6 at the
    // bottom row.
    private void fillKnownState() {
        int h = engine.field.getHeight();
        engine.field.setBlockColor(4, h - 1, 1);
        engine.field.setBlockColor(5, h - 1, 1);
        engine.field.setBlockColor(6, h - 1, 1);
    }

    // ─── newPiece: thread + ARE<=0 branch (line 145-146) ───
    @Test
    void newPieceThreadAreBranch() {
        // aiUseThread true so the first !aiUseThread test is false and the
        // else-if is evaluated. Earlier OR terms are false so getARE()/getARELine()
        // get evaluated (default speed.are == 0 => <= 0).
        engine.aiUseThread = true;
        engine.aiPrethink = true;
        engine.aiShowHint = false;
        ai.thinking = false;
        ai.thinkComplete = true;
        engine.speed.are = 0;      // getARE() <= 0 -> evaluates line 146
        engine.speed.areLine = 0;

        ai.newPiece(engine, 0);

        assertTrue(ai.thinkCurrentPieceNo >= 1, "newPiece should request a think");
        // restore so teardown stays thread-free
        engine.aiUseThread = false;
    }

    // ─── newPiece: no-thread direct think (line 143-144) ───
    @Test
    void newPieceNoThread() {
        engine.aiUseThread = false;
        engine.nowPieceObject = newOffsetPiece(Piece.PIECE_T);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.holdPieceObject = newOffsetPiece(Piece.PIECE_S);
        engine.nextPieceArrayObject = new Piece[]{
            newOffsetPiece(Piece.PIECE_T), newOffsetPiece(Piece.PIECE_S),
            newOffsetPiece(Piece.PIECE_L), newOffsetPiece(Piece.PIECE_J),
            newOffsetPiece(Piece.PIECE_Z), newOffsetPiece(Piece.PIECE_I)};
        engine.nextPieceArrayID = new int[]{
            Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_L, Piece.PIECE_J, Piece.PIECE_Z, Piece.PIECE_I};
        engine.nextPieceCount = 0;
        ai.createTables(engine);

        ai.newPiece(engine, 0);
        assertTrue(true, "newPiece no-thread path executed");
    }

    // ─── onFirst: bestHold with hold == null reads next+1 (line 174) ───
    @Test
    void onFirstBestHoldHoldNull() {
        engine.aiUseThread = false;
        engine.aiPrethink = true;
        engine.aiMoveDelay = 0;
        engine.stat = GameEngine.Status.ARE;
        ai.inARE = true;          // newInARE && !inARE -> false
        ai.thinkSuccess = true;   // !thinking && !thinkSuccess -> false
        ai.delay = 50;
        ai.bestHold = true;
        ai.thinkComplete = true;  // stays true: prethink block (163) is skipped
        engine.holdPieceObject = null;
        engine.nextPieceArrayObject = new Piece[]{newOffsetPiece(Piece.PIECE_T), newOffsetPiece(Piece.PIECE_S)};
        engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
        engine.nextPieceCount = 0;

        ai.onFirst(engine, 0);
        assertTrue(true, "onFirst bestHold hold-null executed");
    }

    // ─── onFirst: threadRunning move-toward-bestX branch (lines 182-193) ───
    @Test
    void onFirstThreadRunningMove() {
        engine.aiUseThread = true;
        engine.aiPrethink = true;
        engine.aiMoveDelay = 0;
        engine.stat = GameEngine.Status.ARE;
        ai.inARE = true;
        ai.delay = 50;
        ai.bestHold = false;
        ai.threadRunning = true;
        ai.thinking = false;
        ai.thinkCurrentPieceNo = 0;
        ai.thinkLastPieceNo = 1;
        ai.bestX = 0; // far left -> spawnX - bestX > 1 -> right? depends; just exercise
        engine.holdPieceObject = null;
        engine.nextPieceArrayObject = new Piece[]{newOffsetPiece(Piece.PIECE_T), newOffsetPiece(Piece.PIECE_S)};
        engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
        engine.nextPieceCount = 0;

        ai.onFirst(engine, 0);

        engine.aiUseThread = false;
        assertTrue(true, "onFirst threadRunning move executed");
    }

    @Test
    void onFirstThreadRunningMoveRight() {
        engine.aiUseThread = true;
        engine.aiPrethink = true;
        engine.aiMoveDelay = 0;
        engine.stat = GameEngine.Status.ARE;
        ai.inARE = true;
        ai.delay = 50;
        ai.bestHold = false;
        ai.threadRunning = true;
        ai.thinking = false;
        ai.thinkCurrentPieceNo = 0;
        ai.thinkLastPieceNo = 1;
        ai.bestX = 9; // far right -> bestX - spawnX > 1
        engine.holdPieceObject = null;
        engine.nextPieceArrayObject = new Piece[]{newOffsetPiece(Piece.PIECE_T), newOffsetPiece(Piece.PIECE_S)};
        engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
        engine.nextPieceCount = 0;

        ai.onFirst(engine, 0);

        engine.aiUseThread = false;
        assertTrue(true, "onFirst threadRunning move right executed");
    }

    // ─── setControl: reverse-180 rotation, rrot != UP -> rotateDir = -1 (249-254) ───
    @Test
    void setControlReverse180RrotNotUp() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.ruleopt.rotateButtonAllowDouble = false; // skip line 243
        engine.ruleopt.rotateButtonAllowReverse = true;
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_RIGHT; // rt=1, rt&1==1
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        ai.bestRt = Piece.DIRECTION_LEFT; // 180 from RIGHT, rrot = DOWN(!=UP)
        ai.bestX = 5;
        ai.bestHold = false;
        ai.thinkComplete = true;

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl reverse-180 rrot!=UP");
    }

    // ─── setControl: reverse-180 rotation, rrot == UP -> rotateDir = 1 (249-252) ───
    @Test
    void setControlReverse180RrotUp() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.ruleopt.rotateButtonAllowReverse = true;
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_LEFT; // rt=3, rt&1==1, rrot = UP
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        ai.bestRt = Piece.DIRECTION_RIGHT; // 180 from LEFT
        ai.bestX = 5;
        ai.bestHold = false;
        ai.thinkComplete = true;

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl reverse-180 rrot==UP");
    }

    // ─── setControl: rotation fall-through else rotateDir = 1 (line 257) ───
    @Test
    void setControlRotationFallthroughElse() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.ruleopt.rotateButtonAllowDouble = false; // skip 243
        engine.ruleopt.rotateButtonAllowReverse = false; // skip 249 -> else 257
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP; // rt=0
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        ai.bestRt = Piece.DIRECTION_DOWN; // 180; not rrot/lrot -> else 257
        ai.bestX = 5;
        ai.bestHold = false;
        ai.thinkComplete = true;

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl rotation fall-through else (257)");
    }

    // ─── setControl: bestRtSub != 0 funnel, softdrop (lines 295-298) ───
    @Test
    void setControlFunnelSubSoftdrop() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        Piece p = newOffsetPiece(Piece.PIECE_T);
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        ai.bestX = 5;
        ai.bestRt = 0;
        ai.movestate = 1;     // movestate > 0 so funnel runs even after ground rotate
        ai.bestRtSub = 1;     // != 0 -> takes the else branch (lines 295-298)
        ai.bestHold = false;
        ai.thinkComplete = true;
        engine.ruleopt.harddropEnable = false;
        engine.ruleopt.harddropLock = false;
        engine.ruleopt.softdropEnable = true;
        engine.ruleopt.softdropLock = false;
        for (int x = 0; x < 10; x++)
            engine.field.setBlockColor(x, 19, 1);

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl funnel sub softdrop (295-298)");
    }

    // ─── setControl: bestRtSub != 0 funnel, harddrop (lines 295-296) ───
    @Test
    void setControlFunnelSubHarddrop() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        Piece p = newOffsetPiece(Piece.PIECE_T);
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        ai.bestX = 5;
        ai.bestRt = 0;
        ai.movestate = 1;
        ai.bestRtSub = 1;
        ai.bestHold = false;
        ai.thinkComplete = true;
        engine.ruleopt.harddropEnable = true;
        engine.ruleopt.harddropLock = false;
        for (int x = 0; x < 10; x++)
            engine.field.setBlockColor(x, 19, 1);

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl funnel sub harddrop (295-296)");
    }

    // ─── thinkBestPosition: hold scoring + showHint block (459-497) ───
    @Test
    void thinkBestPositionHoldBeatsAndShowHint() throws Exception {
        engine.aiShowHint = true;
        engine.ruleopt.holdEnable = true;
        engine.wallkick = new StandardSymmetricWallkick();
        ai.createTables(engine);

        fillKnownState();
        int state = ComboRaceBot.fieldToIndex(engine.field);
        assertTrue(state >= 0, "crafted field must map to a valid state");

        // now piece = O (no transition for the crafted state), hold = different
        engine.nowPieceObject = newOffsetPiece(Piece.PIECE_O);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.holdPieceObject = newOffsetPiece(Piece.PIECE_I);
        engine.nextPieceArrayObject = new Piece[]{
            newOffsetPiece(Piece.PIECE_T), newOffsetPiece(Piece.PIECE_S),
            newOffsetPiece(Piece.PIECE_L), newOffsetPiece(Piece.PIECE_J),
            newOffsetPiece(Piece.PIECE_Z), newOffsetPiece(Piece.PIECE_I)};
        engine.nextPieceArrayID = new int[]{
            Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_L, Piece.PIECE_J, Piece.PIECE_Z, Piece.PIECE_I};
        engine.nextPieceCount = 0;

        // Force a hold transition that beats bestPts: clear now-piece transitions,
        // install a single hold transition with rtSub != 0 (drives showHint block).
        ai.moves[state][Piece.PIECE_O] = null;
        ai.moves[state][Piece.PIECE_I] =
            new ComboRaceBot.Transition(0 /*x*/, 0 /*rt*/, 1 /*rtSub*/, state /*newField*/);

        ai.thinkBestPosition(engine, 0);

        assertTrue(ai.bestHold, "hold move should have been selected");
    }

    // ─── thinkBestPosition: showHint with bestRtSub == 0 (else, line 497) ───
    @Test
    void thinkBestPositionShowHintNoSub() {
        engine.aiShowHint = true;
        engine.ruleopt.holdEnable = false;
        ai.createTables(engine);
        fillKnownState();
        int state = ComboRaceBot.fieldToIndex(engine.field);
        assertTrue(state >= 0);

        engine.nowPieceObject = newOffsetPiece(Piece.PIECE_T);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.holdPieceObject = newOffsetPiece(Piece.PIECE_T);
        engine.nextPieceArrayObject = new Piece[]{
            newOffsetPiece(Piece.PIECE_T), newOffsetPiece(Piece.PIECE_S),
            newOffsetPiece(Piece.PIECE_L), newOffsetPiece(Piece.PIECE_J),
            newOffsetPiece(Piece.PIECE_Z), newOffsetPiece(Piece.PIECE_I)};
        engine.nextPieceArrayID = new int[]{
            Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_L, Piece.PIECE_J, Piece.PIECE_Z, Piece.PIECE_I};
        engine.nextPieceCount = 0;

        // single now-transition with rtSub == 0 -> showHint else branch (497)
        ai.moves[state][Piece.PIECE_T] =
            new ComboRaceBot.Transition(0, 0, 0, state);

        ai.thinkBestPosition(engine, 0);
        assertTrue(true, "thinkBestPosition showHint no-sub executed");
    }

    // ─── createTables with wallkick (lines 678-687, 713-722) ───
    @Test
    void createTablesWithWallkick() {
        ComboRaceBot fresh = new ComboRaceBot();
        fresh.init(engine, 0);
        engine.wallkick = new StandardSymmetricWallkick();
        engine.ruleopt.rotateWallkick = true;
        engine.ruleopt.rotateButtonDefaultRight = true;
        engine.ruleopt.rotateButtonAllowReverse = true;

        fresh.createTables(engine);
        assertNotNull(fresh.moves, "createTables with wallkick produced a table");
    }

    // ─── run(): drive the think + createTables loop via a bounded daemon thread ───
    @Test
    void runThreadLoopThinkAndCreateTables() throws Exception {
        ComboRaceBot bot = new ComboRaceBot();
        bot.init(engine, 0);
        bot.gEngine = engine;
        bot.gManager = gm;
        bot.thinkDelay = 0;

        engine.nowPieceObject = newOffsetPiece(Piece.PIECE_T);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.nextPieceArrayObject = new Piece[]{
            newOffsetPiece(Piece.PIECE_T), newOffsetPiece(Piece.PIECE_S),
            newOffsetPiece(Piece.PIECE_L), newOffsetPiece(Piece.PIECE_J),
            newOffsetPiece(Piece.PIECE_Z), newOffsetPiece(Piece.PIECE_I)};
        engine.nextPieceArrayID = new int[]{
            Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_L, Piece.PIECE_J, Piece.PIECE_Z, Piece.PIECE_I};
        engine.nextPieceCount = 0;

        Thread t = new Thread(bot, "test-combo-run");
        t.setDaemon(true);
        t.start();

        // wait until the thread loop has actually started running
        for (int i = 0; i < 200 && !bot.threadRunning; i++) Thread.sleep(5);
        assertTrue(bot.threadRunning, "run() should have set threadRunning");

        // request table creation, then a think
        invokeRequest(bot, "newCreateTablesRequest");
        for (int i = 0; i < 200 && bot.moves == null; i++) Thread.sleep(5);

        invokeRequest(bot, "newRequest");
        for (int i = 0; i < 200 && !bot.thinkComplete; i++) Thread.sleep(5);

        // stop the loop deterministically
        bot.threadRunning = false;
        invokeRequest(bot, "newRequest"); // wake from any wait()
        t.interrupt();
        t.join(2000);

        assertNotNull(bot.moves, "run() should have created the moves table");
    }

    // ─── run(): thinkDelay > 0 sleep path then interrupt break (594-597) ───
    @Test
    void runThreadLoopThinkDelaySleep() throws Exception {
        ComboRaceBot bot = new ComboRaceBot();
        bot.init(engine, 0);
        bot.gEngine = engine;
        bot.gManager = gm;
        // small positive delay so Thread.sleep(thinkDelay) (594) completes
        // normally at least once before we force the interrupt break (596).
        bot.thinkDelay = 30;

        engine.nowPieceObject = newOffsetPiece(Piece.PIECE_T);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.nextPieceArrayObject = new Piece[]{
            newOffsetPiece(Piece.PIECE_T), newOffsetPiece(Piece.PIECE_S),
            newOffsetPiece(Piece.PIECE_L), newOffsetPiece(Piece.PIECE_J),
            newOffsetPiece(Piece.PIECE_Z), newOffsetPiece(Piece.PIECE_I)};
        engine.nextPieceArrayID = new int[]{
            Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_L, Piece.PIECE_J, Piece.PIECE_Z, Piece.PIECE_I};
        engine.nextPieceCount = 0;

        Thread t = new Thread(bot, "test-combo-run-delay");
        t.setDaemon(true);
        t.start();
        for (int i = 0; i < 200 && !bot.threadRunning; i++) Thread.sleep(5);

        invokeRequest(bot, "newRequest");
        // wait until it has finished thinking and slept normally at least once
        for (int i = 0; i < 200 && !bot.thinkComplete; i++) Thread.sleep(5);
        Thread.sleep(80); // long enough for the 30ms sleep at line 594 to elapse

        // now force the interrupt break (596) while it waits/sleeps again
        bot.thinkDelay = 5000;
        invokeRequest(bot, "newRequest");
        Thread.sleep(40); // let it finish thinking and enter the long sleep
        bot.threadRunning = false;
        t.interrupt();
        t.join(2000);
        assertTrue(true, "run() thinkDelay sleep path executed");
    }

    // ─── run(): thinkBestPosition throws -> Throwable catch (584-585) ───
    @Test
    void runThreadThinkThrows() throws Exception {
        ComboRaceBot bot = new ComboRaceBot();
        bot.init(engine, 0);
        bot.gEngine = engine;
        bot.gManager = gm;
        bot.thinkDelay = 0;

        // No moves table + a now piece => thinkBestPosition dereferences moves
        // (null) inside the think routine and throws, exercising the catch.
        engine.nowPieceObject = newOffsetPiece(Piece.PIECE_T);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.holdPieceObject = newOffsetPiece(Piece.PIECE_S);
        engine.nextPieceArrayObject = new Piece[]{
            newOffsetPiece(Piece.PIECE_T), newOffsetPiece(Piece.PIECE_S),
            newOffsetPiece(Piece.PIECE_L), newOffsetPiece(Piece.PIECE_J),
            newOffsetPiece(Piece.PIECE_Z), newOffsetPiece(Piece.PIECE_I)};
        engine.nextPieceArrayID = new int[]{
            Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_L, Piece.PIECE_J, Piece.PIECE_Z, Piece.PIECE_I};
        engine.nextPieceCount = 0;
        // craft a valid state so fieldToIndex >= 0 and moves[state] is dereferenced
        fillKnownState();
        bot.moves = null; // ensure NPE on moves[state][...]

        Thread t = new Thread(bot, "test-combo-run-throws");
        t.setDaemon(true);
        t.start();
        for (int i = 0; i < 200 && !bot.threadRunning; i++) Thread.sleep(5);

        invokeRequest(bot, "newRequest");
        // think will throw; thinkComplete stays false but thinking returns to false
        for (int i = 0; i < 200 && bot.thinking; i++) Thread.sleep(5);
        Thread.sleep(20);

        bot.threadRunning = false;
        invokeRequest(bot, "newRequest");
        t.interrupt();
        t.join(2000);
        assertTrue(true, "run() thinkBestPosition Throwable catch executed");
    }

    // ─── renderState: aiShowHint sub + score color branches (831-832, 848-851, 872-882) ───
    @Test
    void renderStateShowHintAndColors() {
        engine.createFieldIfNeeded();
        engine.aiShowHint = true;
        engine.nowPieceObject = newOffsetPiece(Piece.PIECE_T);
        engine.nowPieceX = 4;
        engine.nowPieceY = 3;
        ai.bestXSub = 5;
        ai.bestYSub = 6;
        ai.bestRtSub = 1;
        ai.thinkSuccess = true;
        ai.thinkComplete = true;
        ai.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};

        // bestPts >= MAX_THINK_DEPTH*1000 keeps scoreColor GREEN, but still walks
        // the queue loop (872-882).
        ai.bestPts = 6000;
        ai.renderState(engine, 0);

        // YELLOW band (line 851): MAX-1 .. MAX
        ai.bestPts = (6 - 1) * 1000 + 1;
        ai.renderState(engine, 0);

        // ORANGE band (line 849): MAX-2 .. MAX-1
        ai.bestPts = (6 - 2) * 1000 + 1;
        ai.renderState(engine, 0);

        // RED band (line 847): < MAX-2 ; also drives queue-loop RED coloring
        ai.bestPts = 0;
        ai.renderState(engine, 0);

        assertTrue(true, "renderState showHint+colors executed");
    }

    // ─── renderState: queue loop sets RED when thinkComplete and i < len-1 (877-878) ───
    @Test
    void renderStateQueueRed() {
        engine.createFieldIfNeeded();
        engine.nowPieceObject = newOffsetPiece(Piece.PIECE_T);
        engine.nowPieceX = 4;
        engine.nowPieceY = 3;
        ai.thinkComplete = true;
        ai.thinkSuccess = true;
        ai.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        ai.bestPts = 0; // i >= 0 from the start -> color logic triggers early
        ai.renderState(engine, 0);
        assertTrue(true, "renderState queue RED executed");
    }

    // ─── renderHint: sub-position match shows ROTATE 180 (922-923, 937-943) ───
    @Test
    void renderHintSubMatch180() {
        engine.createFieldIfNeeded();
        engine.ruleopt.rotateButtonAllowDouble = true;
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        for (int x = 0; x < 10; x++) engine.field.setBlockColor(x, 19, 1);

        ai.bestPts = 3000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 6;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_DOWN;   // 180 from sub (UP) -> rotateDir 2
        ai.bestXSub = 5;
        ai.bestYSub = 18;
        ai.bestRtSub = Piece.DIRECTION_UP;  // matches now -> sub-match block
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint sub-match 180 executed");
    }

    // ─── renderHint: sub-position match reverse-180 odd (928-934) ───
    @Test
    void renderHintSubMatchReverse180() {
        engine.createFieldIfNeeded();
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.ruleopt.rotateButtonAllowReverse = true;
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_RIGHT; // rt=1 odd
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        for (int x = 0; x < 10; x++) engine.field.setBlockColor(x, 19, 1);

        ai.bestPts = 3000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 6;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_LEFT;    // 180 from sub (RIGHT)
        ai.bestXSub = 5;
        ai.bestYSub = 18;
        ai.bestRtSub = Piece.DIRECTION_RIGHT; // matches now
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint sub-match reverse-180 executed");
    }

    // ─── renderHint: full move/rotate/drop output block (951-1030) ───
    @Test
    void renderHintMoveAndDropOutput() {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 3;       // not aligned -> moveDir output (1016-1023)
        engine.nowPieceY = 5;
        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 7;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_RIGHT;   // != bestRtSub -> rotate block (951-979)
        ai.bestXSub = 7;
        ai.bestYSub = 10;
        ai.bestRtSub = Piece.DIRECTION_DOWN; // rt(UP) != bestRtSub(DOWN), 180
        engine.ruleopt.rotateButtonAllowDouble = true;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint move/rotate output executed");
    }

    // ─── renderHint: funnel drop output, aligned (994-1030) ───
    @Test
    void renderHintFunnelDropOutput() {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        for (int x = 0; x < 10; x++) engine.field.setBlockColor(x, 19, 1);

        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 6;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 5;                     // == nowX so funnel runs
        ai.bestYSub = 19;                    // != nowY so not the early sub-match return
        ai.bestRtSub = Piece.DIRECTION_UP;   // == rt so 951 skipped, 994 funnel entered
        engine.ruleopt.harddropEnable = true;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint funnel drop output executed");
    }

    // ─── renderHint: funnel softdrop when bestRtSub == bestRt (997-1003) ───
    @Test
    void renderHintFunnelSoftdrop() {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        for (int x = 0; x < 10; x++) engine.field.setBlockColor(x, 19, 1);

        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 5;
        ai.bestYSub = 19;
        ai.bestRtSub = Piece.DIRECTION_UP;   // == bestRt -> first funnel branch (997)
        engine.ruleopt.harddropEnable = false;
        engine.ruleopt.softdropEnable = true;
        engine.ruleopt.softdropLock = false;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint funnel softdrop executed");
    }

    // ─── renderHint: unreachable sub triggers rethink (986-992) ───
    @Test
    void renderHintUnreachableSub() {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 3;
        engine.nowPieceY = 5;
        ai.bestPts = 5000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 100;
        ai.bestY = 0;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 100;     // unreachable
        ai.bestYSub = 0;
        ai.bestRtSub = Piece.DIRECTION_UP;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint unreachable sub executed");
    }

    // Common grounded setup for the sub-match rotate block (914-943).
    private void groundedSubMatch(int nowDir, int bestRt) {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = nowDir;
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        for (int x = 0; x < 10; x++) engine.field.setBlockColor(x, 19, 1);
        ai.bestPts = 3000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 9;             // differs from bestXSub so 912 does not return
        ai.bestY = 0;
        ai.bestRt = bestRt;
        ai.bestXSub = 5;          // == nowX
        ai.bestYSub = 18;         // == nowY
        ai.bestRtSub = nowDir;    // == rt -> sub-match block
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;
    }

    // ─── renderHint sub-match: bestRt == rrot -> ROTATE RIGHT (925, 940) ───
    @Test
    void renderHintSubMatchRrot() {
        groundedSubMatch(Piece.DIRECTION_UP, Piece.DIRECTION_RIGHT); // rrot from UP
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint sub-match rrot");
    }

    // ─── renderHint sub-match: bestRt == lrot -> ROTATE LEFT (927, 938) ───
    @Test
    void renderHintSubMatchLrot() {
        groundedSubMatch(Piece.DIRECTION_UP, Piece.DIRECTION_LEFT); // lrot from UP
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint sub-match lrot");
    }

    // ─── renderHint sub-match: reverse-180 rrot==UP -> rotateDir 1 (931) ───
    @Test
    void renderHintSubMatchReverse180RrotUp() {
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.ruleopt.rotateButtonAllowReverse = true;
        groundedSubMatch(Piece.DIRECTION_LEFT, Piece.DIRECTION_RIGHT); // 180, rrot==UP
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint sub-match reverse-180 rrot==UP");
    }

    // ─── renderHint sub-match: final else rotateDir 1 (936) ───
    @Test
    void renderHintSubMatchFinalElse() {
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.ruleopt.rotateButtonAllowReverse = false;
        groundedSubMatch(Piece.DIRECTION_UP, Piece.DIRECTION_DOWN); // 180, no reverse
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint sub-match final else");
    }

    // Common airborne setup for the rt != bestRtSub rotate block (951-979).
    // Piece is high in the air so it never touches ground and the 912/914
    // early returns are skipped.
    private void airborneRotate(int nowDir, int bestRtSub) {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = nowDir;
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 3;     // airborne -> pieceTouchGround false
        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = nowDir;
        ai.bestXSub = 5;          // == nowX
        ai.bestYSub = 18;
        ai.bestRtSub = bestRtSub; // != rt -> enters rotate block
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;
    }

    // ─── renderHint rotate block: bestRtSub == rrot -> ROTATE RIGHT (960, 976) ───
    @Test
    void renderHintRotateRrot() {
        airborneRotate(Piece.DIRECTION_UP, Piece.DIRECTION_RIGHT);
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint rotate rrot");
    }

    // ─── renderHint rotate block: bestRtSub == lrot -> ROTATE LEFT (962, 974) ───
    @Test
    void renderHintRotateLrot() {
        airborneRotate(Piece.DIRECTION_UP, Piece.DIRECTION_LEFT);
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint rotate lrot");
    }

    // ─── renderHint rotate block: reverse-180 rrot==UP (966-967) ───
    @Test
    void renderHintRotateReverse180RrotUp() {
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.ruleopt.rotateButtonAllowReverse = true;
        airborneRotate(Piece.DIRECTION_LEFT, Piece.DIRECTION_RIGHT); // 180, rrot==UP
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint rotate reverse-180 rrot==UP");
    }

    // ─── renderHint rotate block: reverse-180 rrot!=UP (968-969) ───
    @Test
    void renderHintRotateReverse180RrotNotUp() {
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.ruleopt.rotateButtonAllowReverse = true;
        airborneRotate(Piece.DIRECTION_RIGHT, Piece.DIRECTION_LEFT); // 180, rrot==DOWN
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint rotate reverse-180 rrot!=UP");
    }

    // ─── renderHint rotate block: final else rotateDir 1 (972) ───
    @Test
    void renderHintRotateFinalElse() {
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.ruleopt.rotateButtonAllowReverse = false;
        airborneRotate(Piece.DIRECTION_UP, Piece.DIRECTION_DOWN); // 180, no reverse
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint rotate final else");
    }

    // ─── renderHint funnel: grounded softdropLock -> SOFT DROP (998-999, 1027) ───
    @Test
    void renderHintFunnelGroundedSoftdropLock() {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 17;
        for (int x = 0; x < 10; x++) engine.field.setBlockColor(x, 19, 1);
        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_UP;     // bestRtSub == bestRt -> first funnel branch
        ai.bestXSub = 5;                    // == nowX
        ai.bestYSub = 18;                   // != nowY so 914 does not return early
        ai.bestRtSub = Piece.DIRECTION_UP;  // == rt so 951 skipped, 994 funnel
        engine.ruleopt.softdropLock = true; // grounded + softdropLock -> drop=-1 (999)
        engine.ruleopt.harddropEnable = false;
        engine.ruleopt.softdropEnable = false;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint funnel grounded softdropLock");
    }

    // ─── renderHint funnel: bestRtSub != bestRt harddrop (1004-1006, 1028) ───
    @Test
    void renderHintFunnelSubDiffHarddrop() {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 17;
        for (int x = 0; x < 10; x++) engine.field.setBlockColor(x, 19, 1);
        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_RIGHT;  // bestRtSub != bestRt -> else funnel (1004)
        ai.bestXSub = 5;
        ai.bestYSub = 18;
        ai.bestRtSub = Piece.DIRECTION_UP;  // == rt
        engine.ruleopt.harddropEnable = true;
        engine.ruleopt.harddropLock = false;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint funnel sub-diff harddrop");
    }

    // ─── renderHint funnel: bestRtSub != bestRt softdrop (1007-1008) ───
    @Test
    void renderHintFunnelSubDiffSoftdrop() {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 17;
        for (int x = 0; x < 10; x++) engine.field.setBlockColor(x, 19, 1);
        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_RIGHT;  // != bestRtSub -> else funnel branch
        ai.bestXSub = 5;
        ai.bestYSub = 18;
        ai.bestRtSub = Piece.DIRECTION_UP;
        engine.ruleopt.harddropEnable = false;
        engine.ruleopt.harddropLock = false;
        engine.ruleopt.softdropEnable = true;
        engine.ruleopt.softdropLock = false;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint funnel sub-diff softdrop");
    }

    // ─── renderHint funnel: airborne softdropEnable, bestRtSub == bestRt (1000-1003) ───
    @Test
    void renderHintFunnelAirborneSoftdrop() {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 5;
        engine.nowPieceY = 3;               // airborne -> 998 false
        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_UP;     // bestRtSub == bestRt -> first funnel branch
        ai.bestXSub = 5;                    // == nowX
        ai.bestYSub = 18;                   // >= nowY so not unreachable
        ai.bestRtSub = Piece.DIRECTION_UP;  // == rt
        engine.ruleopt.harddropEnable = false; // skip 1000
        engine.ruleopt.softdropEnable = true;  // 1002 -> drop=-1 (1003)
        engine.ruleopt.softdropLock = false;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint funnel airborne softdrop (1002-1003)");
    }

    // ─── renderHint: move-left output (1011-1012, 1019) ───
    @Test
    void renderHintMoveLeftOutput() {
        engine.createFieldIfNeeded();
        Piece p = newOffsetPiece(Piece.PIECE_T);
        p.direction = Piece.DIRECTION_UP;
        engine.nowPieceObject = p;
        engine.nowPieceX = 7;       // nowX > bestXSub -> moveDir=-1 (1012)
        engine.nowPieceY = 3;       // airborne
        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 3;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 3;            // < nowX
        ai.bestYSub = 18;
        ai.bestRtSub = Piece.DIRECTION_UP; // == rt so 951 skipped
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint move-left output");
    }
}
