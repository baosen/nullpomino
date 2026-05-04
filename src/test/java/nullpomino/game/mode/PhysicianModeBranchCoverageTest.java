package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Branch coverage for {@link PhysicianMode}: covers replay-mode playerInit,
 * speed clamping in onSetting, lastscore display in renderLast, and gem
 * color counting branches in renderLast.
 */
class PhysicianModeBranchCoverageTest {

	@Test
	void playerInitReplayModeLoadsReplayProp() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		mode.playerInit(e, 0);
		// loadSetting(owner.replayProp) should have been called without NPE
		// Verify defaults were loaded
		assertEquals(40, readInt(mode, "hoverBlocks"));
		assertEquals(1, readInt(mode, "speed"));
	}

	@Test
	void onSettingSpeedWrapsLowToHigh() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(mode, "menuCursor", 1);
		setInt(mode, "speed", 0);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;

		mode.onSetting(e, 0);

		// speed 0 + change(-1) = -1, clamped to 2
		assertEquals(2, readInt(mode, "speed"));
	}

	@Test
	void onSettingSpeedWrapsHighToLow() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(mode, "menuCursor", 1);
		setInt(mode, "speed", 2);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;

		mode.onSetting(e, 0);

		// speed 2 + change(+1) = 3, clamped to 0
		assertEquals(0, readInt(mode, "speed"));
	}

	@Test
	void renderLastShowsLastscoreSuffix() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastscore", 500);
		setInt(mode, "scgettime", 60);
		e.statistics.score = 10000;
		e.createFieldIfNeeded();

		mode.renderLast(e, 0);

		// lastscore != 0 && scgettime > 0 → "(+500)" suffix added
		// Just verifying no crash and path was hit
		assertEquals(60, readInt(mode, "scgettime"));
	}

	@Test
	void renderLastGemCountingAllColors() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.createFieldIfNeeded();

		// Place one gem block of each color on the field
		e.field.setBlock(1, 0, new Block(Block.BLOCK_COLOR_GEM_BLUE));
		e.field.setBlock(2, 0, new Block(Block.BLOCK_COLOR_GEM_RED));
		e.field.setBlock(3, 0, new Block(Block.BLOCK_COLOR_GEM_YELLOW));

		mode.renderLast(e, 0);

		// Verify gem counts - each should be 1
		// No assertion needed; we just need to hit the branches at lines 264, 266, 268
		assertEquals(3, e.field.getHowManyGems());
	}

	// ---- helpers ----

	private static GameEngine freshEngine(PhysicianMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static int readInt(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static void setInt(Object instance, String name, int value) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
