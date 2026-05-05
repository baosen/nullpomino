package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

class AIRemainingCoverageTest {
    private GameManager gm;
    private GameEngine engine;
    private Controller ctrl;
    @BeforeEach
    void setUp() {
        gm = new GameManager(new EventReceiver());
        gm.init();
        engine = gm.engine[0];
        engine.init();
        engine.createFieldIfNeeded();
        ctrl = new Controller();
    }

    // TSpinAI
    @Test void tspinAI_newTSlot() { TSpinAI a = new TSpinAI(); Field f = new Field(10,20,0,false); for (int y = 14; y < 20; y++) for (int x = 0; x < 10; x++) if ((x>=3&&x<=5)||x==7) continue; else f.setBlockColor(x,y,1); f.setBlockColor(3,16,1); f.setBlockColor(3,18,1); for (int x = 0; x < 10; x++) f.setBlockColor(x,19,1); a.thinkMain(engine,5,16,0,-1,f,new Piece(Piece.PIECE_T),new Piece(Piece.PIECE_S),new Piece(Piece.PIECE_T),1); }
    @Test void tspinAI_lidDanger() { TSpinAI a = new TSpinAI(); Field f = new Field(10,20,0,false); for (int y = 8; y <= 12; y++) for (int x = 0; x < 10; x++) f.setBlockColor(x,y,1); f.setBlockColor(0,11,0); a.thinkMain(engine,0,6,0,-1,f,new Piece(Piece.PIECE_I),new Piece(Piece.PIECE_T),null,1); }
    @Test void tspinAI_tspinBonus() { TSpinAI a = new TSpinAI(); Field f = new Field(10,20,0,false); for (int y = 16; y < 20; y++) for (int x = 0; x < 10; x++) f.setBlockColor(x,y,1); a.thinkMain(engine,4,16,0,1,f,new Piece(Piece.PIECE_T),null,null,1); }
    @Test void tspinAI_needIValley() { TSpinAI a = new TSpinAI(); Field f = new Field(10,20,0,false); for (int y = 8; y <= 12; y++) for (int x = 0; x < 10; x++) f.setBlockColor(x,y,1); f.setBlockColor(4,11,0); f.setBlockColor(4,10,0); engine.comboType = GameEngine.COMBO_TYPE_NORMAL; a.thinkMain(engine,4,10,0,-1,f,new Piece(Piece.PIECE_I),null,null,0); }
    @Test void tspinAI_comboBonus() { TSpinAI a = new TSpinAI(); Field f = new Field(10,20,0,false); for (int x = 0; x < 10; x++) f.setBlockColor(x,19,1); engine.combo = 3; engine.comboType = GameEngine.COMBO_TYPE_NORMAL; a.thinkMain(engine,4,18,0,-1,f,new Piece(Piece.PIECE_O),null,null,1); }

