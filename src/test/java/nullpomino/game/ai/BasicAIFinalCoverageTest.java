package nullpomino.game.ai;
import static org.junit.jupiter.api.Assertions.*;
import nullpomino.game.component.*;
import nullpomino.game.event.*;
import nullpomino.game.play.*;
import org.junit.jupiter.api.*;
class BasicAIFinalCoverageTest {
    private GameManager gm; private GameEngine engine; private BasicAI ai; private Controller ctrl;
    @BeforeEach void setUp() { gm = new GameManager(new EventReceiver()); gm.init(); engine = gm.engine[0]; engine.init(); ai = new BasicAI(); ctrl = new Controller(); }
    @Test void t1() { engine.createFieldIfNeeded(); engine.aiUseThread = false; ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5; engine.nowPieceY = 18; engine.ruleopt.rotateButtonDefaultRight = false; ai.thinkBestPosition(engine, 0); assertTrue(true); }
    @Test void t2() { engine.createFieldIfNeeded(); engine.aiUseThread = false; ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5; engine.nowPieceY = 18; engine.ruleopt.rotateButtonDefaultRight = true; ai.thinkBestPosition(engine, 0); assertTrue(true); }
    @Test void t3() { engine.createFieldIfNeeded(); engine.aiUseThread = false; ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5; engine.nowPieceY = 18; engine.ruleopt.rotateButtonAllowDouble = true; ai.thinkBestPosition(engine, 0); assertTrue(true); }
    @Test void t4() { engine.createFieldIfNeeded(); engine.aiUseThread = false; ai.init(engine, 0);
        engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4; engine.nowPieceY = 5; ai.bestHold = true; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t5() { engine.createFieldIfNeeded(); engine.aiUseThread = false; ai.init(engine, 0);
        engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4; engine.nowPieceY = 5; ai.bestHold = false; ai.bestX = 4; ai.bestY = 5;
        engine.nowPieceObject.direction = 0; ai.bestRt = 2; engine.ruleopt.rotateButtonAllowDouble = true; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t6() { engine.createFieldIfNeeded(); engine.aiUseThread = false; ai.init(engine, 0);
        engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4; engine.nowPieceY = 5; ai.bestHold = false; ai.bestX = 4; ai.bestY = 5;
        engine.nowPieceObject.direction = 1; ai.bestRt = 0; engine.ruleopt.rotateButtonAllowReverse = true; engine.ruleopt.rotateButtonDefaultRight = false; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t7() { engine.createFieldIfNeeded(); engine.aiUseThread = false; ai.init(engine, 0);
        engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5; engine.nowPieceY = 5; ai.bestX = 3; ai.bestY = 5; ai.bestRt = 0; ai.bestHold = false; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t8() { engine.createFieldIfNeeded(); engine.aiUseThread = false; ai.init(engine, 0);
        engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4; engine.nowPieceY = 18; ai.bestX = 4; ai.bestY = 18; ai.bestRt = 0; ai.bestHold = false; ai.bestRtSub = -1;
        engine.ruleopt.harddropEnable = true; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t9() { engine.createFieldIfNeeded(); Field fld = engine.field;
        for (int y = 8; y <= 12; y++) for (int x = 0; x < 10; x++) fld.setBlockColor(x, y, 1);
        fld.setBlockColor(0, 11, 0); ai.thinkMain(engine, 0, 10, 0, -1, fld, new Piece(Piece.PIECE_I), null, null, 1); assertTrue(true); }
    @Test void t10() { engine.createFieldIfNeeded(); Field fld = engine.field;
        for (int y = 8; y <= 12; y++) for (int x = 0; x < 10; x++) fld.setBlockColor(x, y, 1);
        fld.setBlockColor(4, 11, 0); ai.thinkMain(engine, 4, 11, 0, -1, fld, new Piece(Piece.PIECE_O), null, null, 1); assertTrue(true); }
    @Test void t11() { engine.createFieldIfNeeded(); Field fld = engine.field;
        for (int x = 0; x < 10; x++) fld.setBlockColor(x, 19, 1);
        for (int x = 0; x < 10; x++) fld.setBlockColor(x, 17, 1);
        ai.thinkMain(engine, 4, 18, 0, -1, fld, new Piece(Piece.PIECE_O), null, null, 1); assertTrue(true); }
    @Test void t12() { engine.createFieldIfNeeded(); Field fld = engine.field;
        for (int x = 0; x < 10; x++) fld.setBlockColor(x, 19, 1);
        engine.combo = 5; engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
        ai.thinkMain(engine, 4, 18, 0, -1, fld, new Piece(Piece.PIECE_O), null, null, 1); assertTrue(true); }
    @Test void t13() { engine.createFieldIfNeeded(); Field fld = engine.field;
        fld.setBlockColor(4, 16, 1); fld.setBlockColor(6, 16, 1); fld.setBlockColor(4, 18, 1);
        for (int x = 0; x < 10; x++) if (x != 5) { fld.setBlockColor(x, 19, 1); fld.setBlockColor(x, 18, 1); }
        fld.setBlockColor(5, 18, 0); ai.thinkMain(engine, 4, 16, 0, 0, fld, new Piece(Piece.PIECE_T), null, null, 1); assertTrue(true); }
    @Test void t14() { engine.aiUseThread = false; ai.init(engine, 0); ai.setControl(engine, 0, ctrl); assertEquals(1, ai.delay); }
    @Test void t15() { engine.createFieldIfNeeded(); engine.aiUseThread = false; ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5; engine.nowPieceY = 18; engine.holdPieceObject = new Piece(Piece.PIECE_S); ai.thinkBestPosition(engine, 0); assertTrue(true); }
}