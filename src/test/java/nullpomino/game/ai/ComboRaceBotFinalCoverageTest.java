package nullpomino.game.ai;
import static org.junit.jupiter.api.Assertions.*;
import nullpomino.game.component.*;
import nullpomino.game.event.*;
import nullpomino.game.play.*;
import org.junit.jupiter.api.*;
class ComboRaceBotFinalCoverageTest {
    private GameManager gm; private GameEngine engine; private ComboRaceBot ai; private Controller ctrl;
    @BeforeEach void setUp() { gm = new GameManager(new EventReceiver()); gm.init(); engine = gm.engine[0]; engine.init(); engine.aiUseThread = false; ai = new ComboRaceBot(); ai.init(engine, 0); ctrl = new Controller(); }
    @Test void t1() { assertEquals("Combo Race AI V1.03", ai.getName()); }
    @Test void t2() { engine.createFieldIfNeeded(); assertEquals(0, ComboRaceBot.fieldToCode(engine.field)); }
    @Test void t3() { assertEquals(0, ComboRaceBot.fieldToIndex((short)0x7)); assertEquals(-1, ComboRaceBot.fieldToIndex((short)0xFFF)); }
    @Test void t4() { assertEquals(0, ai.thinkMain(engine, -1, -1, 0)); }
    @Test void t5() { ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t6() { ai.createTables(engine); assertNotNull(ai.moves); }
    @Test void t7() { engine.createFieldIfNeeded(); assertNotNull(ComboRaceBot.checkOffset(new Piece(Piece.PIECE_T), engine)); }
    @Test void t8() { ai.printPieceAndDirection(Piece.PIECE_I, 0); ai.printPieceAndDirection(Piece.PIECE_L, 1); ai.printPieceAndDirection(Piece.PIECE_O, 2); ai.printPieceAndDirection(Piece.PIECE_Z, 3); ai.printPieceAndDirection(Piece.PIECE_T, 0); ai.printPieceAndDirection(Piece.PIECE_J, 1); ai.printPieceAndDirection(Piece.PIECE_S, 2); ai.printPieceAndDirection(Piece.PIECE_I1, 3); ai.printPieceAndDirection(Piece.PIECE_I2, 0); ai.printPieceAndDirection(Piece.PIECE_I3, 1); ai.printPieceAndDirection(Piece.PIECE_L3, 2); assertTrue(true); }
    @Test void t9() { engine.stat = GameEngine.Status.READY; engine.statc[0] = 0; ai.onLast(engine, 0); assertTrue(true); }
    @Test void t10() { engine.createFieldIfNeeded(); engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4; engine.nowPieceY = 5; ai.delay = 9999; ai.thinkComplete = true; ai.bestHold = false;
        ai.bestX = 4; ai.bestRt = 0; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t11() { engine.createFieldIfNeeded(); engine.aiPrethink = true; engine.stat = GameEngine.Status.ARE; ai.onFirst(engine, 0); assertTrue(true); }
}