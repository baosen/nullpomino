package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;
class NetVSDigRaceModeDeepCoverage {
    @Test void onReadyFills() throws Exception {
        NetVSDigRaceMode m=new NetVSDigRaceMode(); GameManager mg=new GameManager(new EventReceiver());
        m.modeInit(mg); mg.mode=m; mg.init(); mg.engine[0].init();
        GameEngine e=mg.engine[0]; e.statc[0]=0;
        setBoolArr(m,"netvsPlayerExist",new boolean[]{true,false,false,false,false,false});
        // netCurrentRoomInfo must be non-null to avoid NPE in parent onReady
        java.lang.reflect.Field f = NetDummyMode.class.getDeclaredField("netCurrentRoomInfo");
        f.setAccessible(true);
        f.set(m, new nullpomino.game.net.NetRoomInfo());
        m.onReady(e,0);
    }
    @Test void calcScorePractice() throws Exception {
        NetVSDigRaceMode m=new NetVSDigRaceMode(); GameEngine e=fe(m);
        m.modeInit(new GameManager(new EventReceiver())); e.playerID=0;
        e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
        setBool(m,"netvsIsPractice",true);
        setIntArr(m,"playerRemainLines",new int[]{0,0,0,0,0,0});
        m.calcScore(e,0,1); assertEquals(GameEngine.Status.EXCELLENT,e.stat);
    }
    @Test void renderResult() throws Exception {
        NetVSDigRaceMode m=new NetVSDigRaceMode(); GameEngine e=fe(m);
        e.displaysize=-1; m.renderResult(e,0);
    }
    private static GameEngine fe(NetVSDigRaceMode m) { GameManager mg=new GameManager(new EventReceiver()); mg.mode=m; mg.init(); mg.engine[0].init(); return mg.engine[0]; }
    private static void setBool(Object o,String n,boolean v) throws Exception { ff(o,n).setBoolean(o,v); }
    private static void setBoolArr(Object o,String n,boolean[] v) throws Exception { ff(o,n).set(o,v); }
    private static void setIntArr(Object o,String n,int[] v) throws Exception { ff(o,n).set(o,v); }
    private static Field ff(Object o,String n) throws NoSuchFieldException { Class<?> c=o.getClass(); while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
}
