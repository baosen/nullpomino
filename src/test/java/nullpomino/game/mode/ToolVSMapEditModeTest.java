package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link ToolVSMapEditMode}: getName(),
 * modeInit's defaults, and the playerInit/loadAllMaps wiring against a
 * receiver whose loadProperties returns null (the missing-file
 * fallback). The render and edit screens are out of scope here — they
 * need an SDL context and live keyboard input.
 */
class ToolVSMapEditModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("TOOL-VS MAP EDIT", new ToolVSMapEditMode().getName());
	}

	@Test
	void modeInitInitialisesEmptyMapListAndZeroSetCursor() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.init();

		mode.modeInit(manager);

		assertNotNull(readField(mode, "listFields"),
				"modeInit must construct a fresh map list");
		assertEquals(0, ((java.util.List<?>) readField(mode, "listFields")).size());
		assertEquals(0, readInt(mode, "nowMapSetID"));
		assertEquals(0, readInt(mode, "nowMapID"));
	}

	@Test
	void playerInitClearsMenuStateAndPaintsFrameGrayAndLoadsEmptyMapListForMissingSet() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.init();
		mode.modeInit(manager);
		// Spoil menuTime and menuCursor so the playerInit reset is observable.
		setInt(mode, "menuTime", 99);
		setInt(mode, "menuCursor", 7);
		// Aim loadAllMaps at a setID that has no bundled .map file so the
		// missing-file fallback runs (propMap = new CustomProperties,
		// maxMapNumber defaults to 0, list stays empty).
		setInt(mode, "nowMapSetID", 9999);

		manager.engine[0].init();
		mode.playerInit(manager.engine[0], 0);

		assertEquals(0, readInt(mode, "menuTime"));
		assertEquals(0, readInt(mode, "menuCursor"));
		assertEquals(GameEngine.FRAME_COLOR_GRAY, manager.engine[0].framecolor);
		assertNotNull(manager.engine[0].field,
				"createFieldIfNeeded must allocate a Field on the engine");

		java.util.List<?> listFields = (java.util.List<?>) readField(mode, "listFields");
		assertTrue(listFields.isEmpty(),
				"missing per-set .map file must leave the field list empty");
	}

	@Test
	void playerInitWithBundledSetZeroLoadsEveryMapEntry() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.init();
		mode.modeInit(manager);
		// nowMapSetID stays at 0 so loadAllMaps reads config/map/vsbattle/0.map
		// from the runfile path; that file declares map.maxMapNumber=20.

		manager.engine[0].init();
		mode.playerInit(manager.engine[0], 0);

		java.util.List<?> listFields = (java.util.List<?>) readField(mode, "listFields");
		assertEquals(20, listFields.size(),
				"bundled vsbattle/0.map declares map.maxMapNumber=20");
	}

	@Test
	void modeReportsSinglePlayerAndTetrominoStyleByInheritance() {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();

		assertEquals(1, mode.getPlayers());
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, mode.getGameStyle());
	}

	private static int readInt(ToolVSMapEditMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static Object readField(ToolVSMapEditMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(ToolVSMapEditMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
