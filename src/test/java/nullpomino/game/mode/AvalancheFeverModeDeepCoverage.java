package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;
class AvalancheFeverModeDeepCoverage {
    @Test void onSettingReplay() throws Exception {
        AvalancheFeverMode m=new AvalancheFeverMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.owner.replayMode=true; boolean r=true;
        while(r) r=m.onSetting(e,0);
    }
    @Test void onLastCountdown() throws Exception {
        AvalancheFeverMode m=new AvalancheFeverMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.timerActive=true; setInt(m,"timeLimit",360);
        m.onLast(e,0); assertEquals(359,readInt(m,"timeLimit"));
    }
    @Test void lineClearEndGameOver() throws Exception {
        AvalancheFeverMode m=new AvalancheFeverMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.createFieldIfNeeded(); setBool(m,"cleared",false);
        e.field.setBlock(2,0,new Block(Block.BLOCK_COLOR_RED));
        m.lineClearEnd(e,0); assertEquals(GameEngine.Status.GAMEOVER,e.stat);
    }
    @Test void renderResult() throws Exception {
        AvalancheFeverMode m=new AvalancheFeverMode(); GameEngine e=fe(m); m.playerInit(e,0);
        setInt(m,"rankingRank",-1); setInt(m,"scoreBeforeBonus",1000);
        setInt(m,"zenKeshiCount",5); setInt(m,"zenKeshiBonus",500);
        setInt(m,"maxChainBonus",300); e.statistics.maxChain=8; e.statistics.score=1800;
        m.renderResult(e,0);
    }
    @Test void saveReplay() throws Exception {
        AvalancheFeverMode m=new AvalancheFeverMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.statistics.score=50000; e.statistics.time=3000;
        setInt(m,"mapSet",0); setInt(m,"numColors",4);
        m.saveReplay(e,0,new CustomProperties());
    }
    @Test void calcOjama() throws Exception {
        AvalancheFeverMode m=new AvalancheFeverMode(); setInt(m,"ojamaRate",30);
        Method meth=AvalancheFeverMode.class.getDeclaredMethod("calcOjama",int.class,int.class,int.class,int.class);
        meth.setAccessible(true); assertEquals(10,(int)meth.invoke(m,100,10,100,3));
    }
    private static GameEngine fe(AvalancheFeverMode m) { GameManager mg=new GameManager(new EventReceiver()); mg.mode=m; mg.init(); mg.engine[0].init(); return mg.engine[0]; }
    private static int readInt(Object o,String n) throws Exception { return ff(o,n).getInt(o); }
    private static void setInt(Object o,String n,int v) throws Exception { ff(o,n).setInt(o,v); }
    private static void setBool(Object o,String n,boolean v) throws Exception { ff(o,n).setBoolean(o,v); }
    private static Field ff(Object o,String n) throws NoSuchFieldException { Class<?> c=o.getClass(); while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
}
