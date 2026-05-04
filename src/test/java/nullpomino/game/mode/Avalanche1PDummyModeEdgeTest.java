package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;

class Avalanche1PDummyModeEdgeTest {
	private static class CM extends Avalanche1PDummyMode {}
	@Test void multiplierClamp999() throws Exception {
		CM m = new CM(); GameEngine e = fresh(m); e.createFieldIfNeeded(); e.chain=10;
		e.field.colorClearExtraCount=2000; m.calcScore(e,0,3); assertEquals(999, gf(m,"lastmultiplier"));
	}
	@Test void multiplierClamp1() throws Exception {
		CM m = new CM(); GameEngine e = fresh(m); e.createFieldIfNeeded(); e.chain=0;
		e.field.colorClearExtraCount=-5; m.calcScore(e,0,3); assertEquals(1, gf(m,"lastmultiplier"));
	}
	@Test void addBonus5Colors() throws Exception {
		CM m = new CM(); GameEngine e = fresh(m); sf(m,"numColors",5); sf(m,"zenKeshiCount",3);
		e.statistics.maxChain=2;
		Method me = Avalanche1PDummyMode.class.getDeclaredMethod("addBonus",GameEngine.class,int.class);
		me.setAccessible(true); me.invoke(m,e,0); assertEquals(9000, gf(m,"zenKeshiBonus"));
	}
	@Test void readyInitOutline2() throws Exception {
		CM m = new CM(); GameEngine e = fresh(m); e.createFieldIfNeeded(); sf(m,"outlinetype",2);
		Method me = Avalanche1PDummyMode.class.getDeclaredMethod("readyInit",GameEngine.class,int.class);
		me.setAccessible(true); me.invoke(m,e,0);
		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, e.blockOutlineType);
	}
	private static GameEngine fresh(Avalanche1PDummyMode m) {
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
