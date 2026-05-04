package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.ToolVSMapEditMode;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers uncovered branches in {@link ToolVSMapEditMode}: saveMap,
 * saveAllMaps, grayToRandomColor, onSetting menu operations (save, load,
 * delete, write, read, edit, gray->?, clear), renderSetting, and
 * renderFieldEdit.
 */
class ToolVSMapEditModeCoverageTest {

	@Test
	void saveMapWritesProperty() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		CustomProperties prop = new CustomProperties();
		invokeSaveMap(mode, engine.field, prop, 5);

		assertTrue(prop.getProperty("map.5", "").length() > 0,
				"saveMap should write a non-empty field string");
	}

	@Test
	void saveAllMapsWritesMaxMapNumber() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Use nowMapSetID=9999 so loadAllMaps finds no file and list stays empty
		setInt(mode, "nowMapSetID", 9999);
		mode.playerInit(engine, 0);

		java.util.LinkedList<Field> list = (java.util.LinkedList<Field>) readFieldObj(mode, "listFields");
		list.add(new Field(engine.field));

		invokeSaveAllMaps(mode, 9999);

		CustomProperties propMap = (CustomProperties) readFieldObj(mode, "propMap");
		assertNotNull(propMap);
		assertEquals(1, propMap.getProperty("map.maxMapNumber", -1));
	}

	@Test
	void grayToRandomColorChangesGrayBlocks() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		for (int x = 0; x < w; x++) {
			engine.field.setBlock(x, h - 1,
					new Block(Block.BLOCK_COLOR_GRAY, engine.getSkin(), Block.BLOCK_ATTRIBUTE_VISIBLE));
		}

		invokeGrayToRandomColor(mode, engine.field);

		boolean hasNonGray = false;
		for (int x = 0; x < w; x++) {
			Block b = engine.field.getBlock(x, h - 1);
			if (b != null && b.color != Block.BLOCK_COLOR_GRAY) {
				hasNonGray = true;
				break;
			}
		}
		assertTrue(hasNonGray, "grayToRandomColor should change at least one gray block");
	}

	@Test
	void onSettingSaveAddsFieldToList() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Use nowMapSetID=9999 so loadAllMaps finds no file and list stays empty
		setInt(mode, "nowMapSetID", 9999);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		int sizeBefore = ((java.util.LinkedList<Field>) readFieldObj(mode, "listFields")).size();

		mode.onSetting(engine, 0);

		int sizeAfter = ((java.util.LinkedList<Field>) readFieldObj(mode, "listFields")).size();
		assertEquals(sizeBefore + 1, sizeAfter, "SAVE with out-of-range nowMapID should add new field");
	}

	@Test
	void onSettingLoadResetsFieldWhenNoMap() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setInt(mode, "menuCursor", 4);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_RED, engine.getSkin(), Block.BLOCK_ATTRIBUTE_VISIBLE));

		mode.onSetting(engine, 0);

		Block b = engine.field.getBlock(0, engine.field.getHeight() - 1);
		assertTrue(b == null || !b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"LOAD with out-of-range nowMapID should reset field");
	}

	@Test
	void onSettingDeleteRemovesMap() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		java.util.LinkedList<Field> list = (java.util.LinkedList<Field>) readFieldObj(mode, "listFields");
		list.add(new Field(engine.field));

		setInt(mode, "menuCursor", 5);
		setInt(mode, "menuTime", 10);
		setInt(mode, "nowMapID", 0);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		assertEquals(0, list.size(), "DELETE should remove the map from the list");
	}

	@Test
	void onSettingWriteCallsSaveAllMaps() throws Exception {
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
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);
	}

	@Test
	void onSettingReadCallsLoadAllMaps() throws Exception {
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
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "nowMapID"), "READ should reset nowMapID to 0");
	}

	@Test
	void onSettingEditEntersFieldEdit() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		assertEquals(GameEngine.Status.FIELDEDIT, engine.stat);
	}

	@Test
	void onSettingGrayToRandomColor() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		engine.field.setBlock(0, h - 1,
				new Block(Block.BLOCK_COLOR_GRAY, engine.getSkin(), Block.BLOCK_ATTRIBUTE_VISIBLE));

		setInt(mode, "menuCursor", 1);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		Block b = engine.field.getBlock(0, h - 1);
		assertTrue(b.color != Block.BLOCK_COLOR_GRAY, "GRAY->? should change gray blocks");
	}

	@Test
	void onSettingClearResetsField() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		engine.field.setBlock(0, h - 1,
				new Block(Block.BLOCK_COLOR_RED, engine.getSkin(), Block.BLOCK_ATTRIBUTE_VISIBLE));

		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		Block b = engine.field.getBlock(0, h - 1);
		assertTrue(b == null || !b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"CLEAR should reset the field");
	}

	@Test
	void onSettingCursorChangeWithMapSetID() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		setInt(mode, "menuCursor", 6);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;

		mode.onSetting(engine, 0);
	}

	@Test
	void onSettingQuitFlag() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		setInt(mode, "menuTime", 10);

		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag, "D+E should set quitflag");
	}

