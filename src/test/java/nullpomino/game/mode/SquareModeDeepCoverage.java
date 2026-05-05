package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Field;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;
class SquareModeDeepCoverage {
    @Test void calcScoreSpin() throws Exception {
        SquareMode m=new SquareMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.tspin=true; e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
        setInt(m,"version",1); m.calcScore(e,0,1);
    }
    @Test void onLastUltra() throws Exception {
        SquareMode m=new SquareMode(); GameEngine e=fe(m); m.playerInit(e,0);
        setInt(m,"gametype",1); e.statistics.time=10801; e.timerActive=true;
        m.onLast(e,0); assertEquals(GameEngine.Status.ENDINGSTART,e.stat);
    }
    @Test void pieceLockedSquare() throws Exception {
        SquareMode m=new SquareMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.createFieldIfNeeded(); e.nowPieceObject=new Piece(Piece.PIECE_T);
        for(int x=0;x<2;x++) for(int y=0;y<2;y++) e.field.setBlock(x,y,new Block(Block.BLOCK_COLOR_RED));
        assertDoesNotThrow(()->m.pieceLocked(e,0,0));
    }
    @Test void saveReplay() throws Exception {
        SquareMode m=new SquareMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.ai=null; e.statistics.score=500; e.statistics.time=3000;
        setInt(m,"squares",10); setInt(m,"gametype",0);
        m.saveReplay(e,0,new CustomProperties());
    }
    @Test void lineClearEndTnt() throws Exception {
        SquareMode m=new SquareMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.lineGravityType=GameEngine.LineGravity.CASCADE; e.lineGravityTotalLines=1;
        setBool(m,"tntAvalanche",true); e.createFieldIfNeeded();
        assertTrue(m.lineClearEnd(e,0));
    }
    private static GameEngine fe(SquareMode m) { GameManager mg=new GameManager(new EventReceiver()); mg.mode=m; mg.init(); mg.engine[0].init(); return mg.engine[0]; }
    private static int readInt(Object o,String n) throws Exception { return ff(o,n).getInt(o); }
    private static void setInt(Object o,String n,int v) throws Exception { ff(o,n).setInt(o,v); }
    private static void setBool(Object o,String n,boolean v) throws Exception { ff(o,n).setBoolean(o,v); }
    private static Field ff(Object o,String n) throws NoSuchFieldException { Class<?> c=o.getClass(); while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
}
