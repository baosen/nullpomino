package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.Test;

class PhysicianModeEdgeTest {
	@Test void replayModeAutoAdvances() throws Exception {
		PhysicianMode m = new PhysicianMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		e.owner.replayMode=true; sf(m,"menuTime",60); assertFalse(m.onSetting(e,0));
	}
	@Test void excellentNoGems() throws Exception {
		PhysicianMode m = new PhysicianMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		e.timerActive=true; e.createFieldIfNeeded(); sf(m,"hoverBlocks",40);
		m.onLast(e,0); assertFalse(e.timerActive);
	}
	@Test void meterColorGreen() throws Exception {
		PhysicianMode m = new PhysicianMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		e.createFieldIfNeeded(); sf(m,"hoverBlocks",100);
		e.field.setBlock(0,0,new Block(Block.BLOCK_COLOR_GEM_RED));
		e.field.setBlock(1,0,new Block(Block.BLOCK_COLOR_GEM_RED));
		m.onLast(e,0); assertEquals(GameEngine.METER_COLOR_GREEN, e.meterColor);
	}
	@Test void saveReplay() throws Exception {
		PhysicianMode m = new PhysicianMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		CustomProperties p = new CustomProperties(); m.saveReplay(e,0,p);
		assertEquals(40, p.getProperty("physician.hoverBlocks",-1));
	}
	@Test void startGameDefaults() throws Exception {
		PhysicianMode m = new PhysicianMode(); GameEngine e = fresh(m); m.playerInit(e,0);
		m.startGame(e,0); assertEquals(30, e.speed.are);
	}
	private static GameEngine fresh(PhysicianMode m) {
		GameManager mgr = new GameManager(new EventReceiver()); mgr.mode=m; mgr.init(); mgr.engine[0].init(); return mgr.engine[0];
	}
	private static void sf(Object o, String n, int v) throws Exception {
		Field f = ff(o.getClass(),n); f.setAccessible(true); f.setInt(o,v);
	}
	private static Field ff(Class<?> c, String n) throws NoSuchFieldException {
		while(c!=null){try{return c.getDeclaredField(n);}catch(NoSuchFieldException e){c=c.getSuperclass();}}
		throw new NoSuchFieldException(n);
	}
}
