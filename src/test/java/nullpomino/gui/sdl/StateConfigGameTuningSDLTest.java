package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link StateConfigGameTuningSDL}: the
 * cursor / page bounds set by the constructor, the documented defaults
 * applied by loadConfig (every override sentinel at -1), the
 * per-player saveConfig key shape ('{player}.tuning.{field}'), and
 * rebuildList's label rendering for the rotate / tri-state / AUTO
 * decoders.
 */
class StateConfigGameTuningSDLTest {

	@Test
	void constructorPinsPageHeightMaxCursorPlayerAndCursorZero() throws Exception {
		StateConfigGameTuningSDL state = new StateConfigGameTuningSDL();

		assertEquals(10, readInt(state, DummyMenuScrollStateSDL.class, "pageHeight"));
		assertEquals(9, readInt(state, DummyMenuChooseStateSDL.class, "maxCursor"),
				"ten entries (0..9) -> maxCursor pinned to 9");
		assertEquals(0, state.player);
		assertEquals(0, readInt(state, DummyMenuChooseStateSDL.class, "cursor"));
	}

	@Test
	void uiTextListsTheTenMenuRowKeysInOrder() throws Exception {
		Field f = StateConfigGameTuningSDL.class.getDeclaredField("UI_TEXT");
		f.setAccessible(true);
		assertArrayEquals(
				new String[] {
						"GameTuning_RotateButtonDefaultRight",
						"GameTuning_Skin",
						"GameTuning_MinDAS",
						"GameTuning_MaxDAS",
						"GameTuning_DasDelay",
						"GameTuning_ReverseUpDown",
						"GameTuning_MoveDiagonal",
						"GameTuning_BlockOutlineType",
						"GameTuning_BlockShowOutlineOnly",
						"GameTuning_Preview",
				},
				(String[]) f.get(null));
	}

	@Test
	void outlineTypeNamesListsTheFiveBlockOutlineLabels() throws Exception {
		Field f = StateConfigGameTuningSDL.class.getDeclaredField("OUTLINE_TYPE_NAMES");
		f.setAccessible(true);
		assertArrayEquals(
				new String[] {"AUTO", "NONE", "NORMAL", "CONNECT", "SAMECOLOR"},
				(String[]) f.get(null));
	}

	@Test
	void loadConfigAppliesAutoSentinelDefaultsForMissingKeys() throws Exception {
		StateConfigGameTuningSDL state = new StateConfigGameTuningSDL();

		invokeLoadConfig(state, new CustomProperties());

		assertEquals(-1, readInt(state, "owRotateButtonDefaultRight"));
		assertEquals(-1, readInt(state, "owSkin"));
		assertEquals(-1, readInt(state, "owMinDAS"));
		assertEquals(-1, readInt(state, "owMaxDAS"));
		assertEquals(-1, readInt(state, "owDasDelay"));
		assertFalse(readBoolean(state, "owReverseUpDown"));
		assertEquals(-1, readInt(state, "owMoveDiagonal"));
		assertEquals(-1, readInt(state, "owBlockOutlineType"));
		assertEquals(-1, readInt(state, "owBlockShowOutlineOnly"));
	}

	@Test
	void saveConfigKeyShapeUsesPlayerPrefixWithoutSeparatorBeforeTuning() throws Exception {
		StateConfigGameTuningSDL state = new StateConfigGameTuningSDL();
		state.player = 0;
		setInt(state, "owSkin", 3);

		CustomProperties prop = new CustomProperties();
		invokeSaveConfig(state, prop);

		// player + ".tuning.owSkin" = "0.tuning.owSkin"
		assertEquals(3, prop.getProperty("0.tuning.owSkin", -99));
		// Negative: player 1 namespace must not have any of these keys.
		assertEquals(-99, prop.getProperty("1.tuning.owSkin", -99));
	}

	@Test
	void saveConfigAndLoadConfigRoundTripPreservesEveryField() throws Exception {
		StateConfigGameTuningSDL source = new StateConfigGameTuningSDL();
		source.player = 0;
		setInt(source, "owRotateButtonDefaultRight", 1);
		setInt(source, "owSkin", 5);
		setInt(source, "owMinDAS", 8);
		setInt(source, "owMaxDAS", 16);
		setInt(source, "owDasDelay", 3);
		setBoolean(source, "owReverseUpDown", true);
		setInt(source, "owMoveDiagonal", 1);
		setInt(source, "owBlockOutlineType", 2);
		setInt(source, "owBlockShowOutlineOnly", 1);

		CustomProperties prop = new CustomProperties();
		invokeSaveConfig(source, prop);

		StateConfigGameTuningSDL dest = new StateConfigGameTuningSDL();
		dest.player = 0;
		invokeLoadConfig(dest, prop);

		assertEquals(1, readInt(dest, "owRotateButtonDefaultRight"));
		assertEquals(5, readInt(dest, "owSkin"));
		assertEquals(8, readInt(dest, "owMinDAS"));
		assertEquals(16, readInt(dest, "owMaxDAS"));
		assertEquals(3, readInt(dest, "owDasDelay"));
		assertTrue(readBoolean(dest, "owReverseUpDown"));
		assertEquals(1, readInt(dest, "owMoveDiagonal"));
		assertEquals(2, readInt(dest, "owBlockOutlineType"));
		assertEquals(1, readInt(dest, "owBlockShowOutlineOnly"));
	}

