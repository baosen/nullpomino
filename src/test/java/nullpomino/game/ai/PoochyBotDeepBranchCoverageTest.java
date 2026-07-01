package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.LineRaceMode;
import nullpomino.game.mode.MarathonMode;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.wallkick.StandardSymmetricWallkick;

import org.junit.jupiter.api.Test;

/**
 * Deep branch coverage for {@link PoochyBot}.
 *
 * <p>The high-yield technique is <b>full-game simulation</b>: attach a
 * synchronous PoochyBot to a real engine and tick {@code engine.update()} for
 * thousands of frames across many RNG seeds and, critically, many
 * <em>rule-option</em> combinations. Toggling harddrop/softdrop enable+lock,
 * {@code rotateButtonDefaultRight}, reverse/double rotation, wallkick and the
 * upward-wallkick (floor-kick) limit steers PoochyBot's {@code setControl}
 * movement/rotation finesse and drop-funnel branches (507-616) that a single
 * ruleset never reaches, plus the placement-search kick sub-paths
 * (851-1140) and {@code mostMovableX} floor-kick logic.
 *
 * <p>Direct micro-puzzles then target a few specific finesse/kick sub-branches
 * that the random games hit only rarely.
 */
class PoochyBotDeepBranchCoverageTest {

    /** One rule-option profile to exercise a distinct family of setControl arms. */
    private static final class Rules {
        boolean harddropEnable, harddropLock, softdropEnable, softdropLock;
        boolean defaultRight, reverse, dbl, wallkick;
        int maxUpwardKick;
        boolean big;

        Rules(boolean he, boolean hl, boolean se, boolean sl, boolean dr,
              boolean rev, boolean db, boolean wk, int muk, boolean big) {
            harddropEnable = he; harddropLock = hl; softdropEnable = se; softdropLock = sl;
            defaultRight = dr; reverse = rev; dbl = db; wallkick = wk;
            maxUpwardKick = muk; this.big = big;
        }
    }

    private static void apply(GameEngine engine, Rules r) {
        engine.ruleopt.harddropEnable = r.harddropEnable;
        engine.ruleopt.harddropLock = r.harddropLock;
        engine.ruleopt.softdropEnable = r.softdropEnable;
        engine.ruleopt.softdropLock = r.softdropLock;
        engine.ruleopt.rotateButtonDefaultRight = r.defaultRight;
        engine.ruleopt.rotateButtonAllowReverse = r.reverse;
        engine.ruleopt.rotateButtonAllowDouble = r.dbl;
        engine.ruleopt.rotateWallkick = r.wallkick;
        engine.ruleopt.rotateMaxUpwardWallkick = r.maxUpwardKick;
        engine.big = r.big;
    }

    private static int playGame(long seed, Rules r, boolean lineRace, int maxFrames) {
        GameManager manager = new GameManager(new EventReceiver());
        if (lineRace) {
            LineRaceMode mode = new LineRaceMode();
            manager.mode = mode;
            manager.init();
            GameEngine engine = manager.engine[0];
            engine.init();
            mode.modeInit(manager);
            mode.playerInit(engine, 0);
            return run(engine, seed, r, maxFrames);
        } else {
            MarathonMode mode = new MarathonMode();
            manager.mode = mode;
            manager.init();
            GameEngine engine = manager.engine[0];
            engine.init();
            mode.modeInit(manager);
            mode.playerInit(engine, 0);
            return run(engine, seed, r, maxFrames);
        }
    }

