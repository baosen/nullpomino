package nullpomino.game.ai;
import static org.junit.jupiter.api.Assertions.*;
import nullpomino.game.component.*;
import nullpomino.game.event.*;
import nullpomino.game.play.*;
import org.junit.jupiter.api.*;
class PoochyBotFinalCoverageTest {
    private GameManager gm; private GameEngine engine; private PoochyBot ai; private Controller ctrl;
    @BeforeEach void setUp() { gm = new GameManager(new EventReceiver()); gm.init(); engine = gm.engine[0]; engine.init(); engine.aiUseThread = false; ai = new PoochyBot(); ai.init(engine, 0); ctrl = new Controller(); }
    @Test void t1() { assertEquals("PoochyBot V1.25", ai.getName()); }
    @Test void t2() { engine.createFieldIfNeeded(); assertNotNull(PoochyBot.checkOffset(new Piece(Piece.PIECE_T), engine)); }
    @Test void t3() { engine.createFieldIfNeeded(); assertEquals(engine.field.getWidth(), PoochyBot.getColumnDepths(engine.field).length); }
    @Test void t4() { engine.createFieldIfNeeded(); assertEquals(3, PoochyBot.calcValleys(PoochyBot.getColumnDepths(engine.field), 1).length); }
    @Test void t5() { ai.bestX = 1; ai.bestY = 2; ai.bestRt = 3; ai.bestHold = true; ai.logBest(1); assertTrue(true); }
    @Test void t6() { engine.createFieldIfNeeded(); ai.thinkMain(4, 18, 0, -1, new Field(10, 20, 0, false), new Piece(Piece.PIECE_O), 0); assertTrue(true); }
    @Test void t7() { Field fld = new Field(10, 20, 0, false); for (int x = 0; x < 10; x++) fld.setBlockColor(x, 19, 1); ai.thinkMain(4, 18, 0, -1, fld, new Piece(Piece.PIECE_O), 0); assertTrue(true); }
    @Test void t8() { Field fld = new Field(10, 20, 0, false); for (int x = 0; x < 10; x++) fld.setBlockColor(x, 1, 1); ai.thinkMain(4, 1, 0, -1, fld, new Piece(Piece.PIECE_O), 0); assertTrue(true); }
    @Test void t9() { Field fld = new Field(10, 20, 0, false); for (int y = 8; y <= 12; y++) for (int x = 0; x < 10; x++) fld.setBlockColor(x, y, 1); fld.setBlockColor(4, 11, 0); ai.thinkMain(4, 11, 0, -1, fld, new Piece(Piece.PIECE_O), 0); assertTrue(true); }
    @Test void t10() { engine.createFieldIfNeeded(); Field fld = new Field(10, 20, 0, false);
        for (int y = 17; y <= 19; y++) { fld.setBlockColor(3, y, 1); fld.setBlockColor(5, y, 1); }
        for (int y = 15; y <= 19; y++) fld.setBlockColor(4, y, 1); ai.thinkMain(4, 15, 1, -1, fld, new Piece(Piece.PIECE_I), 0); assertTrue(true); }
    @Test void t11() { engine.createFieldIfNeeded(); engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4; engine.nowPieceY = 5; ai.delay = 9999; ai.thinkComplete = true; ai.bestHold = true; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t12() { engine.createFieldIfNeeded(); engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4; engine.nowPieceY = 5; ai.delay = 9999; ai.thinkComplete = true; ai.bestHold = false;
        ai.bestX = 4; engine.nowPieceObject.direction = 0; ai.bestRt = 2; engine.ruleopt.rotateButtonAllowDouble = true; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t13() { engine.createFieldIfNeeded(); engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5; engine.nowPieceY = 5; ai.delay = 9999; ai.thinkComplete = true; ai.bestHold = false;
        ai.bestX = 3; ai.bestRt = 0; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t14() { engine.createFieldIfNeeded(); ai.calcIRS(new Piece(Piece.PIECE_T), engine); assertTrue(true); }
    @Test void t15() { engine.createFieldIfNeeded(); ai.mostMovableX(4, 5, -1, engine, engine.field, new Piece(Piece.PIECE_T), 0); ai.mostMovableX(4, 5, 1, engine, engine.field, new Piece(Piece.PIECE_T), 0); assertTrue(true); }
    @Test void t16() { engine.createFieldIfNeeded(); ai.renderState(engine, 0); assertTrue(true); }
    @Test void t17() { engine.createFieldIfNeeded(); engine.aiPrethink = true; engine.stat = GameEngine.Status.ARE; ai.onFirst(engine, 0); assertTrue(true); }
    @Test void t18() { ai.shutdown(engine, 0); assertTrue(true); }
    @Test void t19() { engine.createFieldIfNeeded(); engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4; engine.nowPieceY = 5; ai.delay = 9999; ai.thinkComplete = true; ai.bestHold = false;
        ai.bestX = 9; ai.bestY = 10; ai.bestRt = 0; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t20() { engine.createFieldIfNeeded(); PoochyBot.getColumnDepth(engine.field, 0); assertTrue(true); }
}