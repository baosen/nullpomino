package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;

class RetroMarathonModeEdgeTest {
	@Test void meterBType() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded(); sf(m,"gametype",1);
		e.field.setBlock(0,e.field.getHeight()-1,new Block(Block.BLOCK_COLOR_GRAY));
		e.statistics.lines=20; m.calcScore(e,0,1);
		assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
	}
	@Test void scoreCap999999() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		e.nowPieceObject=new Piece(Piece.PIECE_T); e.createFieldIfNeeded(); sf(m,"gametype",0);
		e.statistics.score=999990;
		e.field.setBlock(0,e.field.getHeight()-1,new Block(Block.BLOCK_COLOR_GRAY));
		m.calcScore(e,0,1); assertEquals(999999, e.statistics.score);
	}
	@Test void setSpeedArrange() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode(); GameEngine e = fresh(m); sf(m,"gametype",2);
		e.statistics.level=0;
		Method me = RetroMarathonMode.class.getDeclaredMethod("setSpeed",GameEngine.class);
		me.setAccessible(true); me.invoke(m,e);
		assertEquals(1, e.speed.gravity); assertEquals(48, e.speed.denominator);
	}
	@Test void startGameBig() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		sfb(m,"big",true); sf(m,"startlevel",3); m.startGame(e,0);
		assertTrue(e.big); assertEquals(3, e.statistics.level);
	}
	private static GameEngine fresh(RetroMarathonMode m) {
		GameManager mgr = new GameManager(new EventReceiver()); mgr.mode=m; mgr.init(); mgr.engine[0].init(); return mgr.engine[0];
	}
	private static void sf(Object o, String n, int v) throws Exception {
		Field f = ff(o.getClass(),n); f.setAccessible(true); f.setInt(o,v);
	}
	private static void sfb(Object o, String n, boolean v) throws Exception {
		Field f = ff(o.getClass(),n); f.setAccessible(true); f.setBoolean(o,v);
	}
	private static Field ff(Class<?> c, String n) throws NoSuchFieldException {
		while(c!=null){try{return c.getDeclaredField(n);}catch(NoSuchFieldException e){c=c.getSuperclass();}}
		throw new NoSuchFieldException(n);
	}
}