	@Test
	void perPlayerKeyspacesAreIsolatedSoTwoPlayersDontStompOnEachOther() throws Exception {
		// Two states sharing the same CustomProperties at different
		// player ids must not see each other's writes.
		StateConfigGameTuningSDL p0 = new StateConfigGameTuningSDL();
		p0.player = 0;
		setInt(p0, "owSkin", 3);

		StateConfigGameTuningSDL p1 = new StateConfigGameTuningSDL();
		p1.player = 1;
		setInt(p1, "owSkin", 7);

		CustomProperties prop = new CustomProperties();
		invokeSaveConfig(p0, prop);
		invokeSaveConfig(p1, prop);

		// Re-load each into a fresh state and confirm isolation.
		StateConfigGameTuningSDL d0 = new StateConfigGameTuningSDL();
		d0.player = 0;
		invokeLoadConfig(d0, prop);
		assertEquals(3, readInt(d0, "owSkin"));

		StateConfigGameTuningSDL d1 = new StateConfigGameTuningSDL();
		d1.player = 1;
		invokeLoadConfig(d1, prop);
		assertEquals(7, readInt(d1, "owSkin"));
	}

	@Test
	void rebuildListRendersAutoLabelsForEverySentinelOverride() throws Exception {
		StateConfigGameTuningSDL state = new StateConfigGameTuningSDL();
		// All sentinel defaults from a fresh load.
		invokeLoadConfig(state, new CustomProperties());

		invokeRebuildList(state);
		String[] list = (String[]) readField(state, "list");

		assertEquals("A BUTTON ROTATE:AUTO", list[0]);
		assertEquals("BLOCK SKIN:AUTO", list[1]);
		assertEquals("MIN DAS:AUTO", list[2]);
		assertEquals("MAX DAS:AUTO", list[3]);
		assertEquals("DAS DELAY:AUTO", list[4]);
		// owReverseUpDown=false -> 'e' (X glyph).
		assertEquals("REVERSE UP/DOWN:e", list[5]);
		assertEquals("DIAGONAL MOVE:AUTO", list[6]);
		// OUTLINE_TYPE_NAMES[-1+1] = OUTLINE_TYPE_NAMES[0] = AUTO.
		assertEquals("OUTLINE TYPE:AUTO", list[7]);
		assertEquals("SHOW OUTLINE ONLY:AUTO", list[8]);
		assertEquals("[PREVIEW]", list[9]);
	}

	@Test
	void rebuildListRendersFixedLabelsForOverriddenValues() throws Exception {
		StateConfigGameTuningSDL state = new StateConfigGameTuningSDL();
		setInt(state, "owRotateButtonDefaultRight", 1); // RIGHT
		setInt(state, "owSkin", 4);
		setInt(state, "owMinDAS", 8);
		setInt(state, "owMaxDAS", 18);
		setInt(state, "owDasDelay", 2);
		setBoolean(state, "owReverseUpDown", true);
		setInt(state, "owMoveDiagonal", 1); // ON glyph 'c'
		setInt(state, "owBlockOutlineType", 3); // SAMECOLOR (index 3+1=4)
		setInt(state, "owBlockShowOutlineOnly", 0); // 'e' glyph

		invokeRebuildList(state);
		String[] list = (String[]) readField(state, "list");

		assertEquals("A BUTTON ROTATE:RIGHT", list[0]);
		assertEquals("BLOCK SKIN:4", list[1]);
		assertEquals("MIN DAS:8", list[2]);
		assertEquals("MAX DAS:18", list[3]);
		assertEquals("DAS DELAY:2", list[4]);
		// owReverseUpDown=true -> 'c' (O glyph).
		assertEquals("REVERSE UP/DOWN:c", list[5]);
		assertEquals("DIAGONAL MOVE:c", list[6]);
		assertEquals("OUTLINE TYPE:SAMECOLOR", list[7]);
		assertEquals("SHOW OUTLINE ONLY:e", list[8]);
		assertEquals("[PREVIEW]", list[9]);
	}

	@Test
	void rebuildListRendersLeftRotationLabel() throws Exception {
		StateConfigGameTuningSDL state = new StateConfigGameTuningSDL();
		setInt(state, "owRotateButtonDefaultRight", 0); // LEFT
		invokeRebuildList(state);
		String[] list = (String[]) readField(state, "list");

		assertEquals("A BUTTON ROTATE:LEFT", list[0]);
	}

	private static int readInt(Object instance, String name) throws Exception {
		return readInt(instance, instance.getClass(), name);
	}

	private static int readInt(Object instance, Class<?> declaringClass, String name) throws Exception {
		Field f = declaringClass.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static boolean readBoolean(Object instance, String name) throws Exception {
		Field f = instance.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(instance);
	}

	private static Object readField(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
	}

	private static void setInt(Object instance, String name, int value) throws Exception {
		Field f = instance.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static void setBoolean(Object instance, String name, boolean value) throws Exception {
		Field f = instance.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(instance, value);
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

	private static void invokeLoadConfig(StateConfigGameTuningSDL state, CustomProperties prop) throws Exception {
		Method m = StateConfigGameTuningSDL.class.getDeclaredMethod("loadConfig", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(state, prop);
	}

	private static void invokeSaveConfig(StateConfigGameTuningSDL state, CustomProperties prop) throws Exception {
		Method m = StateConfigGameTuningSDL.class.getDeclaredMethod("saveConfig", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(state, prop);
	}

	private static void invokeRebuildList(StateConfigGameTuningSDL state) throws Exception {
		Method m = StateConfigGameTuningSDL.class.getDeclaredMethod("rebuildList");
		m.setAccessible(true);
		m.invoke(state);
	}
}
