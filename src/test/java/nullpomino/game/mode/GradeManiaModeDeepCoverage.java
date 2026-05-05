package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;
class GradeManiaModeDeepCoverage {
    @Test void calcScoreLV999Ending() throws Exception {
        GradeManiaMode m=new GradeManiaMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.ending=0; e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
        e.statistics.level=990; setInt(m,"grade",17); setBool(m,"gm300",true);
        setBool(m,"gm500",true); e.statistics.time=10000;
        m.calcScore(e,0,9); assertEquals(2,e.ending);
    }
    @Test void renderLastGame() throws Exception {
        GradeManiaMode m=new GradeManiaMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.stat=GameEngine.Status.MOVE; setInt(m,"grade",3); setInt(m,"lastscore",500);
        setInt(m,"scgettime",10); m.renderLast(e,0);
    }
    @Test void onLastRollLimit() throws Exception {
        GradeManiaMode m=new GradeManiaMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.gameActive=true; e.ending=2; setInt(m,"rolltime",2968);
        m.onLast(e,0); assertEquals(GameEngine.Status.EXCELLENT,e.stat);
    }
    @Test void renderResultPages() throws Exception {
        GradeManiaMode m=new GradeManiaMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.statc[1]=0; m.renderResult(e,0);
        e.statc[1]=2; setInt(m,"grade",18); e.statistics.time=30000; m.renderResult(e,0);
    }
    @Test void saveReplay() throws Exception {
        GradeManiaMode m=new GradeManiaMode(); GameEngine e=fe(m); m.playerInit(e,0);
        setInt(m,"grade",5); e.statistics.level=300; setInt(m,"lastGradeTime",15000);
        e.owner.replayProp=new CustomProperties(); m.saveReplay(e,0,new CustomProperties());
    }
    private static GameEngine fe(GradeManiaMode m) { GameManager mg=new GameManager(new EventReceiver()); mg.mode=m; mg.init(); mg.engine[0].init(); return mg.engine[0]; }
    private static void setInt(Object o,String n,int v) throws Exception { ff(o,n).setInt(o,v); }
    private static void setBool(Object o,String n,boolean v) throws Exception { ff(o,n).setBoolean(o,v); }
    private static Field ff(Object o,String n) throws NoSuchFieldException { Class<?> c=o.getClass(); while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
}
