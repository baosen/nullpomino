package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;
class AvalancheVSDummyModeDeepCoverage extends AvalancheVSDummyMode {
    @Override public boolean lineClearEnd(GameEngine engine, int playerID) { gameOverCheck(engine,playerID); return false; }
    @Test void startGameSetsEngine() throws Exception { GameEngine e=fe(); startGame(e,0); assertEquals(GameEngine.COMBO_TYPE_DISABLE,e.comboType); }
    @Test void afterHardDrop() throws Exception { GameEngine e=fe(); afterHardDropFall(e,0,5); assertEquals(5,e.statistics.score); }
    @Test void calcChainClassic() throws Exception {
        GameEngine e=fe();
        assertEquals(0,calcChainClassicPower(e,0,1)); assertEquals(8,calcChainClassicPower(e,0,2));
    }
    @Test void calcChainNew() throws Exception { setBA("newChainPower",new boolean[]{true,false}); assertEquals(4,calcChainNewPower(null,0,1)); }
    @Test void renderResultWin() throws Exception {
        GameManager mgr=new GameManager(new EventReceiver()); modeInit(mgr); mgr.mode=this; mgr.init(); mgr.engine[0].init();
        GameEngine e=mgr.engine[0]; playerInit(e,0); winnerID=0; renderResult(e,0);
    }
    @Test void drawHardOjama() throws Exception {
        GameEngine e=fe(); e.createFieldIfNeeded();
        Block blk=new Block(Block.BLOCK_COLOR_GRAY); blk.hard=3; e.field.setBlock(0,0,blk);
        drawHardOjama(e,0);
    }
    private GameEngine fe() { GameManager m=new GameManager(new EventReceiver()); modeInit(m); m.mode=this; m.init(); m.engine[0].init(); return m.engine[0]; }
    private void setBA(String n,boolean[] v) throws Exception { ff(n).set(this,v); }
    private Field ff(String n) throws NoSuchFieldException { Class<?> c=getClass(); while(c!=null){try{return c.getDeclaredField(n);}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
}
