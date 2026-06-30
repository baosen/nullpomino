package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link GemManiaMode#onSetting} across every numeric configuration
 * cursor in all three menu screens (edit-main {@code editModeScreen==1},
 * edit-stage {@code editModeScreen==2}, and the normal options menu), pushing
 * each value past both its lower and upper bound so the wraparound branches
 * (value &lt; min and value &gt; max) all fire. Also exercises the BUTTON_E
 * (x100) and BUTTON_F (x1000) multipliers on the edit-stage screen and the
 * boolean-toggle cases on the normal menu.
 */
class GemManiaModeSettingWraparoundTest {

	private static final int MAX_STAGE_TOTAL = 27;

	private static GameEngine freshEngine(GemManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/**
	 * Sets editModeScreen + menuCursor, injects a single LEFT/RIGHT (and optional
	 * E/F multiplier) press, then runs onSetting. menuTime is left at 0 so that
	 * the {@code isPush(BUTTON_A) && menuTime >= 5} decide blocks never fire.
	 */
	private static void change(GemManiaMode mode, GameEngine engine, int editScreen,
			int cursor, int dirButton, int multButton) throws Exception {
		setInt(mode, "editModeScreen", editScreen);
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		// reset() clears both buttonPress and buttonTime; clearButtonState() would
		// leave stale buttonTime entries so a prior LEFT keeps overriding RIGHT.
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		if(multButton >= 0) {
			engine.ctrl.buttonPress[multButton] = true;
			engine.ctrl.buttonTime[multButton] = 1;
		}
		mode.onSetting(engine, 0);
	}

	// ----------------------------------------------------------------------
	// editModeScreen == 1 (edit main menu): startstage (case 1/2), stageset (3/4)
	// ----------------------------------------------------------------------

	@Test
	void editMainStartStageWrapsAtBothBounds() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 1: startstage underflow 0 - 1 -> MAX_STAGE_TOTAL - 1
		setInt(mode, "startstage", 0);
		change(mode, engine, 1, 1, Controller.BUTTON_LEFT, -1);
		assertEquals(MAX_STAGE_TOTAL - 1, readInt(mode, "startstage"));

		// case 2: startstage overflow (MAX-1) + 1 -> 0
		setInt(mode, "startstage", MAX_STAGE_TOTAL - 1);
		change(mode, engine, 1, 2, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "startstage"));
	}

	@Test
	void editMainStageSetWrapsAtBothBounds() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 3: stageset underflow 0 - 1 -> 99
		setInt(mode, "stageset", 0);
		change(mode, engine, 1, 3, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readInt(mode, "stageset"));

		// case 4: stageset overflow 99 + 1 -> 0
		setInt(mode, "stageset", 99);
		change(mode, engine, 1, 4, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "stageset"));
	}

	// ----------------------------------------------------------------------
	// editModeScreen == 2 (edit stage menu): stagetimeStart (1), limittimeStart (2),
	// stagebgm (3), gimmickMirror (4) + E/F multipliers.
	// ----------------------------------------------------------------------

	@Test
	void editStageStageTimeWrapsWithMultipliers() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 1: underflow with x100 multiplier (BUTTON_E): 0 - 60*100 < 0 -> 3600*20
		setInt(mode, "stagetimeStart", 0);
		change(mode, engine, 2, 1, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(3600 * 20, readInt(mode, "stagetimeStart"));

		// case 1: overflow with x1000 multiplier (BUTTON_F): max + 60*1000 > max -> 0
		setInt(mode, "stagetimeStart", 3600 * 20);
		change(mode, engine, 2, 1, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		assertEquals(0, readInt(mode, "stagetimeStart"));
	}

	@Test
	void editStageLimitTimeWrapsAtBothBounds() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 2: underflow 0 - 60 < 0 -> 3600*20
		setInt(mode, "limittimeStart", 0);
		change(mode, engine, 2, 2, Controller.BUTTON_LEFT, -1);
		assertEquals(3600 * 20, readInt(mode, "limittimeStart"));

		// case 2: overflow max + 60 > max -> 0
		setInt(mode, "limittimeStart", 3600 * 20);
		change(mode, engine, 2, 2, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "limittimeStart"));
	}

	@Test
	void editStageBgmWrapsAtBothBounds() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 3: stagebgm underflow 0 - 1 -> BGM_COUNT - 1
		setInt(mode, "stagebgm", 0);
		change(mode, engine, 2, 3, Controller.BUTTON_LEFT, -1);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "stagebgm"));

		// case 3: stagebgm overflow (BGM_COUNT-1) + 1 -> 0
		setInt(mode, "stagebgm", BGMStatus.BGM_COUNT - 1);
		change(mode, engine, 2, 3, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "stagebgm"));
	}

	@Test
	void editStageGimmickMirrorWrapsAtBothBounds() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 4: gimmickMirror underflow 0 - 1 -> 99
		setInt(mode, "gimmickMirror", 0);
		change(mode, engine, 2, 4, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readInt(mode, "gimmickMirror"));

		// case 4: gimmickMirror overflow 99 + 1 -> 0
		setInt(mode, "gimmickMirror", 99);
		change(mode, engine, 2, 4, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "gimmickMirror"));
	}

	// ----------------------------------------------------------------------
	// Normal options menu (editModeScreen == 0, replayMode == false).
	// ----------------------------------------------------------------------

	@Test
	void normalStartStageWrapsAtBothBounds() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 0: startstage underflow 0 - 1 -> MAX_STAGE_TOTAL - 1
		setInt(mode, "startstage", 0);
		change(mode, engine, 0, 0, Controller.BUTTON_LEFT, -1);
		assertEquals(MAX_STAGE_TOTAL - 1, readInt(mode, "startstage"));

		// case 0: startstage overflow (MAX-1) + 1 -> 0
		setInt(mode, "startstage", MAX_STAGE_TOTAL - 1);
		change(mode, engine, 0, 0, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "startstage"));
	}

	@Test
	void normalStageSetWrapsAtBothBounds() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 1: stageset underflow -2 -> 99 (boundary: stageset < -1)
		setInt(mode, "stageset", -1);
		change(mode, engine, 0, 1, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readInt(mode, "stageset"));

		// case 1: stageset overflow 99 + 1 -> -1
		setInt(mode, "stageset", 99);
		change(mode, engine, 0, 1, Controller.BUTTON_RIGHT, -1);
		assertEquals(-1, readInt(mode, "stageset"));
	}

	@Test
	void normalBooleanTogglesFlip() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 2: alwaysghost
		setBool(mode, "alwaysghost", false);
		change(mode, engine, 0, 2, Controller.BUTTON_RIGHT, -1);
		assertEquals(true, readBool(mode, "alwaysghost"));

		// case 3: always20g
		setBool(mode, "always20g", false);
		change(mode, engine, 0, 3, Controller.BUTTON_RIGHT, -1);
		assertEquals(true, readBool(mode, "always20g"));

		// case 4: lvstopse (defaults true -> flips to false)
		setBool(mode, "lvstopse", true);
		change(mode, engine, 0, 4, Controller.BUTTON_RIGHT, -1);
		assertEquals(false, readBool(mode, "lvstopse"));

		// case 5: showsectiontime
		setBool(mode, "showsectiontime", false);
		change(mode, engine, 0, 5, Controller.BUTTON_RIGHT, -1);
		assertEquals(true, readBool(mode, "showsectiontime"));

		// case 6: randomnext
		setBool(mode, "randomnext", false);
		change(mode, engine, 0, 6, Controller.BUTTON_RIGHT, -1);
		assertEquals(true, readBool(mode, "randomnext"));
	}

	@Test
	void normalTrainingTypeWrapsAtBothBounds() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 7: trainingType underflow 0 - 1 -> 2
		setInt(mode, "trainingType", 0);
		change(mode, engine, 0, 7, Controller.BUTTON_LEFT, -1);
		assertEquals(2, readInt(mode, "trainingType"));

		// case 7: trainingType overflow 2 + 1 -> 0
		setInt(mode, "trainingType", 2);
		change(mode, engine, 0, 7, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "trainingType"));
	}

	@Test
	void normalStartNextCountWrapsAtBothBounds() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int maxNext = nextListMax();

		// case 8: startnextc underflow 0 - 1 -> length - 1
		setInt(mode, "startnextc", 0);
		change(mode, engine, 0, 8, Controller.BUTTON_LEFT, -1);
		assertEquals(maxNext, readInt(mode, "startnextc"));

		// case 8: startnextc overflow (length-1) + 1 -> 0
		setInt(mode, "startnextc", maxNext);
		change(mode, engine, 0, 8, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "startnextc"));
	}

	/** Reads STRING_DEFAULT_NEXT_LIST.length() - 1 via reflection. */
	private static int nextListMax() throws Exception {
		Field f = field(GemManiaMode.class, "STRING_DEFAULT_NEXT_LIST");
		String s = (String) f.get(null);
		return s.length() - 1;
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
