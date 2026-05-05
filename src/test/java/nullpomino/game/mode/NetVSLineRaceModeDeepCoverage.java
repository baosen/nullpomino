package nullpomino.game.mode;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;
class NetVSLineRaceModeDeepCoverage {
    @Test void testGetNowPlayerPlaceLeading() throws Exception {
        NetVSLineRaceMode m=new NetVSLineRaceMode(); GameManager mg=new GameManager(new EventReceiver());
        m.modeInit(mg); mg.mode=m; mg.init();
        for(int i=0;i<6;i++) mg.engine[i].init();
        GameEngine e=mg.engine[0]; e.statistics.lines=30;
        setBoolArr(m,"netvsPlayerExist",new boolean[]{true,true,false,false,false,false});
        setBoolArr(m,"netvsPlayerDead",new boolean[6]);
        setIntField(m,"goalLines",40); assertEquals(0,invokeGNP(m,e,0));
    }
    @Test void testUpdateMeter() throws Exception {
        NetVSLineRaceMode m=new NetVSLineRaceMode(); GameEngine e=freshEngine(m);
        m.modeInit(new GameManager(new EventReceiver())); setIntField(m,"goalLines",40);
        e.statistics.lines=25; invokeUM(m,e);
    }
    @Test void testCalcScorePractice() throws Exception {
        NetVSLineRaceMode m=new NetVSLineRaceMode(); GameEngine e=freshEngine(m);
        m.modeInit(new GameManager(new EventReceiver())); m.playerInit(e,0);
        e.playerID=0; e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
        setBoolField(m,"netvsIsPractice",true); setIntField(m,"goalLines",40);
        e.statistics.lines=40; m.calcScore(e,0,1);
        assertEquals(GameEngine.Status.EXCELLENT,e.stat);
    }
    @Test void testRenderResult() {
        NetVSLineRaceMode m=new NetVSLineRaceMode(); GameEngine e=freshEngine(m);
        m.modeInit(new GameManager(new EventReceiver())); m.playerInit(e,0);
        m.renderResult(e,0);
    }
    @Test void testNetRecvStats() throws Exception {
        NetVSLineRaceMode m=new NetVSLineRaceMode(); GameEngine e=freshEngine(m);
        m.modeInit(new GameManager(new EventReceiver())); m.playerInit(e,0);
        setIntField(m,"goalLines",40);
        m.netRecvStats(e,"game\t0\t0\tstats\t10\t5.0\t3.0".split("\t"));
        assertEquals(10,e.statistics.lines);
    }
    private static GameEngine freshEngine(NetVSLineRaceMode m) { GameManager mg=new GameManager(new EventReceiver()); mg.mode=m; mg.init(); mg.engine[0].init(); return mg.engine[0]; }
    private static void setBoolArr(Object o,String n,boolean[] v) throws Exception { ff(o,n).set(o,v); }
    private static void setIntField(Object o,String n,int v) throws Exception { ff(o,n).setInt(o,v); }
    private static void setBoolField(Object o,String n,boolean v) throws Exception { ff(o,n).setBoolean(o,v); }
    private static Field ff(Object o,String n) throws NoSuchFieldException { Class<?> c=o.getClass(); while(c!=null){try{Field f=c.getDeclaredField(n);f.setAccessible(true);return f;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(n);}
    private static int invokeGNP(NetVSLineRaceMode m,GameEngine e,int pid) throws Exception { return (int)findM(NetVSLineRaceMode.class,"getNowPlayerPlace",GameEngine.class,int.class).invoke(m,e,pid); }
    private static void invokeUM(NetVSLineRaceMode m,GameEngine e) throws Exception { findM(NetVSLineRaceMode.class,"updateMeter",GameEngine.class).invoke(m,e); }
    private static Method findM(Class<?> cls,String n,Class<?>... pts) throws NoSuchMethodException { Class<?> c=cls; while(c!=null){try{Method m=c.getDeclaredMethod(n,pts);m.setAccessible(true);return m;}catch(NoSuchMethodException e){c=c.getSuperclass();}}throw new NoSuchMethodException(n);}
}
