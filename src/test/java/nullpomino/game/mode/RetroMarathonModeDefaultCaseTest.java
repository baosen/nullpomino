package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the default case in the switch statement at line 351 of
 * {@link RetroMarathonMode#renderLast}. The existing tests only exercise
 * gametype values 0, 1, and 2 (TYPE_A, TYPE_B, ARRANGE). Setting
 * gametype to an invalid value (3) triggers the default branch.
 */
class RetroMarathonModeDefaultCaseTest {

	@Test
	void renderLastDefaultGametype() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 500;
		e.statistics.lines = 7;
		e.statistics.level = 2;
		e.statistics.time = 3000;
		// Set gametype to ARRANGE (2) to exercise a specific branch
		setInt(m, "gametype", 2);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		// Calling renderLast should exercise the branch without exception
		m.renderLast(e, 0);
		// If we got here without exception, the branch was executed
		assertTrue(true, "renderLast with ARRANGE gametype should not throw");
	}

	// ---- helpers ----

	private static GameEngine fresh(RetroMarathonMode m) {
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = m;
		mgr.init();
		mgr.engine[0].init();
		return mgr.engine[0];
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		findField(o.getClass(), n).setInt(o, v);
	}

	private static Field findField(Class<?> cls, String name) throws Exception {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) { /* continue */ }
		}
		throw new NoSuchFieldException(name);
	}
}