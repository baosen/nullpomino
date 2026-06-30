package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers GemMania {@code onCustom} stage-exit branches that the existing tests
 * miss — the medium time-extend (cleartime 600..1200 -> +5s, line 1225) and the
 * three "last stage" selections for the final normal stage based on clear-rate
 * and time (laststage 19 at 1235, 24 at 1238-1239, MAX_STAGE_TOTAL-1 at 1241) —
 * plus the {@code onGameOver} field-graying loop (1420-1425).
 */
class GemManiaModeBranchCoverageTest2 {

	private static GameEngine fresh(GemManiaMode mode) {
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

	private static void set(Object o, String name, int v) throws Exception {
		Field f = GemManiaMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	private static void setB(Object o, String name, boolean v) throws Exception {
		Field f = GemManiaMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(o, v);
	}

	private static int get(Object o, String name) throws Exception {
		Field f = GemManiaMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(o);
	}

	@Test
	void onCustomMediumTimeExtend() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = fresh(mode);
		setB(mode, "clearflag", true);
		set(mode, "cleartime", 700);   // in [600,1200) -> +5s
		set(mode, "stage", 5);         // != MAX_STAGE_NORMAL-1 so no +60
		engine.statc[0] = 0;
		mode.onCustom(engine, 0);
		assertEquals(5, get(mode, "timeextendStageClearSeconds"),
				"cleartime in [600,1200) extends by 5 seconds");
	}

	@Test
	void onCustomLastStageLowClearRate() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = fresh(mode);
		setB(mode, "clearflag", true);
		set(mode, "stage", 19);        // MAX_STAGE_NORMAL-1
		set(mode, "clearstage", 0);    // ->1 after ++
		set(mode, "trystage", 10);     // ->11 -> clearper = 9 (<90)
		engine.statc[0] = 0;
		mode.onCustom(engine, 0);
		assertEquals(19, get(mode, "laststage"), "clearper<90 -> laststage 19");
	}

	@Test
	void onCustomLastStagePerfectSlow() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = fresh(mode);
		setB(mode, "clearflag", true);
		set(mode, "stage", 19);
		set(mode, "clearstage", 0);    // ->1
		set(mode, "trystage", 0);      // ->1 -> clearper = 100
		engine.statistics.time = 20000; // > 5*3600
		engine.statc[0] = 0;
		mode.onCustom(engine, 0);
		assertEquals(24, get(mode, "laststage"), "clearper==100 & slow -> laststage 24");
	}

	@Test
	void onCustomLastStagePerfectFast() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = fresh(mode);
		setB(mode, "clearflag", true);
		set(mode, "stage", 19);
		set(mode, "clearstage", 0);
		set(mode, "trystage", 0);      // clearper = 100
		engine.statistics.time = 100;  // <= 5*3600
		engine.statc[0] = 0;
		mode.onCustom(engine, 0);
		assertEquals(26, get(mode, "laststage"), "clearper==100 & fast -> laststage MAX_STAGE_TOTAL-1");
	}

	@Test
	void onGameOverGraysField() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = fresh(mode);
		int row = 5;
		engine.field.setBlock(3, row, new Block(Block.BLOCK_COLOR_RED));
		engine.statc[0] = row;          // graying loop processes this row
		mode.onGameOver(engine, 0);
		assertEquals(Block.BLOCK_COLOR_GRAY, engine.field.getBlockColor(3, row),
				"onGameOver grays out remaining blocks");
	}
}
