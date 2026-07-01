package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link ToolVSMapEditMode#onSetting} edit-menu branches: the nowMapID /
 * nowMapSetID wraparound, and the SAVE/LOAD/DELETE/WRITE/READ decide arms
 * (including the valid vs. out-of-range nowMapID guards). The WRITE path is run
 * against a non-persisting receiver so the TRACKED config/map/vsbattle files are
 * never written.
 */
class ToolVSMapEditModeSettingBranchTest {

	/** No-op persistence so saveAllMaps never writes the tracked config/map files. */
	private static final class NonPersistingReceiver extends EventReceiver {
		@Override public boolean saveProperties(String filename, CustomProperties prop) { return true; }
	}

	private static GameEngine freshEngine(ToolVSMapEditMode mode) {
		GameManager manager = new GameManager(new NonPersistingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		manager.engine[0].createFieldIfNeeded();
		return manager.engine[0];
	}

	private static void nav(ToolVSMapEditMode mode, GameEngine engine, int cursor, int dir) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dir] = true;
		engine.ctrl.buttonTime[dir] = 1;
		mode.onSetting(engine, 0);
	}

	private static void confirm(ToolVSMapEditMode mode, GameEngine engine, int cursor) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		mode.onSetting(engine, 0);
	}

	@SuppressWarnings("unchecked")
	private static List<Object> mapList(ToolVSMapEditMode mode) throws Exception {
		return (List<Object>) field(mode.getClass(), "listFields").get(mode);
	}

	@Test
	void nowMapIdAndSetIdWrapAtBothBounds() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		// Give listFields a couple of entries via SAVE (nowMapID out of range -> add).
		setInt(mode, "nowMapID", 999);
		confirm(mode, engine, 3);
		confirm(mode, engine, 3);
		int size = mapList(mode).size();
		assertTrue(size >= 1);

		// cursor 3 nowMapID wrap: < 0 -> listFields.size(); > size -> 0
		setInt(mode, "nowMapID", 0);
		nav(mode, engine, 3, Controller.BUTTON_LEFT);
		assertEquals(size, readInt(mode, "nowMapID"));
		setInt(mode, "nowMapID", size);
		nav(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "nowMapID"));

		// cursor 6 nowMapSetID wrap (0-99)
		setInt(mode, "nowMapSetID", 0);
		nav(mode, engine, 6, Controller.BUTTON_LEFT);
		assertEquals(99, readInt(mode, "nowMapSetID"));
		setInt(mode, "nowMapSetID", 99);
		nav(mode, engine, 6, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "nowMapSetID"));
	}

	@Test
	void decideArmsCoverValidAndInvalidMapId() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int base = mapList(mode).size();

		// SAVE with out-of-range id -> add a new field
		setInt(mode, "nowMapID", 999);
		confirm(mode, engine, 3);
		assertEquals(base + 1, mapList(mode).size());

		// SAVE with valid id -> overwrite existing (no size change)
		setInt(mode, "nowMapID", 0);
		confirm(mode, engine, 3);
		assertEquals(base + 1, mapList(mode).size());

		// LOAD valid and invalid
		setInt(mode, "nowMapID", 0);
		confirm(mode, engine, 4);
		setInt(mode, "nowMapID", 999);
		confirm(mode, engine, 4);

		// GRAY->? (1), CLEAR (2)
		confirm(mode, engine, 1);
		confirm(mode, engine, 2);

		// DELETE valid -> removes and clamps nowMapID (asserted before WRITE/READ
		// so a map-file reload can't perturb the count).
		setInt(mode, "nowMapID", 0);
		int before = mapList(mode).size();
		confirm(mode, engine, 5);
		assertEquals(before - 1, mapList(mode).size());

		// WRITE (6, non-persisting) and READ (7) exercised last
		confirm(mode, engine, 6);
		confirm(mode, engine, 7);
	}

	@Test
	void confirmIgnoredWhenMenuTimeTooLow() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// BUTTON_A with menuTime < 5 -> guard false, list untouched (no SAVE-add)
		int base = mapList(mode).size();
		setInt(mode, "nowMapID", 999);
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		mode.onSetting(engine, 0);
		assertEquals(base, mapList(mode).size());
	}

	private static void setInt(Object o, String n, int v) throws Exception { field(o.getClass(), n).setInt(o, v); }
	private static int readInt(Object o, String n) throws Exception { return field(o.getClass(), n).getInt(o); }

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