    // Nohoho
    @Test void nohoho_rotateDouble() { Nohoho a = new Nohoho(); engine.aiUseThread = false; a.init(engine,0); engine.stat = GameEngine.Status.MOVE; engine.statc[0] = 1; engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T],engine.ruleopt.pieceOffsetY[Piece.PIECE_T]); engine.nowPieceX = 4; engine.nowPieceY = 5; a.delay = 9999; a.bestX = 6; a.bestY = 5; a.thinkComplete = true; a.bestHold = false; engine.nowPieceObject.direction = Piece.DIRECTION_UP; a.bestRt = Piece.DIRECTION_DOWN; engine.ruleopt.rotateButtonAllowDouble = true; a.setControl(engine,0,ctrl); }
    @Test void nohoho_pieceHoldNull() throws Exception { Nohoho a = new Nohoho(); engine.aiUseThread = false; a.init(engine,0); engine.createFieldIfNeeded(); engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T],engine.ruleopt.pieceOffsetY[Piece.PIECE_T]); engine.nowPieceX = 4; engine.nowPieceY = 18; engine.holdPieceObject = null; engine.nextPieceCount = 0; engine.nextPieceArraySize = 3; engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T),new Piece(Piece.PIECE_S),new Piece(Piece.PIECE_Z)}; a.thinkBestPosition(engine,0); }
    @Test void nohoho_thinkMainChain() { Nohoho a = new Nohoho(); Field f = new Field(10,20,0,false); for (int y = 14; y < 20; y++) for (int x = 0; x < 10; x++) f.setBlockColor(x,y,(x%4)+1); a.thinkMain(4,18,0,-1,f,new Piece(Piece.PIECE_O),4); }

    // RanksAI
    @Test void ranksAI_init() { RanksAI a = new RanksAI(); engine.aiUseThread = false; a.init(engine,0); }
    @Test void ranksAI_thinkBestPosition() { RanksAI a = new RanksAI(); engine.aiUseThread = false; a.init(engine,0); for (int y = 0; y < 20; y++) for (int x = 0; x < 10; x++) engine.field.setBlockColor(x,y,1); engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T],engine.ruleopt.pieceOffsetY[Piece.PIECE_T]); engine.nowPieceX = 4; engine.nowPieceY = 18; engine.nextPieceCount = 0; engine.nextPieceArraySize = 5; engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T),new Piece(Piece.PIECE_S),new Piece(Piece.PIECE_Z),new Piece(Piece.PIECE_L),new Piece(Piece.PIECE_J)}; a.thinkBestPosition(engine,0); }
    @Test void ranksAI_4Lines() { RanksAI a = new RanksAI(); engine.aiUseThread = false; a.init(engine,0); for (int y = 16; y < 20; y++) for (int x = 0; x < 10; x++) engine.field.setBlockColor(x,y,1); engine.nowPieceObject = new Piece(Piece.PIECE_I); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_I],engine.ruleopt.pieceOffsetY[Piece.PIECE_I]); engine.nowPieceX = 3; engine.nowPieceY = 18; engine.nextPieceCount = 0; engine.nextPieceArraySize = 5; engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T),new Piece(Piece.PIECE_S),new Piece(Piece.PIECE_Z),new Piece(Piece.PIECE_L),new Piece(Piece.PIECE_J)}; a.thinkBestPosition(engine,0); }

    // PoochyBotDefensive
    @Test void poochyDefensive_diffMod4() { PoochyBotDefensive a = new PoochyBotDefensive(); Field f = new Field(10,20,0,false); for (int y = 12; y < 20; y++) { f.setBlockColor(3,y,1); f.setBlockColor(4,y,1); } f.setBlockColor(2,16,1); f.setBlockColor(2,15,1); f.setBlockColor(6,16,1); f.setBlockColor(6,15,1); a.thinkMain(3,15,0,-1,f,new Piece(Piece.PIECE_T),1); }
    @Test void poochyDefensive_IValley() { PoochyBotDefensive a = new PoochyBotDefensive(); Field f = new Field(10,20,0,false); for (int y = 14; y < 20; y++) { f.setBlockColor(2,y,1); f.setBlockColor(3,y,1); } for (int y = 10; y < 20; y++) f.setBlockColor(1,y,1); a.thinkMain(1,12,0,-1,f,new Piece(Piece.PIECE_I),1); }
    @Test void poochyDefensive_valleyBonus() { PoochyBotDefensive a = new PoochyBotDefensive(); Field f = new Field(10,20,0,false); for (int y = 14; y < 20; y++) for (int x = 0; x < 10; x++) f.setBlockColor(x,y,1); for (int y = 0; y < 20; y++) f.setBlockColor(5,y,0); a.thinkMain(5,14,1,-1,f,new Piece(Piece.PIECE_I),1); }
    @Test void poochyDefensive_LJValley() { PoochyBotDefensive a = new PoochyBotDefensive(); Field f = new Field(10,20,0,false); for (int y = 14; y < 20; y++) for (int x = 2; x < 5; x++) f.setBlockColor(x,y,1); f.setBlockColor(1,16,1); f.setBlockColor(1,15,1); f.setBlockColor(6,16,1); a.thinkMain(2,15,0,-1,f,new Piece(Piece.PIECE_T),1); }
    @Test void poochyDefensive_spawnDanger() { PoochyBotDefensive a = new PoochyBotDefensive(); Field f = new Field(10,20,0,false); f.setBlockColor(4,0,1); a.thinkMain(3,0,0,-1,f,new Piece(Piece.PIECE_I),0); }

    // ComboRaceSeedSearch
    @Test void comboRaceSeedSearch_createTables() { ComboRaceSeedSearch.createTables(); ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH]; boolean[] en = new boolean[Piece.PIECE_COUNT]; for(int i = 0; i < Piece.PIECE_STANDARD_COUNT; i++) en[i] = true; assertNotNull(ComboRaceSeedSearch.moves, "moves initialized"); }

    // BasicAI
    @Test void basicAI_shiftRotation() { BasicAI a = new BasicAI(); engine.aiUseThread = false; a.init(engine,0); engine.createFieldIfNeeded(); engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T],engine.ruleopt.pieceOffsetY[Piece.PIECE_T]); engine.nowPieceX = 5; engine.nowPieceY = 18; engine.ruleopt.rotateButtonDefaultRight = true; engine.ruleopt.rotateButtonAllowReverse = true; a.thinkBestPosition(engine,0); }
    @Test void basicAI_thinkMainNoPlace() { BasicAI a = new BasicAI(); Field f = new Field(10,20,0,false); for (int y = 0; y < 20; y++) for (int x = 0; x < 10; x++) f.setBlockColor(x,y,1); a.thinkMain(engine,0,0,0,-1,f,new Piece(Piece.PIECE_T),null,null,1); }
    @Test void basicAI_thinkMainLidDanger() { BasicAI a = new BasicAI(); Field f = new Field(10,20,0,false); for (int y = 8; y <= 12; y++) for (int x = 0; x < 10; x++) f.setBlockColor(x,y,1); f.setBlockColor(0,11,0); a.thinkMain(engine,0,8,0,-1,f,new Piece(Piece.PIECE_I),null,null,1); }
    @Test void basicAI_thinkMainNeedIValley() { BasicAI a = new BasicAI(); Field f = new Field(10,20,0,false); for (int y = 8; y <= 12; y++) for (int x = 0; x < 10; x++) f.setBlockColor(x,y,1); f.setBlockColor(4,11,0); f.setBlockColor(4,10,0); a.thinkMain(engine,4,10,0,-1,f,new Piece(Piece.PIECE_I),null,null,0); }
    @Test void basicAI_thinkMainHeightDec() { BasicAI a = new BasicAI(); Field f = new Field(10,20,0,false); for (int y = 0; y < 20; y++) for (int x = 0; x < 10; x++) f.setBlockColor(x,y,1); for (int x = 0; x < 10; x++) { f.setBlockColor(x,0,0); f.setBlockColor(x,1,0); } a.thinkMain(engine,4,10,0,-1,f,new Piece(Piece.PIECE_T),null,null,1); }
    @Test void basicAI_threadInterrupt() { BasicAI a = new BasicAI(); engine.aiUseThread = true; a.init(engine,0); a.newPiece(engine,0); try { Thread.sleep(30); } catch (InterruptedException e) { } a.shutdown(engine,0); }
}
