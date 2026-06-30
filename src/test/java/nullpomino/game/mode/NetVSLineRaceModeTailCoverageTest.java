package nullpomino.game.mode;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tail coverage for {@link NetVSLineRaceMode#renderLast}: the place rendering
 * branches (1ST..6TH) in both the menu-font layout and the small/direct-font
 * layout, exercised by computing each place 0..5 via line counts.
 */
class NetVSLineRaceModeTailCoverageTest {

	private NetVSLineRaceMode mode;
	private GameManager manager;
	private GameEngine engine;

	@BeforeEach
	void setUp() throws Exception {
		mode = new NetVSLineRaceMode();
		manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		for (int i = 0; i < manager.engine.length; i++) {
			manager.engine[i].init();
			manager.engine[i].createFieldIfNeeded();
		}
		engine = manager.engine[0];
		engine.stat = GameEngine.Status.MOVE;
		engine.isVisible = true;

		set(mode, "netvsPlayerExist", new boolean[]{true, true, true, true, true, true});
		set(mode, "netvsPlayerDead", new boolean[]{false, false, false, false, false, false});
		set(mode, "netvsIsGameActive", true);
		set(mode, "goalLines", 40);
	}

	/**
	 * Give player 0 fewer lines than {@code numAhead} of the other 5 players,
	 * so getNowPlayerPlace(engine0) == numAhead.
	 */
	private void setPlaceTo(int numAhead) {
		manager.engine[0].statistics.lines = 1;
		for (int i = 1; i < manager.engine.length; i++) {
			manager.engine[i].statistics.lines = (i <= numAhead) ? 30 : 0;
		}
	}

	// ---- place 0..5 in the menu-font layout (lines 206-218) ----
	@Test
	void renderLastPlaceMenuLayoutAllRanks() throws Exception {
		engine.displaysize = 0;
		for (int place = 0; place <= 5; place++) {
			setPlaceTo(place);
			mode.renderLast(engine, 0);
		}
	}

	// ---- place 0..5 in the small/direct-font layout (lines 219-233) ----
	@Test
	void renderLastPlaceDirectLayoutAllRanks() throws Exception {
		engine.displaysize = -1;
		for (int place = 0; place <= 5; place++) {
			setPlaceTo(place);
			mode.renderLast(engine, 0);
		}
	}

	// ---- dead player path uses netvsPlayerPlace (line 203) for high ranks ----
	@Test
	void renderLastDeadPlayerUsesStoredPlace() throws Exception {
		engine.displaysize = 0;
		set(mode, "netvsPlayerDead", new boolean[]{true, false, false, false, false, false});
		for (int place = 0; place <= 5; place++) {
			set(mode, "netvsPlayerPlace", new int[]{place, 0, 0, 0, 0, 0});
			mode.renderLast(engine, 0);
		}
	}

	// ================================================================
	// Helpers
	// ================================================================

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name + " in " + cls.getName());
	}

	private static void set(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}
}
