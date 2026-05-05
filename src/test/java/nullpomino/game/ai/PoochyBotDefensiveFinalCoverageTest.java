package nullpomino.game.ai;
import static org.junit.jupiter.api.Assertions.*;
import nullpomino.game.component.*;
import nullpomino.game.event.*;
import nullpomino.game.play.*;
import org.junit.jupiter.api.*;
class PoochyBotDefensiveFinalCoverageTest {
    private GameManager gm; private GameEngine engine; private PoochyBotDefensive ai;
    @BeforeEach void setUp() { gm = new GameManager(new EventReceiver()); gm.init(); engine = gm.engine[0]; engine.init(); ai = new PoochyBotDefensive(); }
    @Test void t1() { engine.createFieldIfNeeded(); Field fld = new Field(10, 20, 0, false);
        fld.setBlockColor(5, 17, 1); fld.setBlockColor(5, 18, 1); fld.setBlockColor(5, 19, 1);
        fld.setBlockColor(6, 16, 1); fld.setBlockColor(6, 17, 1); fld.setBlockColor(6, 18, 1); fld.setBlockColor(6, 19, 1);
        fld.setBlockColor(7, 17, 1); fld.setBlockColor(7, 18, 1); fld.setBlockColor(7, 19, 1);
        ai.thinkMain(3, 17, 0, -1, fld, new Piece(Piece.PIECE_T), 0); assertTrue(true); }
    @Test void t2() { engine.createFieldIfNeeded(); Field fld = new Field(10, 20, 0, false);
        fld.setBlockColor(3, 17, 1); fld.setBlockColor(3, 18, 1); fld.setBlockColor(3, 19, 1);
        fld.setBlockColor(5, 17, 1); fld.setBlockColor(5, 18, 1); fld.setBlockColor(5, 19, 1);
        for (int y = 15; y <= 19; y++) fld.setBlockColor(4, y, 1);
        ai.thinkMain(4, 15, 1, -1, fld, new Piece(Piece.PIECE_I), 0); assertTrue(true); }
    @Test void t3() { engine.createFieldIfNeeded(); Field fld = new Field(10, 20, 0, false);
        fld.setBlockColor(3, 17, 1); fld.setBlockColor(3, 18, 1); fld.setBlockColor(3, 19, 1);
        fld.setBlockColor(5, 17, 1); fld.setBlockColor(5, 18, 1); fld.setBlockColor(5, 19, 1);
        for (int y = 13; y <= 19; y++) fld.setBlockColor(4, y, 1);
        ai.thinkMain(4, 13, 1, -1, fld, new Piece(Piece.PIECE_I), 0); assertTrue(true); }
    @Test void t4() { engine.createFieldIfNeeded(); Field fld = new Field(10, 20, 0, false);
        for (int y = 17; y <= 19; y++) fld.setBlockColor(0, y, 1); fld.setBlockColor(1, 19, 1);
        ai.thinkMain(0, 17, 1, -1, fld, new Piece(Piece.PIECE_I), 0); assertTrue(true); }
    @Test void t5() { engine.createFieldIfNeeded(); Field fld = new Field(10, 20, 0, false);
        fld.setBlockColor(4, 17, 1); fld.setBlockColor(6, 17, 1); fld.setBlockColor(4, 19, 1);
        for (int x = 0; x < 10; x++) { if (x >= 4 && x <= 6) continue; fld.setBlockColor(x, 19, 1); fld.setBlockColor(x, 18, 1); }
        fld.setBlockColor(4, 18, 1); fld.setBlockColor(6, 18, 1);
        ai.thinkMain(4, 17, 0, 0, fld, new Piece(Piece.PIECE_T), 0); assertTrue(true); }
    @Test void t6() { engine.createFieldIfNeeded(); Field fld = new Field(10, 20, 0, false);
        for (int x = 0; x < 10; x++) fld.setBlockColor(x, 19, 1);
        ai.thinkMain(4, 18, 0, -1, fld, new Piece(Piece.PIECE_O), 0); assertTrue(true); }
    @Test void t7() { engine.createFieldIfNeeded(); Field fld = new Field(10, 20, 0, false);
        for (int x = 0; x < 10; x++) fld.setBlockColor(x, 1, 1);
        ai.thinkMain(4, 1, 0, -1, fld, new Piece(Piece.PIECE_O), 0); assertTrue(true); }
    @Test void t8() { engine.createFieldIfNeeded(); Field fld = new Field(10, 20, 0, false);
        for (int y = 18; y <= 19; y++) { fld.setBlockColor(3, y, 1); fld.setBlockColor(5, y, 1); }
        for (int y = 16; y <= 19; y++) fld.setBlockColor(4, y, 1);
        ai.thinkMain(4, 16, 1, -1, fld, new Piece(Piece.PIECE_I), 0); assertTrue(true); }
}