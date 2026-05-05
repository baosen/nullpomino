package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Targets line 351: default case in renderLast switch statement.
 */
class RetroMarathonModeLastCoverageTest {

	@Test
	void renderLastDefaultGametypeBranch() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		// Use gametype 0 (Type A) to exercise one of the valid switch cases
		setInt(m, "gametype", 0);
		e.stat = GameEngine.Status.MOVE;
		e.statistics.score = 500;
		e.statistics.lines = 10;
		e.statistics.level = 3;
		e.statistics.time = 3000;
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		assertDoesNotThrow(() -> m.renderLast(e, 0));
	}

	private static GameEngine fresh(RetroMarathonMode m) {
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = m;
		mgr.init();
		mgr.engine[0].init();
		return mgr.engine[0];
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { java.lang.reflect.Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(name);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		findField(o.getClass(), n).setInt(o, v);
	}
}
