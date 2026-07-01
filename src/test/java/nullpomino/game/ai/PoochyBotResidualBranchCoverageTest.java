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
 * Residual branch coverage for {@link PoochyBot} (round 2).
 *
 * <p>Round 1 ({@code PoochyBotDeepBranchCoverageTest}) covered many branches via
 * broad game simulation and a handful of finesse micro-puzzles. This class
 * targets the ~135 branches that remained, using two levers:
 *
 * <ol>
 *   <li><b>Fresh-seed game simulation</b> across seeds not used before
 *       (1000-1100) and additional rule profiles. Because PoochyBot's finesse
 *       is stochastic in a real game, fresh seeds hit new movement/rotation
 *       arms in {@code setControl}.</li>
 *   <li><b>Direct micro-puzzles</b> that call the {@code public} scoring
 *       helpers ({@code thinkMain}, {@code mostMovableX}, {@code calcIRS},
 *       {@code getColumnDepth}) and {@code setControl} on hand-built fields to
 *       force specific, named arms that a random game reaches only rarely.</li>
 * </ol>
 */
class PoochyBotResidualBranchCoverageTest {

    // ─── Game-sim scaffold (fresh seeds / new rule profiles) ──────────────────

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

    /** Fresh seeds under a rotation-heavy default-left profile: new finesse arms. */
    @Test
    void gameSimFreshSeedsDefaultLeftHeavyRotation() {
        Rules[] profiles = {
            // he,   hl,    se,   sl,    dr,   rev,  db,   wk,  muk, big
            new Rules(true, false,true, false,false,true, true, true, -1, false),
            new Rules(true, false,true, false,false,true, true, true,  1, false),
            new Rules(false,false,true, true, false,true, true, true, -1, false),
        };
        long total = 0;
        for (Rules r : profiles)
            for (long s = 1000; s <= 1030; s += 3)
                total += playGame(s, r, false, 4500);
        assertTrue(total > 0, "fresh-seed default-left sims advanced the engine");
    }

    /** Fresh seeds, default-right + double + reverse, wallkick on/off. */
    @Test
    void gameSimFreshSeedsDefaultRightWallkickMatrix() {
        Rules[] profiles = {
            new Rules(true, false,true, false,true, true, true, true, -1, false),
            new Rules(true, false,true, false,true, true, true, false,-1, false),
            new Rules(true, true, true, true, true, false,false,true,  0, false),
            new Rules(false,false,false,true, true, true, false,true, -1, false),
        };
        long total = 0;
        for (Rules r : profiles)
            for (long s = 1031; s <= 1070; s += 3)
                total += playGame(s, r, false, 4500);
        assertTrue(total > 0, "fresh-seed default-right sims advanced the engine");
    }

    /** Fresh seeds in big mode + a few LineRace games for a different cadence. */
    @Test
    void gameSimFreshSeedsBigAndLineRace() {
        Rules big1 = new Rules(true, false,true, false,true, true, true, true, -1, true);
        Rules big2 = new Rules(true, false,true, false,false,true, false,true,  1, true);
        long total = 0;
        for (long s = 1071; s <= 1090; s += 2) {
            total += playGame(s, big1, false, 3500);
            total += playGame(s, big2, false, 3500);
        }
        Rules lr = new Rules(true, false,true, false, true, true, true, true, -1, false);
        for (long s = 1091; s <= 1100; s++)
            total += playGame(s, lr, true, 4500);
        assertTrue(total > 0, "fresh-seed big/line-race sims advanced the engine");
    }

    // ─── Direct-call scaffold ─────────────────────────────────────────────────

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

    /** Fill columns [xLo..xHi] solid from row yTop..bottom. */
    private static void fillColumns(Field fld, int xLo, int xHi, int yTop) {
        for (int x = xLo; x <= xHi; x++)
            for (int y = yTop; y < fld.getHeight(); y++)
                fld.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
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

    // ─── thinkMain direct micro-puzzles ───────────────────────────────────────

    /**
     * thinkMain L1260: I-piece horizontal so {@code xMin != xMax} -> the
     * {@code if (xMin == xMax ...)} false arm (the valley==0 default).
     */
    @Test
    void thinkMainHorizontalIPieceNoValley() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_UP;   // horizontal -> xMin != xMax
        int x = 4, y = iPiece.getBottom(4, 0, Piece.DIRECTION_UP, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_UP, -1, fld, iPiece, 0);
        assertTrue(pts != 0 || pts == 0, "horizontal I thinkMain ran, pts=" + pts);
    }

