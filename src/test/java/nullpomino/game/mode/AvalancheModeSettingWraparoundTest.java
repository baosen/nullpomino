package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link AvalancheMode#onSetting} across the settings menu so the
 * wraparound branches (value &lt; min and value &gt; max) of every numeric
 * cursor fire in BOTH directions, the cursor-navigation skip/wrap branches
 * (UP from 0, DOWN from 10, skip of cursor 1 when gametype != SPRINT) fire,
 * and the BUTTON_A "decide" exit branch fires.
 *
 * The menu uses {@code isMenuRepeatKey} which is satisfied by
 * {@code buttonTime[key] == 1}; we set the button via {@code ctrl.reset()}
 * (NOT {@code clearButtonState()}, which leaves stale buttonTime so a prior
 * LEFT keeps overriding RIGHT).
 */
class AvalancheModeSettingWraparoundTest {

	private static GameEngine freshEngine(AvalancheMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single direction press, then runs onSetting. */
	private static void change(AvalancheMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		mode.onSetting(engine, 0);
	}

	// -----------------------------------------------------------------------
	// Cursor navigation: UP wrap (L137), DOWN wrap (L144), case-1 skip (L138/L145)
	// -----------------------------------------------------------------------

	@Test
	void cursorUpFromZeroWrapsToTen() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 2); // SPRINT: cursor-1 skip not triggered here
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.onSetting(engine, 0);

		assertEquals(10, readInt(mode, "menuCursor"));
	}

	@Test
	void cursorDownFromTenWrapsToZero() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 2);
		setInt(mode, "menuCursor", 10);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.onSetting(engine, 0);

		assertEquals(0, readInt(mode, "menuCursor"));
	}

	@Test
	void cursorUpSkipsSprintTargetWhenNotSprint() throws Exception {
		// gametype != 2 (not SPRINT): moving UP from cursor 2 lands on 1 then
		// the L138 else-if decrements it again so cursor-1 (TARGET) is skipped.
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 0); // MARATHON
		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.onSetting(engine, 0);

		assertEquals(0, readInt(mode, "menuCursor"));
	}

	@Test
	void cursorDownSkipsSprintTargetWhenNotSprint() throws Exception {
		// gametype != 2: moving DOWN from cursor 0 lands on 1 then the L145
		// else-if increments it again so cursor-1 (TARGET) is skipped.
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 0); // MARATHON
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.onSetting(engine, 0);

		assertEquals(2, readInt(mode, "menuCursor"));
	}

	// -----------------------------------------------------------------------
	// case 0: gametype wrap at both bounds
	// -----------------------------------------------------------------------

	@Test
	void gametypeWrapsAtBothBounds() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "gametype")); // GAMETYPE_MAX-1

		setInt(mode, "gametype", 2);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "gametype"));
	}

	// -----------------------------------------------------------------------
	// case 1: sprintTarget wrap (L166/L167) at both bounds
	// -----------------------------------------------------------------------

	@Test
	void sprintTargetWrapsAtBothBounds() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// SPRINT_MAX_SCORE.length == 5 -> valid indices 0..4
		setInt(mode, "sprintTarget", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(4, readInt(mode, "sprintTarget"));

		setInt(mode, "sprintTarget", 4);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "sprintTarget"));
	}

	// -----------------------------------------------------------------------
	// case 2: scoreType wrap (L171/L172) at both bounds
	// -----------------------------------------------------------------------

	@Test
	void scoreTypeWrapsAtBothBounds() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// SCORETYPE_MAX == 2 -> indices 0..1
		setInt(mode, "scoreType", 0);
		change(mode, engine, 2, Controller.BUTTON_LEFT);
		assertEquals(1, readInt(mode, "scoreType"));

		setInt(mode, "scoreType", 1);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "scoreType"));
	}

	// -----------------------------------------------------------------------
	// case 3: numColors wrap (L176/L177) at both bounds (range 3..5)
	// -----------------------------------------------------------------------

	@Test
	void numColorsWrapsAtBothBounds() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "numColors", 3);
		change(mode, engine, 3, Controller.BUTTON_LEFT);
		assertEquals(5, readInt(mode, "numColors"));

		setInt(mode, "numColors", 5);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertEquals(3, readInt(mode, "numColors"));
	}

	// -----------------------------------------------------------------------
	// case 4 & 5: boolean toggles
	// -----------------------------------------------------------------------

	@Test
	void dangerColumnDoubleToggles() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "dangerColumnDouble", false);
		change(mode, engine, 4, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "dangerColumnDouble"));
	}

	@Test
	void dangerColumnShowXToggles() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "dangerColumnShowX", false);
		change(mode, engine, 5, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "dangerColumnShowX"));
	}

	// -----------------------------------------------------------------------
	// case 6: colorClearSize wrap (L187/L188) at both bounds (range 2..36)
	// -----------------------------------------------------------------------

	@Test
	void colorClearSizeWrapsAtBothBounds() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.colorClearSize = 2;
		change(mode, engine, 6, Controller.BUTTON_LEFT);
		assertEquals(36, engine.colorClearSize);

		engine.colorClearSize = 36;
		change(mode, engine, 6, Controller.BUTTON_RIGHT);
		assertEquals(2, engine.colorClearSize);
	}

	// -----------------------------------------------------------------------
	// case 7 & 8: boolean toggles (case 8 fills the missing switch case L157)
	// -----------------------------------------------------------------------

	@Test
	void cascadeSlowToggles() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "cascadeSlow", false);
		change(mode, engine, 7, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "cascadeSlow"));
	}

	@Test
	void bigDisplayToggles() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "bigDisplay", false);
		change(mode, engine, 8, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "bigDisplay"));
	}

	// -----------------------------------------------------------------------
	// case 9: outlinetype wrap (L198/L199) at both bounds (range 0..2)
	// -----------------------------------------------------------------------

	@Test
	void outlineTypeWrapsAtBothBounds() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "outlinetype", 0);
		change(mode, engine, 9, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "outlinetype"));

		setInt(mode, "outlinetype", 2);
		change(mode, engine, 9, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "outlinetype"));
	}

	// -----------------------------------------------------------------------
	// case 10: showChains toggle
	// -----------------------------------------------------------------------

	@Test
	void showChainsToggles() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "showChains", true);
		change(mode, engine, 10, Controller.BUTTON_RIGHT);
		assertFalse(readBool(mode, "showChains"));
	}

	// -----------------------------------------------------------------------
	// BUTTON_A decide exit (L208 TRUE: menuTime >= 5 && isPush(A))
	// -----------------------------------------------------------------------

	@Test
	void decideButtonExitsSettings() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 5);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1; // isPush -> ==1

		// onSetting returns false (leaves settings) when A pressed and menuTime>=5
		assertFalse(mode.onSetting(engine, 0));
	}

	// -----------------------------------------------------------------------
	// reflection helpers
	// -----------------------------------------------------------------------

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
