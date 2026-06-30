package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.tool.airankstool.AIRanksConstants;
import nullpomino.tool.airankstool.Ranks;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Targets the remaining uncovered lines in {@link RanksAI}:
 *
 * <ul>
 *   <li>{@code initRanks} ranks-file loading paths: a non-empty config
 *       file name (line 146) and the {@code ObjectInputStream} try/catch
 *       block - successful read, {@code FileNotFoundException} and
 *       {@code IOException} (lines 152-161).</li>
 *   <li>{@code playFictitiousMove} hold path (line 342).</li>
 *   <li>engine {@code thinkBestPosition} {@code threadRunning=false}
 *       when no move scores (line 428).</li>
 *   <li>The "MAIN (4 Lines) new best piece" branch in the array
 *       {@code thinkBestPosition} (lines 561, 563-568).</li>
 *   <li>The deep "SUB (4 Lines) new best piece" branch in {@code thinkMain}
 *       (lines 735, 737).</li>
 *   <li>The {@code run()} thread routine - successful think (line 787)
 *       and the {@code catch} on a failing think (line 791).</li>
 * </ul>
 *
 * <p>All scoring is driven through a hand-built {@link Ranks} table so the
 * comparisons are deterministic: a fresh {@code Ranks(4,9)} returns
 * {@code Integer.MAX_VALUE} for every surface, which makes the "new best"
 * branches impossible to reach. We reflectively overwrite the internal
 * rank array with a low constant and bump exactly the surface that the
 * winning (4-line) move produces.
 */
class RanksAICoverageBoostTest {

    private GameManager gm;
    private GameEngine engine;
    private RanksAI ai;
    private Ranks ranks;

    // Saved statics so config-file tests don't leak into other tests.
    private String savedConfigFile;
    private String savedDir;

    @BeforeEach
    void setUp() throws Exception {
        savedConfigFile = AIRanksConstants.RANKSAI_CONFIG_FILE;
        savedDir = AIRanksConstants.RANKSAI_DIR;

        gm = new GameManager(new EventReceiver());
        gm.init();
        engine = gm.engine[0];
        engine.init();
        engine.createFieldIfNeeded();
        engine.statistics.time = 1;
        engine.nextPieceArraySize = 3;
        engine.nextPieceArrayID = new int[]{Piece.PIECE_S, Piece.PIECE_S, Piece.PIECE_S};
        engine.nextPieceArrayObject = new Piece[]{
            new Piece(Piece.PIECE_S), new Piece(Piece.PIECE_S), new Piece(Piece.PIECE_S)};
        engine.nextPieceCount = 0;

        ai = new RanksAI();
        ranks = new Ranks(4, 9);
        setPrivateField(ai, "ranks", ranks);
        setPrivateField(ai, "heights", new int[9]);
        setPrivateField(ai, "MAX_PREVIEWS", 0);
        setPrivateField(ai, "allowHold", false);
        setPrivateField(ai, "gameOver", false);
        setPrivateField(ai, "speedLimit", 0);
    }

    @AfterEach
    void tearDown() {
        AIRanksConstants.RANKSAI_CONFIG_FILE = savedConfigFile;
        AIRanksConstants.RANKSAI_DIR = savedDir;
    }