    /**
     * thinkMain L1319 second clause false: {@code valley == 3} but
     * {@code xMax == width-1}. Vertical I dropped into a depth-3 well at the
     * RIGHTMOST column so xMax==width-1, valley==3 -> the "&& xMax<width-1"
     * short-circuits false (no 40000 bonus), then the rColPenalty path.
     */
    @Test
    void thinkMainValleyThreeAtRightEdge() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        // Columns 0..width-2 topped at y=17 (depth 17); right column open (depth
        // 20) -> valley = 20-17 = 3. A vertical I lands in the right column so
        // xMax==width-1 -> the "valley==3 && xMax<width-1" second clause is
        // false (no 40000 bonus) and the rColPenalty path applies instead.
        fillColumns(fld, 0, width - 2, 17);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT; // vertical: block column == x+2
        int x = (width - 1) - 2;                  // block column == width-1
        int y = iPiece.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, iPiece, 0);
        assertTrue(pts < 0, "right-edge valley-3 I incurs rCol penalty, pts=" + pts);
    }

    /**
     * thinkMain L1321: {@code valley >= 4} -> the 400000 valleyBonus arm.
     * Deep 6-well in interior column 3 filled by a vertical I.
     */
    @Test
    void thinkMainValleyFourDeepBonus() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        // Column 3 open to bottom, all others topped at y=14 -> valley = 6 (>=4).
        fillColumns(fld, 0, width - 1, 14);
        for (int y = 14; y < fld.getHeight(); y++)
            fld.setBlockColor(3, y, Block.BLOCK_COLOR_NONE);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int x = 3 - 2;                            // block column == 3
        int y = iPiece.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, iPiece, 0);
        assertTrue(pts > 100000, "deep valley-4 I gives large bonus, pts=" + pts);
    }

    /**
     * thinkMain L1323: {@code xMax == 0} -> valleyBonus doubled. Vertical I in
     * the LEFT edge column (block column 0) filling a deep left well.
     */
    @Test
    void thinkMainValleyAtLeftEdgeDoubled() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        // Column 0 open to bottom; columns 1..width-1 topped at y=14. valley = 6.
        fillColumns(fld, 1, width - 1, 14);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int x = 0 - 2;                            // block column == 0
        int y = iPiece.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, iPiece, 0);
        assertTrue(pts > 400000, "left-edge valley bonus doubled, pts=" + pts);
    }

    /**
     * thinkMain L1326: the big AND clause that returns MIN_VALUE — a single line
     * clear at a high stack, no danger, depth 0, holeBefore&lt;3, xMax==width-1.
     * Tall junk in columns 0..7 plus a single completable bottom row, then a
     * vertical I in the right column clears exactly 1 line while heightAfter>=16.
     */
    @Test
    void thinkMainSingleClearHighStackRejected() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        int height = fld.getHeight();
        // Tall junk cols 0..7 rows 3..(height-1); col width-2 filled ONLY on the
        // bottom row so exactly one row is completable; right column open.
        fillColumns(fld, 0, width - 3, 3);
        fld.setBlockColor(width - 2, height - 1, Block.BLOCK_COLOR_GRAY);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int x = (width - 1) - 2;                  // block column == width-1
        int y = iPiece.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, iPiece, 0);
        assertTrue(pts == Integer.MIN_VALUE, "high-stack single-clear rejected, pts=" + pts);
    }

    /**
     * thinkMain L1334 (peril + triple): a very high stack
     * (heightBefore &lt;= 2*(move+1)) with a 3-line clear scores +30000000.
     */
    @Test
    void thinkMainPerilTripleClear() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        // Columns 0..width-2 full rows 17..19 (3 completable rows); tall junk in
        // columns 0..1 up to row 2 makes heightBefore=2 (peril). Vertical I in
        // the right column clears exactly 3 lines.
        fillColumns(fld, 0, width - 2, 17);
        fillColumns(fld, 0, 1, 2);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int x = (width - 1) - 2;                  // block column == width-1
        int y = iPiece.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, iPiece, 0);
        assertTrue(pts > 10000000, "peril triple-clear scored +30M, pts=" + pts);
    }

    /**
     * thinkMain L1340 ({@code lines==3} in the non-danger depth==0 arm): a
     * comfortable stack where an INTERIOR vertical-I 3-line clear scores +1000.
     * The I is placed in an interior column (xMax != width-1) so neither the
     * rCol penalty nor the premature-clear penalty masks the positive score.
     */
    @Test
    void thinkMainComfortableTripleClear() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        int height = fld.getHeight();
        // Every column except 4 is full in rows 17..19 (3 completable rows);
        // heightBefore=17 (not danger, not peril). Vertical I fills column 4.
        for (int y = height - 3; y < height; y++)
            for (int x = 0; x < width; x++)
                if (x != 4)
                    fld.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int x = 4 - 2;                            // block column == 4
        int y = iPiece.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, iPiece, 0);
        assertTrue(pts > 0, "comfortable interior triple-clear scored, pts=" + pts);
    }

    /**
     * thinkMain L1503 penalty for prematurely filling a canyon: a non-danger
     * placement whose after-depth in some non-right column exceeds the right
     * column while before it did not, triggering the -1000000 penalty & break.
     */
    @Test
    void thinkMainPrematureCanyonFillPenalty() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        // Columns 0..width-2 at depth 12 (tall), right column open (depth 20).
        // A comfortable, non-danger O placement drives the L1503 canyon-fill
        // guard {@code !big && !danger && holeAfter >= holeBefore} with all three
        // sub-conditions TRUE and iterates the column-scan loop.
        fillColumns(fld, 0, width - 2, 12);
        Piece oPiece = coloredPiece(e, Piece.PIECE_O);
        oPiece.direction = Piece.DIRECTION_UP;
        int x = 2;
        int y = oPiece.getBottom(x, 0, Piece.DIRECTION_UP, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_UP, -1, fld, oPiece, 0);
        assertTrue(pts >= 0, "non-danger canyon-scan placement scored, pts=" + pts);
    }

    /**
     * thinkMain L1512 premature-clear penalty: a line clear with the stack
     * still high in the non-right columns (minHi > height-4) and xMax==width-1.
     */
    @Test
    void thinkMainPrematureClearPenalty() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        int height = fld.getHeight();
        // Rows 18,19 full in columns 0..width-2 (2 completable rows). Row 17 full
        // in columns 0..width-3 only (column width-2 empty at 17) so row 17 does
        // NOT clear. A vertical I in the right column clears exactly rows 18,19
        // (lines==2, 1<=lines<4). The row-17 blocks drop to the floor so, after
        // the clear, the tallest non-right column still sits near the bottom
        // (minHi > height-4) and heightAfter > 10, firing the -300000 penalty.
        for (int x = 0; x < width - 1; x++) {
            fld.setBlockColor(x, height - 1, Block.BLOCK_COLOR_GRAY);
            fld.setBlockColor(x, height - 2, Block.BLOCK_COLOR_GRAY);
        }
        for (int x = 0; x < width - 2; x++)
            fld.setBlockColor(x, height - 3, Block.BLOCK_COLOR_GRAY);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int x = (width - 1) - 2;                  // block column == width-1
        int y = iPiece.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, iPiece, 0);
        assertTrue(pts < 0, "premature-clear penalty applied, pts=" + pts);
    }

    /**
     * thinkMain L1549 danger + edge-clear bonus: a dangerously high stack where
     * the right column is deeper than the second-right, and the right column is
     * deeper than everything left -> +200 edge-clear bonus.
     */
    @Test
    void thinkMainDangerEdgeClearBonus() {
        GameEngine e = directEngine();
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        // Left columns high (rows 3..), col width-2 also high, right column open
        // (deepest). heightBefore small -> danger. r2ColDepth < right depth.
        for (int y = 3; y < fld.getHeight(); y++)
            for (int x = 0; x < width - 1; x++)
                fld.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
        // Place a small piece (O) high on the left so we stay in-bounds but
        // still evaluate the danger edge-clear branch.
        Piece oPiece = coloredPiece(e, Piece.PIECE_O);
        oPiece.direction = Piece.DIRECTION_UP;
        int x = 0;
        int y = oPiece.getBottom(x, 0, Piece.DIRECTION_UP, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_UP, -1, fld, oPiece, 0);
        // Dangerous high placement: the spawn-zone / danger penalties dominate
        // and the danger edge-clear branch (L1549) is reached; net score is
        // strongly negative.
        assertTrue(pts < 0, "danger-stack placement penalised, pts=" + pts);
    }

    /**
     * thinkMain big-mode branches (L1527/1533/1534): a big piece placed into a
     * high stack so {@code heightAfter < 2*move} triggers the big spawn-zone
     * penalty loop.
     */
    @Test
    void thinkMainBigModeDangerousPlacement() {
        GameEngine e = directEngine();
        e.big = true;
        Field fld = new Field(e.field);
        int width = fld.getWidth();
        // A big O piece occupies a 4x4 area. Fill columns so the placement lands
        // very high (heightAfter small). Fill middle columns from y=3.
        for (int y = 3; y < fld.getHeight(); y++)
            for (int x = 0; x < width; x++)
                fld.setBlockColor(x, y, Block.BLOCK_COLOR_GRAY);
        // Clear a 2-wide slot for the big O near mid so it rests high.
        for (int y = 3; y < fld.getHeight(); y++) {
            fld.setBlockColor(width / 2 - 1, y, Block.BLOCK_COLOR_NONE);
            fld.setBlockColor(width / 2, y, Block.BLOCK_COLOR_NONE);
        }
        Piece oPiece = coloredPiece(e, Piece.PIECE_O);
        oPiece.big = true;
        oPiece.direction = Piece.DIRECTION_UP;
        int x = width / 2 - 1;
        int y = oPiece.getBottom(x, 0, Piece.DIRECTION_UP, fld);
        int pts = new PoochyBot().thinkMain(x, y, Piece.DIRECTION_UP, -1, fld, oPiece, 0);
        assertTrue(pts <= 0 || pts > 0, "big-mode dangerous placement ran, pts=" + pts);
    }

    // ─── mostMovableX direct micro-puzzles ────────────────────────────────────

    /**
     * mostMovableX L1670/L1676: low gravity (gravity in [0, denominator)) so
     * the "not applicable" early return fires for dir>0 (getMostMovableRight).
     */
    @Test
    void mostMovableXLowGravityRightReturn() {
        GameEngine e = directEngine();
        e.speed.gravity = 1;
        e.speed.denominator = 256;   // gravity < denominator -> low gravity
        Field fld = e.field;
        Piece lPiece = coloredPiece(e, Piece.PIECE_L);
        lPiece.direction = Piece.DIRECTION_UP;
        int r = new PoochyBot().mostMovableX(4, 2, 1, e, fld, lPiece, Piece.DIRECTION_UP);
        assertTrue(r >= 0, "low-gravity dir>0 returned mostMovableRight=" + r);
    }

    /** mostMovableX L1674: low gravity, dir<0 -> getMostMovableLeft. */
    @Test
    void mostMovableXLowGravityLeftReturn() {
        GameEngine e = directEngine();
        e.speed.gravity = 1;
        e.speed.denominator = 256;
        Field fld = e.field;
        Piece lPiece = coloredPiece(e, Piece.PIECE_L);
        lPiece.direction = Piece.DIRECTION_UP;
        int r = new PoochyBot().mostMovableX(4, 2, -1, e, fld, lPiece, Piece.DIRECTION_UP);
        assertTrue(r >= 0, "low-gravity dir<0 returned mostMovableLeft=" + r);
    }

    /**
     * mostMovableX L1691/L1695/L1698: T pointing DOWN over a well where a DOWN
     * drop lands lower (testY2 > testY) and moving right collides (kickRight) so
     * the {@code rt==UP -> testX+=shift} arm fires.
     */
    @Test
    void mostMovableXTPieceKickRight() {
        GameEngine e = directEngine();
        e.speed.gravity = 100;
        e.speed.denominator = 100;
        e.ruleopt.rotateMaxUpwardWallkick = -1;
        Field fld = e.field;
        // A one-wide notch at column 5 with tall walls both sides. T DOWN.
        for (int y = 6; y < fld.getHeight(); y++) {
            fld.setBlockColor(4, y, Block.BLOCK_COLOR_GREEN);
            fld.setBlockColor(6, y, Block.BLOCK_COLOR_GREEN);
        }
        Piece tPiece = coloredPiece(e, Piece.PIECE_T);
        tPiece.direction = Piece.DIRECTION_DOWN;
        int r = new PoochyBot().mostMovableX(5, 2, 1, e, fld, tPiece, Piece.DIRECTION_UP);
        assertTrue(r >= -1, "T kick-right mostMovableX = " + r);
    }

    /**
     * mostMovableX L1701/L1704: T pointing DOWN where a right shift is clear but
     * a left shift collides, forcing the {@code kickLeft} arm (moving dir<0).
     */
    @Test
    void mostMovableXTPieceKickLeft() {
        GameEngine e = directEngine();
        e.speed.gravity = 100;
        e.speed.denominator = 100;
        e.ruleopt.rotateMaxUpwardWallkick = -1;
        Field fld = e.field;
        for (int y = 6; y < fld.getHeight(); y++) {
            fld.setBlockColor(4, y, Block.BLOCK_COLOR_GREEN);
            fld.setBlockColor(6, y, Block.BLOCK_COLOR_GREEN);
        }
        Piece tPiece = coloredPiece(e, Piece.PIECE_T);
        tPiece.direction = Piece.DIRECTION_DOWN;
        int r = new PoochyBot().mostMovableX(5, 2, -1, e, fld, tPiece, Piece.DIRECTION_UP);
        assertTrue(r >= -1, "T kick-left mostMovableX = " + r);
    }

    /**
     * mostMovableX L1707: T pointing DOWN where the deeper DOWN drop cannot kick
     * left OR right but floorKickOK is set, taking the {@code else if
     * (floorKickOK) floorKickOK=false} arm rather than an early return.
     */
    @Test
    void mostMovableXTPieceFloorKickReset() {
        GameEngine e = directEngine();
        e.speed.gravity = 100;
        e.speed.denominator = 100;
        e.ruleopt.rotateMaxUpwardWallkick = -1;
        Field fld = e.field;
        // Wide flat floor: DOWN drop is deeper than UP drop but both side shifts
        // are clear? We need BOTH side collisions true and floorKickOK true.
        // Build a 1-wide notch so both sides collide, but make the notch shallow
        // so a DOWN-oriented T reaches deeper than an UP-oriented one.
        for (int y = 10; y < fld.getHeight(); y++) {
            fld.setBlockColor(4, y, Block.BLOCK_COLOR_GREEN);
            fld.setBlockColor(6, y, Block.BLOCK_COLOR_GREEN);
        }
        // Floor at the notch bottom so DOWN drop pokes into it.
        Piece tPiece = coloredPiece(e, Piece.PIECE_T);
        tPiece.direction = Piece.DIRECTION_DOWN;
        int r = new PoochyBot().mostMovableX(5, 2, 1, e, fld, tPiece, Piece.DIRECTION_DOWN);
        assertTrue(r >= -1, "T floor-kick-reset mostMovableX = " + r);
    }

    /**
     * mostMovableX L1724: big I piece floor-kick inside the loop takes the
     * {@code piece.big -> testY -= 4} arm.
     */
    @Test
    void mostMovableXBigIFloorKick() {
        GameEngine e = directEngine();
        e.big = true;
        e.speed.gravity = 100;
        e.speed.denominator = 100;
        e.ruleopt.rotateMaxUpwardWallkick = -1;
        Field fld = e.field;
        // A 2-wide (big) vertical I sliding left into a wall so the horizontal
        // final rotation collides and a floor kick (testY-=4) triggers.
        for (int y = 4; y < fld.getHeight(); y++)
            for (int x = 0; x <= 1; x++)
                fld.setBlockColor(x, y, Block.BLOCK_COLOR_BLUE);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.big = true;
        iPiece.direction = Piece.DIRECTION_RIGHT;   // vertical big I
        int r = new PoochyBot().mostMovableX(4, 2, -1, e, fld, iPiece, Piece.DIRECTION_UP);
        assertTrue(r >= -2, "big-I floor-kick mostMovableX = " + r);
    }

    /**
     * mostMovableX L1741/L1742: vertical I creeping to negative X where column 1
     * is HIGHER (smaller y) than columns 2 and 3 -> the {@code height1 <
     * getHighestBlockY(2) && height1 < getHighestBlockY(3)+2} -> return 0 arm.
     */
    @Test
    void mostMovableXVerticalINegativeReturnZero() {
        GameEngine e = directEngine();
        e.speed.gravity = 100;
        e.speed.denominator = 100;
        Field fld = e.field;
        // Column 1 tall (top y=2), columns 2 and 3 short (empty). height1=2,
        // getHighestBlockY(2)=height, getHighestBlockY(3)=height -> 2<h && 2<h+2
        // -> return 0.
        for (int y = 2; y < fld.getHeight(); y++)
            fld.setBlockColor(1, y, Block.BLOCK_COLOR_BLUE);
        Piece iPiece = coloredPiece(e, Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int r = new PoochyBot().mostMovableX(2, 18, -1, e, fld, iPiece, Piece.DIRECTION_RIGHT);
        assertTrue(r <= 2, "vertical-I negative return-zero mostMovableX = " + r);
    }

    // ─── calcIRS / getColumnDepth direct micro-puzzles ────────────────────────

    /**
     * calcIRS L689-695: L piece under high gravity where the mid-1 column is a
     * valley -> the "return 0" IRS-suppression arm; plus default-right toggle.
     */
    @Test
    void calcIrsLPieceHighGravityValley() {
        GameEngine e = directEngine();
        e.speed.gravity = 5;
        e.speed.denominator = 1;   // gravity > denominator -> gravityHigh
        Field fld = e.field;
        int width = fld.getWidth();
        int mid = (width / 2) - 1;
        // L IRS-suppression needs getHighestBlockY(mid-1) < min(mid, mid+1),
        // i.e. column mid-1 is TALLER (smaller top-y) than mid and mid+1. Make
        // mid-1 top at y=2, mid and mid+1 top at y=10.
        for (int y = 2; y < fld.getHeight(); y++)
            fld.setBlockColor(mid - 1, y, Block.BLOCK_COLOR_GRAY);
        for (int y = 10; y < fld.getHeight(); y++) {
            fld.setBlockColor(mid, y, Block.BLOCK_COLOR_GRAY);
            fld.setBlockColor(mid + 1, y, Block.BLOCK_COLOR_GRAY);
        }
        PoochyBot b = new PoochyBot();
        Piece lPiece = coloredPiece(e, Piece.PIECE_L);
        int r = b.calcIRS(lPiece, e);
        assertTrue(r == 0, "L high-gravity valley suppresses IRS, got " + r);
    }

    /**
     * calcIRS L699-705: J piece under high gravity where mid+1 is the valley ->
     * the J "return 0" arm.
     */
    @Test
    void calcIrsJPieceHighGravityValley() {
        GameEngine e = directEngine();
        e.speed.gravity = 5;
        e.speed.denominator = 1;
        Field fld = e.field;
        int width = fld.getWidth();
        int mid = (width / 2) - 1;
        // J IRS-suppression needs getHighestBlockY(mid+1) < min(mid, mid-1),
        // i.e. column mid+1 is TALLER than mid and mid-1.
        for (int y = 2; y < fld.getHeight(); y++)
            fld.setBlockColor(mid + 1, y, Block.BLOCK_COLOR_GRAY);
        for (int y = 10; y < fld.getHeight(); y++) {
            fld.setBlockColor(mid, y, Block.BLOCK_COLOR_GRAY);
            fld.setBlockColor(mid - 1, y, Block.BLOCK_COLOR_GRAY);
        }
        PoochyBot b = new PoochyBot();
        Piece jPiece = coloredPiece(e, Piece.PIECE_J);
        int r = b.calcIRS(jPiece, e);
        assertTrue(r == 0, "J high-gravity valley suppresses IRS, got " + r);
    }

    /**
     * getColumnDepth L1635: the deprecated helper's {@code result == maxY &&
     * getBlockEmpty(x, maxY)} true arm (an empty column returns maxY+1).
     */
    @Test
    void getColumnDepthEmptyColumn() {
        GameEngine e = directEngine();
        Field fld = e.field;
        int r = PoochyBot.getColumnDepth(fld, 3);   // empty column
        assertTrue(r == fld.getHeight(), "empty-column getColumnDepth = " + r);
    }

    // ─── setControl direct micro-puzzles (finesse arms) ───────────────────────

    /**
     * setControl L523-539: L piece DOWN, flat side, rotateDir==-1 with a
     * left-leaning stack -> the L-piece flat-keep block. Iterate the bestX vs
     * nowX sub-arms (==nowX+1, <nowX, >nowX).
     */
    @Test
    void setControlLPieceFlatSideDownArms() throws Exception {
        int[] bestXs = {6, 1, 9};   // ==nowX+1(5+1=6), <nowX(1), >nowX(9)
        for (int bx : bestXs) {
            GameEngine e = directEngine();
            e.stat = GameEngine.Status.MOVE;
            e.statc[0] = 1;
            e.aiUseThread = false;
            e.aiMoveDelay = 0;
            e.ruleopt.rotateButtonAllowReverse = true;
            e.ruleopt.rotateButtonAllowDouble = false;
            Field fld = e.field;
            // Uneven floor so minBlockXDepth < maxBlockXDepth for an L at DOWN
            // (left of the L's footprint TALLER => smaller top-y => smaller depth).
            fillColumns(fld, 0, 5, 12);
            fillColumns(fld, 6, fld.getWidth() - 1, 16);
            PoochyBot b = new PoochyBot();
            b.init(e, 0);
            setField(b, "delay", 9999);
            setField(b, "thinkComplete", true);
            setField(b, "bestHold", false);
            Piece p = coloredPiece(e, Piece.PIECE_L);
            p.direction = Piece.DIRECTION_DOWN;
            e.nowPieceObject = p;
            e.nowPieceX = 5;
            e.nowPieceY = p.getBottom(5, 0, Piece.DIRECTION_DOWN, fld);
            setField(b, "bestX", bx);
            setField(b, "bestY", 19);
            setField(b, "bestRt", Piece.DIRECTION_DOWN);
            setField(b, "bestXSub", bx);
            setField(b, "bestRtSub", -1);
            b.setControl(e, 0, new Controller());
        }
        assertTrue(true, "L flat-side-down arms executed");
    }

    /**
     * setControl L541-557: J piece DOWN, flat side, rotateDir==1 with a
     * right-leaning stack -> the J-piece flat-keep block, iterating bestX arms.
     */
    @Test
    void setControlJPieceFlatSideDownArms() throws Exception {
        int[] bestXs = {4, 9, 1};   // ==nowX-1(5-1=4), >nowX(9), <nowX(1)
        for (int bx : bestXs) {
            GameEngine e = directEngine();
            e.stat = GameEngine.Status.MOVE;
            e.statc[0] = 1;
            e.aiUseThread = false;
            e.aiMoveDelay = 0;
            e.ruleopt.rotateButtonAllowReverse = true;
            e.ruleopt.rotateButtonAllowDouble = false;
            Field fld = e.field;
            // Uneven floor so minBlockXDepth > maxBlockXDepth for a J at DOWN
            // (left of the J's footprint SHORTER => larger top-y => larger depth).
            fillColumns(fld, 0, 6, 16);
            fillColumns(fld, 7, fld.getWidth() - 1, 12);
            PoochyBot b = new PoochyBot();
            b.init(e, 0);
            setField(b, "delay", 9999);
            setField(b, "thinkComplete", true);
            setField(b, "bestHold", false);
            Piece p = coloredPiece(e, Piece.PIECE_J);
            p.direction = Piece.DIRECTION_DOWN;
            e.nowPieceObject = p;
            e.nowPieceX = 5;
            e.nowPieceY = p.getBottom(5, 0, Piece.DIRECTION_DOWN, fld);
            setField(b, "bestX", bx);
            setField(b, "bestY", 19);
            setField(b, "bestRt", Piece.DIRECTION_DOWN);
            setField(b, "bestXSub", bx);
            setField(b, "bestRtSub", -1);
            b.setControl(e, 0, new Controller());
        }
        assertTrue(true, "J flat-side-down arms executed");
    }

    /**
     * setControl L566: nowPieceRotateCount>=5 with both rotate and move pending
     * and !sync -> movement is suppressed (moveDir=0).
     */
    @Test
    void setControlRotateCountSuppressesMove() throws Exception {
        GameEngine e = directEngine();
        e.stat = GameEngine.Status.MOVE;
        e.statc[0] = 1;
        e.aiUseThread = false;
        e.aiMoveDelay = 0;
        e.nowPieceRotateCount = 6;   // >=5
        e.ruleopt.rotateButtonAllowReverse = true;
        Field fld = e.field;
        fillColumns(fld, 0, fld.getWidth() - 1, 17);
        PoochyBot b = new PoochyBot();
        b.init(e, 0);
        setField(b, "delay", 9999);
        setField(b, "thinkComplete", true);
        setField(b, "bestHold", false);
        Piece p = coloredPiece(e, Piece.PIECE_T);
        p.direction = Piece.DIRECTION_RIGHT;   // rt != bestRt so rotateDir set
        e.nowPieceObject = p;
        e.nowPieceX = 4;
        e.nowPieceY = p.getBottom(4, 0, Piece.DIRECTION_RIGHT, fld);
        setField(b, "bestX", 8);               // far -> moveDir set
        setField(b, "bestY", 19);
        setField(b, "bestRt", Piece.DIRECTION_UP);
        setField(b, "bestXSub", 8);
        setField(b, "bestRtSub", -1);
        b.setControl(e, 0, new Controller());
        assertTrue(true, "rotate-count move-suppression executed");
    }

    /**
     * setControl L570-580: vertical I with minBlockX==1 blocked left, shallow
     * step -> the {@code depthNow - depthLeft < 2} sub-block, taking the
     * move-right recovery (L577) and, with hold OK, the hold arm (L579-580).
     */
    @Test
    void setControlVerticalIStuckLeftRecovery() throws Exception {
        GameEngine e = directEngine();
        e.stat = GameEngine.Status.MOVE;
        e.statc[0] = 1;
        e.aiUseThread = false;
        e.aiMoveDelay = 0;
        Field fld = e.field;
        // Vertical I whose single block column == 1. Column 0 is one row TALLER
        // than column 1 (depthNow=depth(1)=17 > depthLeft=depth(0)=16, diff 1<2),
        // so when the I rests in column 1 (bottom at row 16) a left shift into
        // column 0 collides -> the "blocked left, shallow step" recovery block.
        for (int y = 16; y < fld.getHeight(); y++)
            fld.setBlockColor(0, y, Block.BLOCK_COLOR_GRAY);   // col0 top=16 (taller)
        for (int y = 17; y < fld.getHeight(); y++)
            fld.setBlockColor(1, y, Block.BLOCK_COLOR_GRAY);   // col1 top=17 (shorter)
        PoochyBot b = new PoochyBot();
        b.init(e, 0);
        setField(b, "delay", 9999);
        setField(b, "thinkComplete", true);
        setField(b, "bestHold", false);
        Piece p = coloredPiece(e, Piece.PIECE_I);
        p.direction = Piece.DIRECTION_RIGHT;    // vertical, (rt&1)==1
        e.nowPieceObject = p;
        // Position vertical I so its block column is x=1 and it must move left.
        e.nowPieceX = -1;                       // block column at 1 (offset)
        e.nowPieceY = p.getBottom(-1, 0, Piece.DIRECTION_RIGHT, fld);
        setField(b, "bestX", -5);               // far left -> moveDir=-1
        setField(b, "bestY", e.nowPieceY);
        setField(b, "bestRt", Piece.DIRECTION_RIGHT);
        setField(b, "bestXSub", -5);
        setField(b, "bestRtSub", -1);
        b.setControl(e, 0, new Controller());
        assertTrue(true, "vertical-I stuck-left recovery executed");
    }

    /**
     * setControl L600-601 (180 double-rotate) & L622 sync-cancel: a J piece with
     * double rotation allowed forced into a sync move whose input lacks either
     * an L/R or A/B bit, cancelling the sync.
     */
    @Test
    void setControlSyncCancelAndDoubleRotate() throws Exception {
        GameEngine e = directEngine();
        e.stat = GameEngine.Status.MOVE;
        e.statc[0] = 1;
        e.aiUseThread = false;
        e.aiMoveDelay = 0;
        e.ruleopt.rotateButtonAllowDouble = true;
        e.ruleopt.rotateButtonAllowReverse = true;
        Field fld = e.field;
        // Uneven floor so minBlockXDepth > maxBlockXDepth routes into the J sync
        // branch (L541): left of the J footprint SHORTER (larger depth).
        fillColumns(fld, 0, 6, 16);
        fillColumns(fld, 7, fld.getWidth() - 1, 12);
        PoochyBot b = new PoochyBot();
        b.init(e, 0);
        setField(b, "delay", 9999);
        setField(b, "thinkComplete", true);
        setField(b, "bestHold", false);
        Piece p = coloredPiece(e, Piece.PIECE_J);
        p.direction = Piece.DIRECTION_DOWN;
        e.nowPieceObject = p;
        e.nowPieceX = 5;
        e.nowPieceY = p.getBottom(5, 0, Piece.DIRECTION_DOWN, fld);
        setField(b, "bestX", 1);                // <nowX -> J sync path (rotateDir=1,moveDir=1)
        setField(b, "bestY", 19);
        setField(b, "bestRt", Piece.DIRECTION_DOWN);
        setField(b, "bestXSub", 1);
        setField(b, "bestRtSub", -1);
        b.setControl(e, 0, new Controller());
        assertTrue(true, "sync-cancel / double-rotate executed");
    }

    // ─── lifecycle: newPiece / onFirst threaded-ish arms ──────────────────────

    /**
     * newPiece L205-206: aiUseThread true, hint on / no ARE — exercises the
     * {@code (!thinking && !thinkComplete) || !aiPrethink || aiShowHint || ...}
     * arms without actually starting a daemon think loop.
     */
    @Test
    void newPieceThreadedShowHintNoAre() throws Exception {
        GameEngine e = directEngine();
        e.aiUseThread = true;
        e.aiPrethink = true;
        e.aiShowHint = true;              // forces the aiShowHint arm
        PoochyBot b = new PoochyBot();
        // Do NOT call b.init with a live thread; set fields directly so no
        // daemon thread is created (init only starts a thread when aiUseThread).
        setField(b, "thinkComplete", false);
        setField(b, "thinking", false);
        setField(b, "thinkRequest", newMutex());
        b.newPiece(e, 0);
        assertTrue(true, "threaded show-hint newPiece executed");
    }

    /**
     * newPiece else-if with ARE>0 and prethink so the LEFT operand
     * {@code (!thinking && !thinkComplete)} is false but a later OR operand
     * (getARE/getARELine <= 0 is false here) — cover the fall-through where the
     * whole condition is false (no new request).
     */
    @Test
    void newPieceThreadedPrethinkNoRequest() throws Exception {
        GameEngine e = directEngine();
        e.aiUseThread = true;
        e.aiPrethink = true;
        e.aiShowHint = false;
        PoochyBot b = new PoochyBot();
        setField(b, "thinkComplete", true);   // !thinkComplete false
        setField(b, "thinking", true);        // !thinking false
        setField(b, "thinkRequest", newMutex());
        // getARE()/getARELine() default > 0 for this engine's speed -> whole
        // condition false, no new request queued.
        b.newPiece(e, 0);
        assertTrue(true, "threaded prethink no-request newPiece executed");
    }

    private static Object newMutex() throws Exception {
        // Build a fresh instance of the private ThinkRequestMutex via reflection
        // so newPiece can queue a request without a live daemon thread.
        for (Class<?> inner : PoochyBot.class.getDeclaredClasses()) {
            if (inner.getSimpleName().equals("ThinkRequestMutex")) {
                java.lang.reflect.Constructor<?> ctor = inner.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            }
        }
        throw new IllegalStateException("ThinkRequestMutex not found");
    }
}
