package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Covers remaining uncovered lines in {@link ToolVSMapEditMode}.
 *
 * Target lines in onSetting:
 *   164-166: nowMapID += change / bounds check (menuCursor 3-5)
 *   170-172: nowMapSetID += change / bounds check (menuCursor 6-7)
 *
 * Target lines in renderSetting:
 *   248: draw "NO MAPS" when listFields is empty
 *   251: draw cursor "b" at position 8..10 (menuCursor 3-5)
 *   260: draw cursor "b" at position 14..15 (menuCursor 6-7)
 *
 * IMPORTANT: updateCursor() returns -1 for LEFT press and 1 for RIGHT press.
 *            UP/DOWN only move the menuCursor, they don't change the return value.
 *            The nowMapID/nowMapSetID are modified by LEFT/RIGHT (change != 0).
 */
class ToolVSMapEditModeFinalCoverageTest {

	// ====================================================================
	// onSetting: menuCursor 3-5 (nowMapID change, lines 164-166)
	//
	// Pressing LEFT returns change=-1, pressing RIGHT returns change=1.
	// line 164: nowMapID += change
	// line 165: if(nowMapID < 0) nowMapID = listFields.size()
	// line 166: if(nowMapID > listFields.size()) nowMapID = 0
	// ====================================================================

	@Test
	void onSettingCursor3MapIDDecrement() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Clear listFields to ensure empty state
		@SuppressWarnings("unchecked")
		java.util.LinkedList<Object> list = (java.util.LinkedList<Object>) readFieldObj(mode, "listFields");
		list.clear();

		// Set menuCursor to 3 and nowMapID to 0
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 10);
		setInt(mode, "nowMapID", 0);

		// Press LEFT → change = -1 → nowMapID = -1 → wraps to listFields.size()
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;

		mode.onSetting(engine, 0);

		// listFields is empty (size 0), so nowMapID should be 0
		assertEquals(0, readInt(mode, "nowMapID"),
				"nowMapID should clamp to listFields.size() when negative");
	}

	@Test
	void onSettingCursor3MapIDIncrement() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Clear listFields to ensure empty state
		@SuppressWarnings("unchecked")
		java.util.LinkedList<Object> list = (java.util.LinkedList<Object>) readFieldObj(mode, "listFields");
		list.clear();

		// Set menuCursor to 3 and nowMapID to listFields.size() (empty = 0)
		// Press RIGHT → change = 1 → nowMapID = 1
		// Since 1 > listFields.size() (= 0), line 166: nowMapID = 0
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 10);
		setInt(mode, "nowMapID", 0);

		// Press RIGHT → change = 1
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;

		mode.onSetting(engine, 0);

		// nowMapID = 0 + 1 = 1 > listFields.size() (= 0) → nowMapID = 0
		assertEquals(0, readInt(mode, "nowMapID"),
				"nowMapID should clamp to 0 when > listFields.size()");
	}

	// ====================================================================
	// onSetting: menuCursor 6-7 (nowMapSetID change, lines 170-172)
	// ====================================================================

	@Test
	void onSettingCursor6MapSetIDDecrement() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setInt(mode, "menuCursor", 6);
		setInt(mode, "menuTime", 10);
		setInt(mode, "nowMapSetID", 0);

		// Press LEFT → change = -1 → nowMapSetID = -1 → wraps to 99
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;

		mode.onSetting(engine, 0);
		assertEquals(99, readInt(mode, "nowMapSetID"),
				"nowMapSetID should wrap to 99 when negative");
	}

	@Test
	void onSettingCursor7MapSetIDIncrement() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setInt(mode, "menuCursor", 7);
		setInt(mode, "menuTime", 10);
		setInt(mode, "nowMapSetID", 99);

		// Press RIGHT → change = 1 → nowMapSetID = 100 → wraps to 0
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;

		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "nowMapSetID"),
				"nowMapSetID should wrap to 0 when > 99");
	}

	// ====================================================================
	// renderSetting: "NO MAPS" when listFields is empty (line 248)
	// ====================================================================

	@Test
	void renderSettingNoMaps() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Ensure listFields is empty
		@SuppressWarnings("unchecked")
		java.util.LinkedList<Object> list = (java.util.LinkedList<Object>) readFieldObj(mode, "listFields");
		list.clear();

		setInt(mode, "menuCursor", 3);
		mode.renderSetting(engine, 0);
		// Should not throw
	}

	// ====================================================================
	// renderSetting: cursor indicators for menuCursor 3-5 (line 251)
	// ====================================================================

	@Test
	void renderSettingCursor3Indicator() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Need non-empty list for "nowMapID/size" display
		@SuppressWarnings("unchecked")
		java.util.LinkedList<Object> list = (java.util.LinkedList<Object>) readFieldObj(mode, "listFields");
		list.add(new Field(engine.field));

		setInt(mode, "menuCursor", 3);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingCursor4Indicator() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		@SuppressWarnings("unchecked")
		java.util.LinkedList<Object> list = (java.util.LinkedList<Object>) readFieldObj(mode, "listFields");
		list.add(new Field(engine.field));

		setInt(mode, "menuCursor", 4);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingCursor5Indicator() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		@SuppressWarnings("unchecked")
		java.util.LinkedList<Object> list = (java.util.LinkedList<Object>) readFieldObj(mode, "listFields");
		list.add(new Field(engine.field));

		setInt(mode, "menuCursor", 5);
		mode.renderSetting(engine, 0);
	}

	// ====================================================================
	// renderSetting: cursor indicators for menuCursor 6-7 (line 260)
	// ====================================================================

	@Test
	void renderSettingCursor6Indicator() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setInt(mode, "menuCursor", 6);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingCursor7Indicator() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setInt(mode, "menuCursor", 7);
		mode.renderSetting(engine, 0);
	}

	// ---- helpers ----

	private static int readInt(Object obj, String name) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static Object readFieldObj(Object obj, String name) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws java.lang.NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (java.lang.NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new java.lang.NoSuchFieldException(name);
	}
}
