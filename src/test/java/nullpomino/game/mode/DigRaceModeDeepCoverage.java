package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;
class DigRaceModeDeepCoverage {
    @Test void onSettingConfirm() throws Exception {
        DigRaceMode m=new DigRaceMode(); GameEngine e=fe(m); m.playerInit(e,0);
        setInt(m,"menuCursor",0); setInt(m,"menuTime",10);
        e.ctrl.buttonTime[Controller.BUTTON_A]=1; e.ctrl.buttonPress[Controller.BUTTON_A]=true;
        assertFalse(m.onSetting(e,0));
    }
    @Test void onReadyFills() throws Exception {
        DigRaceMode m=new DigRaceMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.statc[0]=0; m.netIsNetPlay=false; m.onReady(e,0);
    }
    @Test void renderLastGame() throws Exception {
        DigRaceMode m=new DigRaceMode(); GameEngine e=fe(m); m.playerInit(e,0);
        e.stat=GameEngine.Status.MOVE; e.createFieldIfNeeded();
        int w=e.field.getWidth(),h=e.field.getHeight();
        for(int y=h-5;y<h;y++) for(int x=0;x<w;x++)
            e.field.setBlock(x,y,new Block(Block.BLOCK_COLOR_GRAY,0,Block.BLOCK_ATTRIBUTE_VISIBLE|Block.BLOCK_ATTRIBUTE_GARBAGE));
        m.renderLast(e,0);
    }
    @Test void netRecvStats() throws Exception {
        DigRaceMode m=new DigRaceMode(); GameEngine e=fe(m); m.playerInit(e,0);
        m.netRecvStats(e,"game\t0\t0\tstats\t15\t80\t30000\t30.0\t2.0\t1\ttrue\ttrue\t2\t240".split("\t"));
        assertEquals(15,e.statistics.lines);
    }
    private static GameEngine fe(DigRaceMode m) { GameManager mg=new GameManager(new EventReceiver()); mg.mode=m; mg.init(); mg.engine[0].init(); return mg.engine[0]; }
    private static void setInt(Object o,String n,int v) throws Exception { ff(o,n).setInt(o,v); }
    private static Field ff(Object o,String n) throws NoSuchFieldException { Class<?> c=o.getClass(); while(c!=null){try{return c.getDeclaredField(n);}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
}
