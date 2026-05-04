package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.Test;

class ToolVSMapEditModeEdgeTest {
	@Test void cursor0Edit() throws Exception {
		ToolVSMapEditMode m = new ToolVSMapEditMode(); GameEngine e = fresh(m);
		m.modeInit(m.owner); m.playerInit(e,0); sf(m,"menuCursor",0); sf(m,"menuTime",5);
		Controller c = new Controller(); c.buttonPress[Controller.BUTTON_A]=true; e.ctrl=c;
		m.onSetting(e,0); assertEquals(GameEngine.Status.FIELDEDIT, e.stat);
	}
	@Test void cursor2Clear() throws Exception {
		ToolVSMapEditMode m = new ToolVSMapEditMode(); GameEngine e = fresh(m);
		m.modeInit(m.owner); m.playerInit(e,0); sf(m,"menuCursor",2); sf(m,"menuTime",5);
		e.createFieldIfNeeded(); e.field.setBlock(0,0,new Block(Block.BLOCK_COLOR_RED));
		Controller c = new Controller(); c.buttonPress[Controller.BUTTON_A]=true; e.ctrl=c;
		m.onSetting(e,0); assertTrue(e.field.getBlockEmpty(0,0));
	}
	@Test void dPlusEExits() throws Exception {
		ToolVSMapEditMode m = new ToolVSMapEditMode(); GameEngine e = fresh(m);
		m.modeInit(m.owner); m.playerInit(e,0); sf(m,"menuTime",5);
		Controller c = new Controller(); c.buttonPress[Controller.BUTTON_D]=true; c.buttonPress[Controller.BUTTON_E]=true; e.ctrl=c;
		m.onSetting(e,0); assertTrue(e.quitflag);
	}
	private static GameEngine fresh(ToolVSMapEditMode m) {
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
