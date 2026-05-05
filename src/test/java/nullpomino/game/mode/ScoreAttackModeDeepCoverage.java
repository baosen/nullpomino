package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;
class ScoreAttackModeDeepCoverage {
    @Test void startGameHighLevel() throws Exception {
        ScoreAttackMode m=new ScoreAttackMode(); GameEngine e=fe(m); m.playerInit(e,0);
        setInt(m,"startlevel",9); m.startGame(e,0); assertEquals(900,e.statistics.level);
    }
    @Test void calcScoreLevel300() throws Exception {
        ScoreAttackMode m=new ScoreAttackMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.ending=0; e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
        e.statistics.level=298; e.timerActive=true;
        m.calcScore(e,0,2); assertEquals(2,e.ending);
    }
    @Test void onLastEndingRoll() throws Exception {
        ScoreAttackMode m=new ScoreAttackMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.gameActive=true; e.ending=2; setInt(m,"rolltime",1956);
        m.onLast(e,0); assertEquals(GameEngine.Status.EXCELLENT,e.stat);
    }
    @Test void onARE() throws Exception {
        ScoreAttackMode m=new ScoreAttackMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.ending=0; setBool(m,"lvupflag",false); e.statc[0]=1; e.statc[1]=2;
        m.onARE(e,0); assertTrue(rb(m,"lvupflag"));
    }
    @Test void saveReplay() throws Exception {
        ScoreAttackMode m=new ScoreAttackMode(); GameEngine e=fe(m); m.playerInit(e,0);
        setInt(m,"startlevel",0); setBool(m,"always20g",false); setBool(m,"big",false);
        e.ai=null; e.statistics.score=100000; e.statistics.level=200; e.statistics.time=6000;
        m.saveReplay(e,0,new CustomProperties());
    }
    private static GameEngine fe(ScoreAttackMode m) { GameManager mg=new GameManager(new EventReceiver()); mg.mode=m; mg.init(); mg.engine[0].init(); return mg.engine[0]; }
    private static boolean rb(Object o,String n) throws Exception { return ff(o,n).getBoolean(o); }
    private static void setInt(Object o,String n,int v) throws Exception { ff(o,n).setInt(o,v); }
    private static void setBool(Object o,String n,boolean v) throws Exception { ff(o,n).setBoolean(o,v); }
    private static Field ff(Object o,String n) throws NoSuchFieldException { Class<?> c=o.getClass(); while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
}
