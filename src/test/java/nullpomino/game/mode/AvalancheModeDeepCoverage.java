package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;
class AvalancheModeDeepCoverage {
    @Test void onLastUltra() throws Exception {
        AvalancheMode m=new AvalancheMode(); GameEngine e=fe(m); m.playerInit(e,0);
        setInt(m,"gametype",1); e.statistics.time=10801; e.timerActive=true;
        m.onLast(e,0); assertEquals(GameEngine.Status.ENDINGSTART,e.stat);
    }
    @Test void onLastSprint() throws Exception {
        AvalancheMode m=new AvalancheMode(); GameEngine e=fe(m); m.playerInit(e,0);
        setInt(m,"gametype",2); e.statistics.score=15001; e.timerActive=true;
        m.onLast(e,0); assertEquals(GameEngine.Status.ENDINGSTART,e.stat);
    }
    @Test void lineClearEndGameOver() throws Exception {
        AvalancheMode m=new AvalancheMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.createFieldIfNeeded(); e.field.setBlock(2,0,new Block(Block.BLOCK_COLOR_RED));
        m.lineClearEnd(e,0); assertEquals(GameEngine.Status.GAMEOVER,e.stat);
    }
    @Test void renderResultSprint() throws Exception {
        AvalancheMode m=new AvalancheMode(); GameEngine e=fe(m); m.playerInit(e,0);
        setInt(m,"gametype",2); setInt(m,"rankingRank",0); m.renderResult(e,0);
    }
    @Test void calcChainMultiplier() throws Exception {
        AvalancheMode m=new AvalancheMode(); setInt(m,"scoreType",0);
        assertEquals(8,m.calcChainMultiplier(2)); assertEquals(16,m.calcChainMultiplier(3));
    }
    private static GameEngine fe(AvalancheMode m) { GameManager mg=new GameManager(new EventReceiver()); mg.mode=m; mg.init(); mg.engine[0].init(); return mg.engine[0]; }
    private static void setInt(Object o,String n,int v) throws Exception { ff(o,n).setInt(o,v); }
    private static Field ff(Object o,String n) throws NoSuchFieldException { Class<?> c=o.getClass(); while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
}
