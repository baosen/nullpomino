package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import java.lang.reflect.Field;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;
class RetroMarathonModeDefaultCaseCoverageTest {
    @Test void renderLastDefaultGametypeBranch() throws Exception {
        RetroMarathonMode m = new RetroMarathonMode();
        GameEngine e = fresh(m); m.playerInit(e, 0);
        Field nf = RetroMarathonMode.class.getDeclaredField("GAMETYPE_NAME");
        nf.setAccessible(true);
        String[] old = (String[])nf.get(null);
        String[] nu = new String[4]; System.arraycopy(old,0,nu,0,old.length); nu[3]="X"; nf.set(null,nu);
        setInt(m,"gametype",3); e.stat=GameEngine.Status.MOVE;
        e.statistics.score=500; e.statistics.lines=10; e.statistics.level=3; e.statistics.time=3000;
        setInt(m,"lastscore",0); setInt(m,"scgettime",200);
        assertDoesNotThrow(()->m.renderLast(e,0));
    }
    private static GameEngine fresh(RetroMarathonMode m) {
        GameManager mgr=new GameManager(new EventReceiver()); mgr.mode=m; mgr.init(); mgr.engine[0].init(); return mgr.engine[0];
    }
    private static void setInt(Object o,String n,int v) throws Exception {
        findField(o.getClass(),n).setInt(o,v);
    }
    private static Field findField(Class<?>cls,String name) throws Exception {
        for(Class<?>c=cls;c!=null;c=c.getSuperclass()){try{Field f=c.getDeclaredField(name);f.setAccessible(true);return f;}catch(NoSuchFieldException e){}}
        throw new NoSuchFieldException(name);
    }
}