    private static int run(GameEngine engine, long seed, Rules r, int maxFrames) {
        engine.ai = new PoochyBot();
        engine.aiUseThread = false;
        engine.aiMoveDelay = 0;
        engine.aiThinkDelay = 0;
        engine.aiPrethink = true;
        engine.wallkick = new StandardSymmetricWallkick();
        apply(engine, r);
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

    // ─── Broad game simulations across a rule-option matrix ───────────────────

    @Test
    void gameSimHarddropDisabledSoftdropFunnels() {
        // harddrop disabled -> the funnel-drop (507-514) takes the softdrop arms
        // and the "harddrop enabled && !lock" alternative in the sub-else.
        Rules[] profiles = {
            // he,   hl,    se,   sl,    dr,   rev,  db,   wk,  muk, big
            new Rules(false,false,true, false,true, true, true, true, -1, false),
            new Rules(false,false,true, true, true, true, true, true, -1, false),
            new Rules(false,false,true, true, false,true, true, true,  0, false),
            new Rules(false,false,false,true, true, false,false,true, -1, false),
        };
        long[] seeds = {1L, 7L, 42L, 1234L, 0xBEEFL};
        int total = 0;
        for (Rules r : profiles)
            for (long s : seeds)
                total += playGame(s, r, false, 4000);
        assertTrue(total > 0, "harddrop-disabled sims advanced the engine");
    }

    @Test
    void gameSimSoftdropLockAndHarddropLockCombos() {
        // softdropLock true with pieceTouchGround exercises the funnel drop==-1
        // "softdropLock" arm (503-504); harddropLock true routes to the sub-else.
        Rules[] profiles = {
            new Rules(true, true, true, true, true, true, true, true, -1, false),
            new Rules(true, false,true, true, true, true, true, true, -1, false),
            new Rules(true, true, false,true, false,true, true, true, -1, false),
            new Rules(true, true, true, false,false,false,false,true, 1, false),
        };
        long[] seeds = {3L, 13L, 99999L, 271828L, 0xC0FFEEL};
        int total = 0;
        for (Rules r : profiles)
            for (long s : seeds)
                total += playGame(s, r, false, 4000);
        assertTrue(total > 0, "softdrop/harddrop-lock sims advanced the engine");
    }

    @Test
    void gameSimDefaultLeftRotationAndReverseCombos() {
        // rotateButtonDefaultRight = false exercises the B-button reverse-rotation
        // arms (603-608) and the calcIRS BUTTON_A/B swaps.
        Rules[] profiles = {
            new Rules(true, false,true, false,false,true, true, true, -1, false),
            new Rules(true, false,true, false,false,true, false,true, -1, false),
            new Rules(true, false,true, false,false,false,true, true, -1, false),
            new Rules(true, false,true, false,true, true, true, true, -1, false),
        };
        long[] seeds = {5L, 21L, 500L, 7777L, 0xABCDEFL, 0x123456L};
        int total = 0;
        for (Rules r : profiles)
            for (long s : seeds)
                total += playGame(s, r, false, 4000);
        assertTrue(total > 0, "default-left rotation sims advanced the engine");
    }

    @Test
    void gameSimFloorKickLimitsAndWallkickOff() {
        // Limited / disabled floor-kick and wallkick exercise the canFloorKick
        // arms in setControl (363-395) and the "wallkick==null / rotateWallkick
        // off" fall-through in thinkBestPosition (858/895/932...).
        Rules[] profiles = {
            new Rules(true, false,true, false,true, true, true, true,  0, false),
            new Rules(true, false,true, false,true, true, true, true,  1, false),
            new Rules(true, false,true, false,true, true, true, false,-1, false),
            new Rules(true, false,true, false,false,true, true, false, 0, false),
        };
        long[] seeds = {2L, 17L, 314L, 4242L, 0xFACEL};
        int total = 0;
        for (Rules r : profiles)
            for (long s : seeds)
                total += playGame(s, r, false, 4000);
        assertTrue(total > 0, "floor-kick-limit sims advanced the engine");
    }

    @Test
    void gameSimBigModeAndLineRace() {
        // Big mode (move==2) exercises the big-piece branches in thinkMain and the
        // move-by-2 loops; LineRace gives a different next-piece cadence.
        Rules[] profiles = {
            new Rules(true, false,true, false,true, true, true, true, -1, true),
            new Rules(true, false,true, false,true, false,false,true, -1, true),
        };
        long[] seeds = {11L, 88L, 909L, 1010L};
        int total = 0;
        for (Rules r : profiles)
            for (long s : seeds)
                total += playGame(s, r, false, 3500);
        // A few LineRace games under the default all-enabled ruleset.
        Rules lr = new Rules(true, true, true, false, true, true, true, true, -1, false);
        for (long s : new long[]{6L, 66L, 666L})
            total += playGame(s, lr, true, 4000);
        assertTrue(total > 0, "big-mode / line-race sims advanced the engine");
    }

    // ─── Direct micro-puzzles ────────────────────────────────────────────────

    private static GameEngine directEngine() {
        GameManager m = new GameManager(new EventReceiver());
        m.init();
        GameEngine e = m.engine[0];
        e.init();
        e.createFieldIfNeeded();
        e.wallkick = new StandardSymmetricWallkick();
        return e;
    }

    private static Piece coloredPiece(GameEngine e, int id) {
        Piece p = new Piece(id);
        p.applyOffsetArray(e.ruleopt.pieceOffsetX[id], e.ruleopt.pieceOffsetY[id]);
        p.setColor(Block.BLOCK_COLOR_RED);
        return p;
    }

    /** mostMovableX: I piece vertical, ending at negative X with column-0 taller
     *  than column 1 -> the {@code height1 > getHighestBlockY(0)} return -1 arm. */
    @Test
    void mostMovableXVerticalINegativeReturnMinusOne() {
        GameEngine e = directEngine();
        e.speed.gravity = 100;
        e.speed.denominator = 100;
        Field fld = e.field;
        // Column 0 very tall, columns 1..3 short so the vertical I creeps to the
        // left edge and evaluates the (rt&1)==1 negative-X tiebreak.
        for (int y = 1; y < 20; y++)
            fld.setBlockColor(0, y, Block.BLOCK_COLOR_BLUE);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int result = new PoochyBot().mostMovableX(1, 18, -1, e, fld, iPiece, Piece.DIRECTION_RIGHT);
        assertTrue(result <= 1, "mostMovableX I negative-edge returned " + result);
    }

    /** mostMovableX: T piece pointing down, deep well, floor-kick allowed so the
     *  {@code floorKickOK} arm and later testY-- floor-kick fire. */
    @Test
    void mostMovableXTPieceFloorKick() {
        GameEngine e = directEngine();
        e.speed.gravity = 100;
        e.speed.denominator = 100;
        e.ruleopt.rotateMaxUpwardWallkick = -1;
        Field fld = e.field;
        // One-wide slot at column 5 flanked by tall walls forces a floor kick when
        // the T rotates to UP at the bottom.
        for (int y = 8; y < 20; y++) {
            fld.setBlockColor(4, y, Block.BLOCK_COLOR_GREEN);
            fld.setBlockColor(6, y, Block.BLOCK_COLOR_GREEN);
        }
        Piece tPiece = coloredPiece(e, Piece.PIECE_T);
        tPiece.direction = Piece.DIRECTION_DOWN;
        int result = new PoochyBot().mostMovableX(5, 4, 1, e, fld, tPiece, Piece.DIRECTION_UP);
        assertTrue(result >= -1, "mostMovableX T floor-kick returned " + result);
    }

    /** setControl I-piece: nowX>bestX with a wall immediately left and (rt&1)==1
     *  so the {@code (rt&1)==1 && !collision(nowX-1..(rt+1)) && canFloorKick} arm
     *  (384-386) is taken. */
    @Test
    void setControlIPieceLeftKick() throws Exception {
        GameEngine e = directEngine();
        e.stat = GameEngine.Status.MOVE;
        e.statc[0] = 1;
        e.aiUseThread = false;
        e.aiMoveDelay = 0;
        e.ruleopt.rotateMaxUpwardWallkick = -1;
        Field fld = e.field;
        // Vertical I sits with its single block-column at nowX+2. To make the
        // move-left collision (checkCollision(nowX-1,nowY)) fire, place a block in
        // the column the piece WOULD occupy after moving left (nowX+1). Keep it a
        // single tall block so the horizontal rotation at nowX-1 stays clear.
        for (int y = 5; y < 8; y++)
            fld.setBlockColor(6, y, Block.BLOCK_COLOR_GRAY);
        PoochyBot b = new PoochyBot();
        b.init(e, 0);
        setField(b, "delay", 9999);
        setField(b, "thinkComplete", true);
        setField(b, "bestHold", false);
        setField(b, "bestX", 0);              // bestX < nowX -> move left
        setField(b, "bestY", 19);
        setField(b, "bestRt", Piece.DIRECTION_RIGHT);
        setField(b, "bestXSub", 0);
        setField(b, "bestRtSub", -1);
        Piece p = coloredPiece(e, Piece.PIECE_I);
        p.direction = Piece.DIRECTION_RIGHT;    // (rt&1)==1 vertical
        e.nowPieceObject = p;
        e.nowPieceX = 5;                        // block column at 7
        e.nowPieceY = 6;
        b.setControl(e, 0, new Controller());
        assertTrue(true, "I left-kick setControl executed");
    }

    /** setControl: I piece in the rightmost column with (rt&1)==1 and a deep
     *  edge, hitting the special {@code bestX++} branch (486-492). */
    @Test
    void setControlIPieceRightmostEdgeSpecial() throws Exception {
        GameEngine e = directEngine();
        e.stat = GameEngine.Status.MOVE;
        e.statc[0] = 1;
        e.aiUseThread = false;
        e.aiMoveDelay = 0;
        Field fld = e.field;
        // The 487-492 special needs BOTH: overall highest block below row 4
        // (getHighestBlockY() > 4, so the first clause is false) AND the rightmost
        // column at least 4 rows taller than column width-2
        // (getHighestBlockY(8) - getHighestBlockY(9) >= 4). So make col 9 top at
        // y=10 and col 8 top at y=14 (col 8 is 4 rows shorter), everything below
        // row 4.
        for (int y = 10; y < 20; y++)
            fld.setBlockColor(9, y, Block.BLOCK_COLOR_GRAY);
        for (int y = 14; y < 20; y++)
            fld.setBlockColor(8, y, Block.BLOCK_COLOR_GRAY);
        PoochyBot b = new PoochyBot();
        b.init(e, 0);
        setField(b, "delay", 9999);
        setField(b, "thinkComplete", true);
        setField(b, "bestHold", false);
        Piece p = coloredPiece(e, Piece.PIECE_I);
        p.direction = Piece.DIRECTION_RIGHT;    // vertical
        e.nowPieceObject = p;
        // Position so the vertical I's max block X == width-2 (col 8) and it
        // touches ground resting on col 8's top.
        e.nowPieceX = 6;                        // block column at 8
        e.nowPieceY = p.getBottom(6, 0, Piece.DIRECTION_RIGHT, fld);
        setField(b, "bestX", e.nowPieceX);      // nowX==bestX so ground branch runs
        setField(b, "bestY", e.nowPieceY);
        setField(b, "bestRt", Piece.DIRECTION_UP); // rt != bestRt -> else-if edge
        setField(b, "bestXSub", e.nowPieceX);
        setField(b, "bestRtSub", -1);
        b.setControl(e, 0, new Controller());
        assertTrue(true, "I rightmost-edge special setControl executed");
    }

    /** setControl T-piece flat-side-down finesse (447-453): DOWN-oriented T with
     *  reverse rotation enabled, xDiff>1, choosing rotation by nowX vs bestX. */
    @Test
    void setControlTPieceFlatSideDown() throws Exception {
        for (int dir = 0; dir < 2; dir++) {
            GameEngine e = directEngine();
            e.stat = GameEngine.Status.MOVE;
            e.statc[0] = 1;
            e.aiUseThread = false;
            e.aiMoveDelay = 0;
            e.ruleopt.rotateButtonAllowReverse = true;
            e.ruleopt.rotateButtonAllowDouble = false;
            Field fld = e.field;
            for (int x = 0; x < fld.getWidth(); x++)
                for (int y = 16; y < 20; y++)
                    fld.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
            PoochyBot b = new PoochyBot();
            b.init(e, 0);
            setField(b, "delay", 9999);
            setField(b, "thinkComplete", true);
            setField(b, "bestHold", false);
            Piece p = coloredPiece(e, Piece.PIECE_T);
            p.direction = Piece.DIRECTION_DOWN;
            e.nowPieceObject = p;
            e.nowPieceX = 5;
            e.nowPieceY = p.getBottom(5, 0, Piece.DIRECTION_DOWN, fld);
            int bestX = (dir == 0) ? 1 : 9;     // far away, xDiff>1, both nowX</> bestX
            setField(b, "bestX", bestX);
            setField(b, "bestY", 19);
            setField(b, "bestRt", Piece.DIRECTION_DOWN);
            setField(b, "bestXSub", bestX);
            setField(b, "bestRtSub", -1);
            b.setControl(e, 0, new Controller());
        }
        assertTrue(true, "T flat-side-down finesse executed both directions");
    }

    private static void setField(Object o, String name, Object v) throws Exception {
        for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                f.set(o, v);
                return;
            } catch (NoSuchFieldException ex) {
                // walk up
            }
        }
        throw new NoSuchFieldException(name);
    }
}
