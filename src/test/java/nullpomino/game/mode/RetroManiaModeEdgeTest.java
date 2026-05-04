package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;

class RetroManiaModeEdgeTest {
	@Test void allClearVersion1() throws Exception {
		RetroManiaMode m = new RetroManiaMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		e.statistics.level=0; sf(m,"version",1); m.calcScore(e,0,1);
		assertEquals(2000, gf(m,"lastscore"));
	}
	@Test void onLastCaps() throws Exception {
		RetroManiaMode m = new RetroManiaMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		sf(m,"version",2); e.timerActive=true;
		e.statistics.score=9999999; e.statistics.lines=9999; e.statistics.level=999;
		m.onLast(e,0);
		assertEquals(999999, e.statistics.score); assertEquals(999, e.statistics.lines); assertEquals(99, e.statistics.level);
	}
	@Test void softDropDenom1() throws Exception {
		RetroManiaMode m = new RetroManiaMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		sf(m,"version",2); e.speed.denominator=1; e.statistics.score=100;
		m.afterSoftDropFall(e,0,50); assertEquals(100, e.statistics.score);
	}
	@Test void loadRankingCaps() throws Exception {
		RetroManiaMode m = new RetroManiaMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		CustomProperties p = new CustomProperties();
		p.setProperty("retromania.ranking.Standard.0.score.0", 99999999);
		p.setProperty("retromania.ranking.Standard.0.lines.0", 99999);
		Method me = RetroManiaMode.class.getDeclaredMethod("loadRanking",CustomProperties.class,String.class);
		me.setAccessible(true); me.invoke(m,p,"Standard");
		int[][] rs = (int[][])rf(m,"rankingScore"); int[][] rl = (int[][])rf(m,"rankingLines");
		assertEquals(999999, rs[0][0]); assertEquals(999, rl[0][0]);
	}
	private static GameEngine fresh(RetroManiaMode m) {
		GameManager mgr = new GameManager(new EventReceiver()); mgr.mode=m; mgr.init(); mgr.engine[0].init(); return mgr.engine[0];
	}
	private static int gf(Object o, String n) throws Exception {
		Field f = ff(o.getClass(),n); f.setAccessible(true); return f.getInt(o);
	}
	private static Object rf(Object o, String n) throws Exception {
		Field f = ff(o.getClass(),n); f.setAccessible(true); return f.get(o);
	}
	private static void sf(Object o, String n, int v) throws Exception {
		Field f = ff(o.getClass(),n); f.setAccessible(true); f.setInt(o,v);
	}
	private static Field ff(Class<?> c, String n) throws NoSuchFieldException {
		while(c!=null){try{return c.getDeclaredField(n);}catch(NoSuchFieldException e){c=c.getSuperclass();}}
		throw new NoSuchFieldException(n);
	}
}