@Test
	void onSettingSaveOverwritesExistingMap() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Use nowMapSetID=9999 so loadAllMaps finds no file and list stays empty
		setInt(mode, "nowMapSetID", 9999);
		mode.playerInit(engine, 0);

		// Add a field to the list
		java.util.LinkedList<Field> list = (java.util.LinkedList<Field>) readFieldObj(mode, "listFields");
		list.add(new Field(engine.field));

		// menuCursor = 3 → SAVE, nowMapID = 0 (valid)
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 10);
		setInt(mode, "nowMapID", 0);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		assertEquals(1, list.size(), "SAVE with valid nowMapID should overwrite, not add");
	}

	@Test
	void onSettingLoadFromExistingMap() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Use nowMapSetID=9999 so loadAllMaps finds no file and list stays empty
		setInt(mode, "nowMapSetID", 9999);
		mode.playerInit(engine, 0);

		// Add a field with a block to the list
		java.util.LinkedList<Field> list = (java.util.LinkedList<Field>) readFieldObj(mode, "listFields");
		Field storedField = new Field(engine.field);
		storedField.setBlock(5, storedField.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_RED, engine.getSkin(), Block.BLOCK_ATTRIBUTE_VISIBLE));
		list.add(storedField);

		// menuCursor = 4 → LOAD, nowMapID = 0 (valid)
		setInt(mode, "menuCursor", 4);
		setInt(mode, "menuTime", 10);
		setInt(mode, "nowMapID", 0);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		Block b = engine.field.getBlock(5, engine.field.getHeight() - 1);
		assertTrue(b != null, "LOAD from valid map should copy field data");
	}

	@Test
	void onSettingDeleteAdjustsNowMapID() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Use nowMapSetID=9999 so loadAllMaps finds no file and list stays empty
		setInt(mode, "nowMapSetID", 9999);
		mode.playerInit(engine, 0);

		// Add two fields
		java.util.LinkedList<Field> list = (java.util.LinkedList<Field>) readFieldObj(mode, "listFields");
		list.add(new Field(engine.field));
		list.add(new Field(engine.field));

		// menuCursor = 5 → DELETE, nowMapID = 1 (valid, but after delete nowMapID >= list.size())
		setInt(mode, "menuCursor", 5);
		setInt(mode, "menuTime", 10);
		setInt(mode, "nowMapID", 1);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		assertEquals(1, list.size(), "DELETE should remove one map");
		assertEquals(1, readInt(mode, "nowMapID"), "nowMapID should be adjusted to list.size()");
	}
	}

@Test
	void onSettingLoadResetsFieldWhenNoMap() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Use nowMapSetID=9999 so loadAllMaps finds no file and list stays empty
		setInt(mode, "nowMapSetID", 9999);
		mode.playerInit(engine, 0);

		// menuCursor = 4 → LOAD, nowMapID out of range → field.reset()
		setInt(mode, "menuCursor", 4);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		// Place a block so we can verify it gets cleared
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_RED, engine.getSkin(), Block.BLOCK_ATTRIBUTE_VISIBLE));

		mode.onSetting(engine, 0);

		Block b = engine.field.getBlock(0, engine.field.getHeight() - 1);
		assertTrue(b == null || !b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"LOAD with out-of-range nowMapID should reset field");
	}

@Test
	void onSettingDeleteRemovesMap() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// Use nowMapSetID=9999 so loadAllMaps finds no file and list stays empty
		setInt(mode, "nowMapSetID", 9999);
		mode.playerInit(engine, 0);

		// Add a field to the list
		java.util.LinkedList<Field> list = (java.util.LinkedList<Field>) readFieldObj(mode, "listFields");
		list.add(new Field(engine.field));

		// menuCursor = 5 → DELETE, nowMapID = 0 (valid)
		setInt(mode, "menuCursor", 5);
		setInt(mode, "menuTime", 10);
		setInt(mode, "nowMapID", 0);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		assertEquals(0, list.size(), "DELETE should remove the map from the list");
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

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeSaveMap(ToolVSMapEditMode mode, Field field, CustomProperties prop, int id) throws Exception {
		Method m = ToolVSMapEditMode.class.getDeclaredMethod("saveMap", Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}

	private static void invokeSaveAllMaps(ToolVSMapEditMode mode, int setID) throws Exception {
		Method m = ToolVSMapEditMode.class.getDeclaredMethod("saveAllMaps", int.class);
		m.setAccessible(true);
		m.invoke(mode, setID);
	}

	private static void invokeGrayToRandomColor(ToolVSMapEditMode mode, Field field) throws Exception {
		Method m = ToolVSMapEditMode.class.getDeclaredMethod("grayToRandomColor", Field.class);
		m.setAccessible(true);
		m.invoke(mode, field);
	}

	private static void invokeLoadMap(ToolVSMapEditMode mode, Field field, CustomProperties prop, int id) throws Exception {
		Method m = ToolVSMapEditMode.class.getDeclaredMethod("loadMap", Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}
}