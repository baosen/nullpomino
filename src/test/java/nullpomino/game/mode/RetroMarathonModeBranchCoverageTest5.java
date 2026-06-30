package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Additional branch coverage for {@link RetroMarathonMode} targeting the
 * settings-menu wraparound branches (startlevel / startheight), the
 * background-clamp branch in {@link RetroMarathonMode#playerInit}, the meter
 * threshold ladders in {@link RetroMarathonMode#calcScore} for both B-Type and
 * the standard (modulo-10) game types, and the level-up level clamp.
 */
class RetroMarathonModeBranchCoverageTest5 {

	private static GameEngine fresh(RetroMarathonMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Set menuCursor, inject a single LEFT/RIGHT press, then run onSetting. */
	private static void change(RetroMarathonMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		mode.onSetting(engine, 0);
	}

	// ---- settings-menu wraparound (cursor 1 = startlevel, cursor 2 = startheight) ----

	@Test
	void startlevelWrapsAtBothBounds() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);

		// underflow: 0 - 1 < 0 -> 19
		setInt(mode, "startlevel", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(19, readInt(mode, "startlevel"));

		// overflow: 19 + 1 > 19 -> 0
		setInt(mode, "startlevel", 19);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"));
	}

	@Test
	void startheightWrapsAtBothBounds() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);

		// underflow: 0 - 1 < 0 -> 5
		setInt(mode, "startheight", 0);
		change(mode, engine, 2, Controller.BUTTON_LEFT);
		assertEquals(5, readInt(mode, "startheight"));

		// overflow: 5 + 1 > 5 -> 0
		setInt(mode, "startheight", 5);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startheight"));
	}

	@Test
	void gametypeWrapsAtBothBounds() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);

		// underflow: 0 - 1 < 0 -> GAMETYPE_MAX-1 (2)
		setInt(mode, "gametype", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "gametype"));

		// overflow: 2 + 1 > 2 -> 0
		setInt(mode, "gametype", 2);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "gametype"));
	}

	// ---- playerInit background clamp (startlevel > 19 in config) ----

	@Test
	void playerInitClampsBackgroundWhenStartlevelAbove19() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		// loadSetting reads startlevel from modeConfig; force it above 19 so the
		// bg = startlevel assignment overshoots and the bg > 19 clamp fires.
		engine.owner.modeConfig.setProperty("retromarathon.startlevel", 25);
		mode.playerInit(engine, 0);
		assertEquals(19, engine.owner.backgroundStatus.bg);
	}

	// ---- calcScore B-Type meter color ladder (lines 424-426) ----

	@Test
	void calcScoreBTypeMeterYellowAt10() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 1); // B-Type
		engine.statistics.lines = 10; // >=10 yellow, <15 stops there
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void calcScoreBTypeMeterOrangeAt15() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 1);
		engine.statistics.lines = 15; // >=15 orange, <20 stops there
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void calcScoreBTypeMeterRedAt20() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 1);
		engine.statistics.lines = 20; // >=20 red
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	// ---- calcScore standard meter color ladder via lines % 10 (lines 432-434) ----

	@Test
	void calcScoreStandardMeterYellowAtMod2() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0); // Type A
		setInt(mode, "levellines", 99999); // suppress level-up so meter stands
		engine.statistics.lines = 2; // %10 == 2 -> yellow, <5 stops there
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void calcScoreStandardMeterOrangeAtMod5() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0);
		setInt(mode, "levellines", 99999);
		engine.statistics.lines = 5; // %10 == 5 -> orange, <8 stops there
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void calcScoreStandardMeterRedAtMod8() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0);
		setInt(mode, "levellines", 99999);
		engine.statistics.lines = 8; // %10 == 8 -> red
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	// ---- calcScore level-up level clamp (lines 450-451) ----

	@Test
	void calcScoreLevelUpClampsFadeBgAtHighLevel() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0); // Type A levels up on lines >= levellines
		setInt(mode, "levellines", 0); // any line count triggers level-up
		engine.statistics.level = 30; // after ++ -> 31; lv>=19 clamp -> 19
		engine.statistics.lines = 1;
		mode.calcScore(engine, 0, 0);
		assertEquals(19, engine.owner.backgroundStatus.fadebg);
	}

	@Test
	void calcScoreLevelUpFadeBgPassThroughLowLevel() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0);
		setInt(mode, "levellines", 0);
		engine.statistics.level = 4; // after ++ -> 5; within [0,19) so unclamped
		engine.statistics.lines = 1;
		mode.calcScore(engine, 0, 0);
		assertEquals(5, engine.owner.backgroundStatus.fadebg);
	}

	// --- reflection helpers ---

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
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
