package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the higher CO-medal upgrade branches of {@link FinalMode#calcScore}
 * that the existing FinalMode tests do not reach: the big-mode combo>=3/>=4
 * upgrades (medalCO 1->2 at 672-673, 2->3 at 675-676) and the normal-mode
 * combo>=5/>=7 upgrades (medalCO 1->2 at 683-684, 2->3 at 686-687).
 *
 * <p>Each branch is the second/third arm of an if/else-if chain, so it only
 * fires when {@code medalCO} already holds the preceding value; the test seeds
 * {@code medalCO} and {@code engine.combo} accordingly before calling calcScore.
 */
class FinalModeComboMedalBranchTest {

	private static GameEngine freshEngine(FinalMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		mode.playerInit(manager.engine[0], 0);
		manager.engine[0].createFieldIfNeeded();
		return manager.engine[0];
	}

	private static void setInt(Object o, String name, int v) throws Exception {
		Field f = FinalMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	private static int getInt(Object o, String name) throws Exception {
		Field f = FinalMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static void setBool(Object o, String name, boolean v) throws Exception {
		Field f = FinalMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(o, v);
	}

	@Test
	void bigModeComboUpgradesMedalCoToTwoThenThree() throws Exception {
		// medalCO 1 -> 2 (combo>=3) : line 672-673
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "big", true);
		setInt(mode, "medalCO", 1);
		engine.combo = 3;
		mode.calcScore(engine, 0, 1);
		assertEquals(2, getInt(mode, "medalCO"), "big combo>=3 upgrades medalCO 1->2");

		// medalCO 2 -> 3 (combo>=4) : line 675-676
		mode = new FinalMode();
		engine = freshEngine(mode);
		setBool(mode, "big", true);
		setInt(mode, "medalCO", 2);
		engine.combo = 4;
		mode.calcScore(engine, 0, 1);
		assertEquals(3, getInt(mode, "medalCO"), "big combo>=4 upgrades medalCO 2->3");
	}

	@Test
	void normalModeComboUpgradesMedalCoToTwoThenThree() throws Exception {
		// medalCO 1 -> 2 (combo>=5) : line 683-684
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "big", false);
		setInt(mode, "medalCO", 1);
		engine.combo = 5;
		mode.calcScore(engine, 0, 1);
		assertEquals(2, getInt(mode, "medalCO"), "normal combo>=5 upgrades medalCO 1->2");

		// medalCO 2 -> 3 (combo>=7) : line 686-687
		mode = new FinalMode();
		engine = freshEngine(mode);
		setBool(mode, "big", false);
		setInt(mode, "medalCO", 2);
		engine.combo = 7;
		mode.calcScore(engine, 0, 1);
		assertEquals(3, getInt(mode, "medalCO"), "normal combo>=7 upgrades medalCO 2->3");
	}
}
