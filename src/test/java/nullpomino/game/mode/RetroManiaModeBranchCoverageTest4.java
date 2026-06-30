package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Additional branch coverage for {@link RetroManiaMode}: settings-menu
 * wraparound (gametype / startlevel) and the BIG / POWERON toggles, the
 * background clamp in {@link RetroManiaMode#playerInit}, the version-gated
 * max-out logic in {@link RetroManiaMode#onLast}, the soft-drop early-return,
 * the wide-next-display leaderboard layout in {@link RetroManiaMode#renderLast},
 * and the level-up fade-bg clamp in {@link RetroManiaMode#calcScore}.
 */
class RetroManiaModeBranchCoverageTest4 {

	/** EventReceiver pretending to use the wide (big side-next) layout. */
	private static final class WideNextReceiver extends EventReceiver {
		@Override public int getNextDisplayType() { return 2; }
	}

	private static GameEngine fresh(RetroManiaMode mode) {
		return fresh(mode, new EventReceiver());
	}

	private static GameEngine fresh(RetroManiaMode mode, EventReceiver receiver) {
		GameManager manager = new GameManager(receiver);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void change(RetroManiaMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		mode.onSetting(engine, 0);
	}

	// ---- settings wraparound ----

	@Test
	void gametypeWrapsAtBothBounds() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(3, readInt(mode, "gametype")); // GAMETYPE_MAX-1

		setInt(mode, "gametype", 3);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "gametype"));
	}

	@Test
	void startlevelWrapsAtBothBounds() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(15, readInt(mode, "startlevel"));

		setInt(mode, "startlevel", 15);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"));
	}

	@Test
	void bigToggleCase2() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "big", false);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "big"));
	}

	@Test
	void poweronToggleCase3() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "poweron", false);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "poweron"));
	}

	// ---- playerInit background clamp (startlevel/2 > 19) ----

	@Test
	void playerInitClampsBackgroundWhenStartlevelHigh() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		// startlevel/2 = 25 > 19 forces the clamp branch.
		engine.owner.modeConfig.setProperty("retromania.startlevel", 50);
		mode.playerInit(engine, 0);
		assertEquals(19, engine.owner.backgroundStatus.bg);
	}

	// ---- onLast version gate (version < 2 -> skip max-out) ----

	@Test
	void onLastSkipsMaxOutWhenVersionBelow2() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1); // pre-versioned replay: no clamping
		engine.statistics.score = 5_000_000;
		engine.statistics.lines = 5000;
		engine.statistics.level = 500;
		mode.onLast(engine, 0);
		assertEquals(5_000_000, engine.statistics.score); // untouched
	}

	@Test
	void onLastClampsWhenVersion2() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0); // version defaults to CURRENT_VERSION (2)
		engine.statistics.score = 5_000_000;
		engine.statistics.lines = 5000;
		engine.statistics.level = 500;
		mode.onLast(engine, 0);
		assertEquals(999999, engine.statistics.score);
		assertEquals(999, engine.statistics.lines);
		assertEquals(99, engine.statistics.level);
	}

	// ---- afterSoftDropFall version-2 fast-drop early return (line 400) ----

	@Test
	void afterSoftDropFallEarlyReturnAtMaxGravity() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0); // version 2
		engine.speed.denominator = 1; // 20G-equivalent: soft drop scores nothing
		engine.statistics.score = 100;
		mode.afterSoftDropFall(engine, 0, 7);
		assertEquals(100, engine.statistics.score); // early return, no score added
	}

	@Test
	void afterSoftDropFallScoresWhenNotMaxGravity() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		engine.speed.denominator = 5; // not 1 -> soft drop scores
		engine.statistics.score = 100;
		mode.afterSoftDropFall(engine, 0, 7);
		assertEquals(107, engine.statistics.score);
	}

	// ---- renderLast wide-next leaderboard layout (lines 280/281 ternaries) ----

	@Test
	void renderLastWideNextLeaderboardLayout() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode, new WideNextReceiver());
		mode.playerInit(engine, 0);
		// SETTING + replayMode false + big false + startlevel 0 + ai null -> board
		setBool(mode, "big", false);
		setInt(mode, "startlevel", 0);
		setInt(mode, "rankingRank", 0); // highlight first row (i == rankingRank true)
		engine.ai = null;
		mode.renderLast(engine, 0); // exercises scale 0.5 / topY 6 branches
		assertTrue(true);
	}

	// ---- calcScore level-up fade-bg clamp (lines 382/383) ----

	@Test
	void calcScoreLevelUpClampsFadeBgAtHighLevel() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		// Make the field non-empty so the perfect-clear bonus path is skipped.
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 50; // after ++ -> 51; /2 = 25 > 19 -> clamp to 19
		setInt(mode, "linesAfterLastLevelUp", 3);
		mode.calcScore(engine, 0, 1); // 1 line -> linesAfter=4 -> level up
		assertEquals(19, engine.owner.backgroundStatus.fadebg);
	}

	@Test
	void calcScoreLevelUpFadeBgPassThroughLowLevel() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 8; // after ++ -> 9; /2 = 4 (<=19) unclamped
		setInt(mode, "linesAfterLastLevelUp", 3);
		mode.calcScore(engine, 0, 1);
		assertEquals(4, engine.owner.backgroundStatus.fadebg);
	}

	// --- reflection helpers ---

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
	}

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
