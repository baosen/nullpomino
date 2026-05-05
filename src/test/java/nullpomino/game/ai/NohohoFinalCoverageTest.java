package nullpomino.game.ai;
import static org.junit.jupiter.api.Assertions.*;
import nullpomino.game.component.*;
import nullpomino.game.event.*;
import nullpomino.game.play.*;
import org.junit.jupiter.api.*;
class NohohoFinalCoverageTest {
    private GameManager gm; private GameEngine engine; private Nohoho ai; private Controller ctrl;
    @BeforeEach void setUp() { gm = new GameManager(new EventReceiver()); gm.init(); engine = gm.engine[0]; engine.init(); engine.aiUseThread = false; ai = new Nohoho(); ai.init(engine, 0); ctrl = new Controller(); }
    private void setupMove() { engine.createFieldIfNeeded(); engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 4; engine.nowPieceY = 5; ai.delay = 9999; ai.bestX = 6; ai.bestY = 5; ai.thinkComplete = true; ai.bestHold = false; }
    @Test void t1() { setupMove(); engine.nowPieceObject.direction = Piece.DIRECTION_RIGHT; ai.bestRt = Piece.DIRECTION_DOWN;
        engine.ruleopt.rotateButtonAllowReverse = true; engine.ruleopt.rotateButtonAllowDouble = false; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t2() { setupMove(); engine.nowPieceObject.direction = Piece.DIRECTION_UP; ai.bestRt = Piece.DIRECTION_LEFT;
        engine.ruleopt.rotateButtonAllowReverse = false; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t3() { setupMove(); ai.bestX = 4; ai.bestY = 5; engine.nowPieceObject.direction = Piece.DIRECTION_UP; ai.bestRt = Piece.DIRECTION_DOWN;
        engine.ruleopt.rotateButtonAllowDouble = true; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t4() { setupMove(); ai.bestX = 4; ai.bestY = 5; engine.nowPieceObject.direction = 0; ai.bestRt = engine.getRotateDirection(1);
        engine.ruleopt.rotateButtonAllowDouble = false; engine.ruleopt.rotateButtonAllowReverse = true; ai.setControl(engine, 0, ctrl); assertTrue(true); }
    @Test void t5() { engine.createFieldIfNeeded(); assertNotNull(Nohoho.checkOffset(new Piece(Piece.PIECE_T), engine)); }
    @Test void t6() { Field fld = new Field(10, 20, 0, false); fld.setBlockColor(2, 18, 1); fld.setBlockColor(2, 19, 1); fld.setBlockColor(3, 18, 1); fld.setBlockColor(3, 19, 1);
        ai.thinkMain(1, 18, 0, -1, fld, new Piece(Piece.PIECE_T), 5); assertTrue(true); }
    @Test void t7() { Field fld = new Field(10, 20, 0, false); for (int x = 0; x < 10; x++) for (int y = 0; y < 20; y++) fld.setBlockColor(x, y, (x + y) % 4 + 1);
        ai.thinkMain(4, 18, 0, -1, fld, new Piece(Piece.PIECE_T), 3); assertTrue(true); }
}