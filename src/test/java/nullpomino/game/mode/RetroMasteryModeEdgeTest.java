package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;

class RetroMasteryModeEdgeTest {
	@Test void meterPressureRed() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded(); sf(m,"gametype",2);
		sf(m,"levellines",20); sf(m,"loons",19); m.calcScore(e,0,1);
		// loons=19+1=20 >= levellines=20 → level up, levellines→25, togo=5 → GREEN (not 1/2/3)
		assertEquals(GameEngine.METER_COLOR_GREEN, e.meterColor);
	}
	@Test void endlessHighLevel() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		sf(m,"gametype",1); sf(m,"startlevel",15); m.startGame(e,0);
		assertEquals(130, gf(m,"levellines"));
	}
	private static GameEngine fresh(RetroMasteryMode m) {
		GameManager mgr = new GameManager(new EventReceiver()); mgr.mode=m; mgr.init(); mgr.engine[0].init(); return mgr.engine[0];
	}
	private static int gf(Object o, String n) throws Exception {
		Field f = ff(o.getClass(),n); f.setAccessible(true); return f.getInt(o);
	}
	private static void sf(Object o, String n, int v) throws Exception {
		Field f = ff(o.getClass(),n); f.setAccessible(true); f.setInt(o,v);
	}
	private static Field ff(Class<?> c, String n) throws NoSuchFieldException {
		while(c!=null){try{return c.getDeclaredField(n);}catch(NoSuchFieldException e){c=c.getSuperclass();}}
		throw new NoSuchFieldException(n);
	}
}