    private static void setPrivateField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Object getPrivateField(Object obj, String name) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }

    /** Returns the internal int[] rank table of a Ranks instance. */
    private static int[] rankArrayOf(Ranks r) throws Exception {
        Field f = Ranks.class.getDeclaredField("ranks");
        f.setAccessible(true);
        return (int[]) f.get(r);
    }

    /**
     * Fills the whole rank table with {@code low} and sets the surface that
     * {@code winningHeights} encodes to {@code high}, so a move resulting in
     * that exact surface beats everything else.
     */
    private void riggedRanks(int[] winningHeights, int low, int high) throws Exception {
        int[] table = rankArrayOf(ranks);
        Arrays.fill(table, low);
        int idx = ranks.encode(ranks.heightsToSurface(winningHeights));
        table[idx] = high;
    }

    // ─── initRanks: non-empty file name + ObjectInputStream paths ──────────

    @Test
    void initRanksLoadsSerializedRanksFromFile() throws Exception {
        // Write a real serialized Ranks object and load it back.
        Path dir = Files.createTempDirectory("ranksai_ok");
        String fileName = "ranks.ser";
        Path file = dir.resolve(fileName);
        try (ObjectOutputStream out =
                new ObjectOutputStream(new FileOutputStream(file.toFile()))) {
            out.writeObject(new Ranks(4, 9));
        }

        Path cfg = Files.createTempFile("ranksai_cfg", ".cfg");
        Files.writeString(cfg, "ranksai.file=" + fileName + "\n");

        AIRanksConstants.RANKSAI_CONFIG_FILE = cfg.toString();
        AIRanksConstants.RANKSAI_DIR = dir.toString() + "/";

        RanksAI fresh = new RanksAI();
        fresh.initRanks(); // line 146 + try/readObject (152-154)

        assertNotNull(getPrivateField(fresh, "ranks"),
                "ranks should be deserialized from the file");
    }

    @Test
    void initRanksFileNotFoundFallsBackToDefault() throws Exception {
        Path dir = Files.createTempDirectory("ranksai_missing");

        Path cfg = Files.createTempFile("ranksai_cfg", ".cfg");
        Files.writeString(cfg, "ranksai.file=does_not_exist.ser\n");

        AIRanksConstants.RANKSAI_CONFIG_FILE = cfg.toString();
        AIRanksConstants.RANKSAI_DIR = dir.toString() + "/";

        RanksAI fresh = new RanksAI();
        fresh.initRanks(); // line 146 + FileNotFoundException -> new Ranks (155-156)

        assertNotNull(getPrivateField(fresh, "ranks"),
                "ranks should fall back to a fresh Ranks on missing file");
    }

    @Test
    void initRanksCorruptFileHitsIOExceptionCatch() throws Exception {
        // An existing file that is NOT a valid object stream -> the
        // ObjectInputStream constructor throws an IOException (bad header).
        Path dir = Files.createTempDirectory("ranksai_bad");
        String fileName = "corrupt.ser";
        Path file = dir.resolve(fileName);
        Files.writeString(file, "this is not a serialized java object stream");

        Path cfg = Files.createTempFile("ranksai_cfg", ".cfg");
        Files.writeString(cfg, "ranksai.file=" + fileName + "\n");

        AIRanksConstants.RANKSAI_CONFIG_FILE = cfg.toString();
        AIRanksConstants.RANKSAI_DIR = dir.toString() + "/";

        RanksAI fresh = new RanksAI();
        // ranks starts null; on IOException ranks is left untouched, but the
        // subsequent heights init needs a non-null ranks, so the IOException
        // catch (157-158) runs and then heights init would NPE. Pre-seed ranks
        // so initRanks completes, while still exercising the IOException catch.
        setPrivateField(fresh, "ranks", new Ranks(4, 9));
        setPrivateField(fresh, "currentRanksFile", "force-reload");

        fresh.initRanks(); // line 146 + IOException catch (157-158)

        assertNotNull(getPrivateField(fresh, "ranks"),
                "ranks should remain set after an IOException");
    }

    @Test
    void initRanksSerializedMissingClassHitsClassNotFoundCatch() throws Exception {
        // Serialize a real Ranks object, then rewrite its class descriptor to a
        // same-length class name that is not on the test classpath. readObject()
        // catches ClassNotFoundException (lines 159-160), then initRanks reaches
        // the existing null-ranks dereference at heights initialization.
        Path dir = Files.createTempDirectory("ranksai_missing_class");
        String fileName = "missing-class.ser";
        Path file = dir.resolve(fileName);

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(new Ranks(4, 9));
        }
        byte[] raw = bytes.toByteArray();
        byte[] from = "nullpomino.tool.airankstool.Ranks"
                .getBytes(StandardCharsets.ISO_8859_1);
        byte[] to = "nopepomino.tool.airankstool.Ranks"
                .getBytes(StandardCharsets.ISO_8859_1);
        assertTrue(from.length == to.length, "replacement class name must keep stream length");
        int start = -1;
        outer:
        for (int i = 0; i <= raw.length - from.length; i++) {
            for (int j = 0; j < from.length; j++)
                if (raw[i + j] != from[j])
                    continue outer;
            start = i;
            break;
        }
        assertTrue(start >= 0, "serialized Ranks class descriptor should be present");
        System.arraycopy(to, 0, raw, start, to.length);
        Files.write(file, raw);

        Path cfg = Files.createTempFile("ranksai_cfg", ".cfg");
        Files.writeString(cfg, "ranksai.file=" + fileName + "\n");

        AIRanksConstants.RANKSAI_CONFIG_FILE = cfg.toString();
        AIRanksConstants.RANKSAI_DIR = dir.toString() + "/";

        RanksAI fresh = new RanksAI();
        assertThrows(NullPointerException.class, fresh::initRanks);
    }

    // ─── playFictitiousMove: hold path (line 342) ─────────────────────────

    @Test
    void playFictitiousMoveTakesHoldPath() throws Exception {
        setPrivateField(ai, "allowHold", true);

        // Flat field at height 5. The current S piece fits nowhere on a flat
        // surface (no rotation matches), so the no-hold branch scores 0. The
        // held O piece does fit, giving rankStacking != 0, so the hold move
        // wins -> bestHold == true -> line 342 (holdOK[0] = false).
        int[] heights = new int[9];
        Arrays.fill(heights, 5);
        int[] pieces = {Piece.PIECE_S, Piece.PIECE_S, Piece.PIECE_S};
        int[] holdPiece = {Piece.PIECE_O};
        boolean[] holdOK = {true};

        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);

        assertTrue(ai.bestHold, "hold move should win when current piece never fits");
    }

    // ─── engine thinkBestPosition: threadRunning=false (line 428) ─────────

    @Test
    void engineThinkBestPositionClearsThreadRunningWhenNoMove() throws Exception {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        // S piece on an empty (flat, height 0) field fits nowhere -> the best
        // score stays rankStacking == 0 -> line 428 sets threadRunning=false.
        engine.nowPieceObject = new Piece(Piece.PIECE_S);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_S],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_S]);
        engine.nowPieceX = 4;
        engine.nowPieceY = 0;
        engine.holdPieceObject = null;
        setPrivateField(ai, "allowHold", false);
        ai.threadRunning = true;

        ai.thinkBestPosition(engine, 0);

        assertTrue(((RanksAI.Score) getPrivateField(ai, "bestScore")).rankStacking == 0,
                "no fitting move means rankStacking 0");
        assertTrue(!ai.threadRunning, "threadRunning should be cleared (line 428)");
    }

    // ─── array thinkBestPosition: MAIN (4 Lines) new best (561, 563-568) ──

    @Test
    void arrayThinkBestPositionFourLineBecomesBest() throws Exception {
        // Flat field at height 4: force-tetris is skipped (min 4 < threshold 8)
        // but the 4-line branch is allowed (min 4 >= 4). Every regular I
        // placement leaves a non-flat surface (low rank), while the tetris move
        // (subtract 4) leaves an all-flat field. We rig the all-flat surface to
        // the high rank so the 4-line score beats the regular best -> lines
        // 561, 563-568 execute.
        int[] flat = new int[9]; // tetris result: all zeros
        riggedRanks(flat, /*low*/ 1, /*high*/ Integer.MAX_VALUE / 2);

        int[] heights = new int[9];
        Arrays.fill(heights, 4);
        int[] pieces = {Piece.PIECE_I, Piece.PIECE_I, Piece.PIECE_I};
        int[] holdPiece = {-1};

        ai.thinkBestPosition(heights, pieces, holdPiece, false);

        // The 4-line move puts the I piece in the rightmost (10th) column.
        org.junit.jupiter.api.Assertions.assertEquals(9, ai.bestX,
                "4-line move should be chosen (rightmost column)");
        org.junit.jupiter.api.Assertions.assertEquals(1, ai.bestRt,
                "4-line move should be vertical (rt=1)");
        assertTrue(((RanksAI.Score) getPrivateField(ai, "bestScore")).rankStacking
                        == Integer.MAX_VALUE / 2,
                "4-line score should be the rigged high value");
    }

    // ─── thinkMain: deep SUB (4 Lines) new best (735, 737) ────────────────

    @Test
    void thinkMainDeepSubFourLineBecomesBest() throws Exception {
        // Outer move: O piece at x=0 on a flat height-4 field -> heightsWork
        // becomes {6,6,4,4,4,4,4,4,4}, heightMin = 4 (>=4, < threshold 8) so the
        // recursive call's force-4-line-sub (line 664) is NOT taken, and we land
        // in the else branch where the deep SUB (4 Lines) tetris check lives.
        // The next piece (pieces[1]) is I; its vertical tetris move subtracts 4
        // from heightsWork -> {2,2,0,0,0,0,0,0,0}. We rig exactly that surface to
        // the high rank so the SUB 4-line score becomes the best -> lines
        // 735, 737 execute.
        int[] tetrisResult = {2, 2, 0, 0, 0, 0, 0, 0, 0};
        riggedRanks(tetrisResult, /*low*/ 1, /*high*/ Integer.MAX_VALUE / 2);

        int[] heights = new int[9];
        Arrays.fill(heights, 4);
        int[] pieces = {Piece.PIECE_O, Piece.PIECE_I, Piece.PIECE_I, Piece.PIECE_I};
        int[] holdPiece = {-1};

        // numPreviews = 1 so thinkMain recurses one level into the SUB branch.
        RanksAI.Score score = ai.thinkMain(0, 0, heights, pieces, holdPiece, false, 1);

        assertNotNull(score, "thinkMain should return a score");
        assertTrue(score.rankStacking == Integer.MAX_VALUE / 2,
                "deep SUB 4-line move should propagate the rigged high score");
    }

    // ─── run(): successful think (line 787) ───────────────────────────────

    @Test
    void runRoutineSucceedsThenSelfTerminates() throws Exception {
        // S piece on an empty field scores 0 -> thinkBestPosition sets
        // threadRunning=false (line 428), which lets the run() loop exit after a
        // single successful think (line 787). thinkDelay 0 avoids any sleep.
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_S);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_S],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_S]);
        engine.nowPieceX = 4;
        engine.nowPieceY = 0;
        engine.holdPieceObject = null;
        setPrivateField(ai, "allowHold", false);

        ai.gEngine = engine;
        engine.playerID = 0;
        ai.thinkRequest = true;
        ai.thinkDelay = 0;
        ai.threadRunning = true;

        // run() self-terminates because the (failing-to-place) think clears
        // threadRunning via line 428.
        ai.run();

        assertTrue(!ai.threadRunning, "run() should have exited cleanly");
        assertTrue(!ai.thinking, "thinking flag should be reset after run");
    }

    // ─── run(): failing think hits the catch (line 791) ───────────────────

    @Test
    void runRoutineCatchesThinkFailure() throws Exception {
        // ranks == null makes thinkBestPosition throw a NullPointerException,
        // exercising the catch at line 791. The loop would otherwise spin
        // forever (threadRunning stays true), so we run it on a daemon thread
        // and interrupt it out of its think-delay sleep.
        setPrivateField(ai, "ranks", null);
        ai.gEngine = engine;
        engine.playerID = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4;
        engine.nowPieceY = 0;
        ai.thinkRequest = true;
        ai.thinkDelay = 100000; // long sleep -> we interrupt out of it
        ai.threadRunning = true;

        Thread t = new Thread(ai, "ranksai-run-test");
        t.setDaemon(true);
        t.start();

        // The first iteration synchronously runs (and fails) the think, then
        // enters Thread.sleep(thinkDelay). Interrupt to break out (line 800).
        for (int i = 0; i < 50 && t.isAlive(); i++) {
            t.interrupt();
            t.join(50);
        }
        ai.threadRunning = false;
        t.interrupt();
        t.join(2000);

        assertTrue(!t.isAlive(), "run thread should have terminated after the catch");
    }
}
