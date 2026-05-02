package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

/**
 * Pins the settings logic in {@link StateConfigGameTuningSDL}:
 * config load/save round-trips, {@code rebuildList()} label generation,
 * {@code onChange()} value cycling, and the private static helpers
 * {@code rotateLabel()} and {@code triStateLabel()}.
 *
 * <p>These tests construct the state object but never call
 * {@code render()} or {@code update()} — both require an SDL context.
 */
class StateConfigGameTuningSDLLogicTest {

	private StateConfigGameTuningSDL state;
	private CustomProperties originalPropGlobal;
	private CustomProperties originalPropConfig;

	@BeforeEach
	void setUp() {
		originalPropGlobal = NullpoMinoSDL.propGlobal;
		originalPropConfig = NullpoMinoSDL.propConfig;
		NullpoMinoSDL.propGlobal = new CustomProperties();
		NullpoMinoSDL.propConfig = new CustomProperties();
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
		state = new StateConfigGameTuningSDL();
	}

	@AfterEach
	void tearDown() {
		NullpoMinoSDL.propGlobal = originalPropGlobal;
		NullpoMinoSDL.propConfig = originalPropConfig;
	}

	/* ---------- Constructor defaults ---------- */

	@Test
	void constructorSetsDefaultValues() {
		assertEquals(10, state.pageHeight);
		assertEquals(9, state.maxCursor);
		assertEquals(0, state.player);
		assertEquals(0, state.cursor);
	}

	/* ---------- loadConfig / saveConfig round-trip ---------- */

	@Test
	void loadConfigReadsPlayerPrefixedProperties() {
		CustomProperties prop = new CustomProperties();
		prop.setProperty("1.tuning.owRotateButtonDefaultRight", "1");
		prop.setProperty("1.tuning.owSkin", "3");
		prop.setProperty("1.tuning.owMinDAS", "5");
		prop.setProperty("1.tuning.owMaxDAS", "12");
		prop.setProperty("1.tuning.owDasDelay", "2");
		prop.setProperty("1.tuning.owReverseUpDown", "true");
		prop.setProperty("1.tuning.owMoveDiagonal", "1");
		prop.setProperty("1.tuning.owBlockOutlineType", "2");
		prop.setProperty("1.tuning.owBlockShowOutlineOnly", "0");

		state.player = 1;
		state.loadConfig(prop);

		assertEquals(1, state.owRotateButtonDefaultRight);
		assertEquals(3, state.owSkin);
		assertEquals(5, state.owMinDAS);
		assertEquals(12, state.owMaxDAS);
		assertEquals(2, state.owDasDelay);
		assertTrue(state.owReverseUpDown);
		assertEquals(1, state.owMoveDiagonal);
		assertEquals(2, state.owBlockOutlineType);
		assertEquals(0, state.owBlockShowOutlineOnly);
	}

	@Test
	void loadConfigUsesDefaultsForMissingKeys() {
		state.loadConfig(new CustomProperties());

		assertEquals(-1, state.owRotateButtonDefaultRight);
		assertEquals(-1, state.owSkin);
		assertEquals(-1, state.owMinDAS);
		assertEquals(-1, state.owMaxDAS);
		assertEquals(-1, state.owDasDelay);
		assertFalse(state.owReverseUpDown);
		assertEquals(-1, state.owMoveDiagonal);
		assertEquals(-1, state.owBlockOutlineType);
		assertEquals(-1, state.owBlockShowOutlineOnly);
	}

	@Test
	void saveConfigRoundTripsWithLoadConfig() {
		CustomProperties prop = new CustomProperties();
		state.player = 0;
		state.owRotateButtonDefaultRight = 0;
		state.owSkin = 2;
		state.owMinDAS = 3;
		state.owMaxDAS = 8;
		state.owDasDelay = 1;
		state.owReverseUpDown = true;
		state.owMoveDiagonal = 0;
		state.owBlockOutlineType = 1;
		state.owBlockShowOutlineOnly = 1;

		state.saveConfig(prop);

		StateConfigGameTuningSDL loaded = new StateConfigGameTuningSDL();
		loaded.player = 0;
		loaded.loadConfig(prop);
		assertEquals(0, loaded.owRotateButtonDefaultRight);
		assertEquals(2, loaded.owSkin);
		assertEquals(3, loaded.owMinDAS);
		assertEquals(8, loaded.owMaxDAS);
		assertEquals(1, loaded.owDasDelay);
		assertTrue(loaded.owReverseUpDown);
		assertEquals(0, loaded.owMoveDiagonal);
		assertEquals(1, loaded.owBlockOutlineType);
		assertEquals(1, loaded.owBlockShowOutlineOnly);
	}

	/* ---------- rebuildList ---------- */

	@Test
	void rebuildListGeneratesExpectedLabels() {
		state.owRotateButtonDefaultRight = -1;
		state.owSkin = -1;
		state.owMinDAS = -1;
		state.owMaxDAS = -1;
		state.owDasDelay = -1;
		state.owReverseUpDown = false;
		state.owMoveDiagonal = -1;
		state.owBlockOutlineType = -1;
		state.owBlockShowOutlineOnly = -1;

		state.rebuildList();

		assertEquals(10, state.list.length);
		assertEquals("A BUTTON ROTATE:AUTO", state.list[0]);
		assertEquals("BLOCK SKIN:AUTO", state.list[1]);
		assertEquals("MIN DAS:AUTO", state.list[2]);
		assertEquals("MAX DAS:AUTO", state.list[3]);
		assertEquals("DAS DELAY:AUTO", state.list[4]);
		assertEquals("REVERSE UP/DOWN:e", state.list[5]);
		assertEquals("DIAGONAL MOVE:AUTO", state.list[6]);
		assertEquals("OUTLINE TYPE:AUTO", state.list[7]);
		assertEquals("SHOW OUTLINE ONLY:AUTO", state.list[8]);
		assertEquals("[PREVIEW]", state.list[9]);
	}

	@Test
	void rebuildListShowsNumericValuesWhenSet() {
		state.owRotateButtonDefaultRight = 0;
		state.owSkin = 5;
		state.owMinDAS = 3;
		state.owMaxDAS = 12;
		state.owDasDelay = 2;
		state.owReverseUpDown = true;
		state.owMoveDiagonal = 1;
		state.owBlockOutlineType = 2;
		state.owBlockShowOutlineOnly = 0;

		state.rebuildList();

		assertEquals("A BUTTON ROTATE:LEFT", state.list[0]);
		assertEquals("BLOCK SKIN:5", state.list[1]);
		assertEquals("MIN DAS:3", state.list[2]);
		assertEquals("MAX DAS:12", state.list[3]);
		assertEquals("DAS DELAY:2", state.list[4]);
		assertEquals("REVERSE UP/DOWN:c", state.list[5]);
		assertEquals("DIAGONAL MOVE:c", state.list[6]);
		assertEquals("OUTLINE TYPE:CONNECT", state.list[7]);
		assertEquals("SHOW OUTLINE ONLY:e", state.list[8]);
	}

	/* ---------- onChange value cycling ---------- */

	@Test
	void onChangeCursor0CyclesRotateButton() {
		state.cursor = 0;
		state.owRotateButtonDefaultRight = -1;

		state.onChange(1);
		assertEquals(0, state.owRotateButtonDefaultRight);

		state.onChange(1);
		assertEquals(1, state.owRotateButtonDefaultRight);

		state.onChange(1);
		assertEquals(-1, state.owRotateButtonDefaultRight);
	}

	@Test
	void onChangeCursor0CyclesBackward() {
		state.cursor = 0;
		state.owRotateButtonDefaultRight = -1;

		state.onChange(-1);
		assertEquals(1, state.owRotateButtonDefaultRight);
	}

	@Test
	void onChangeCursor2CyclesMinDas() {
		state.cursor = 2;
		state.owMinDAS = -1;

		state.onChange(1);
		assertEquals(0, state.owMinDAS);

		state.onChange(-1);
		assertEquals(-1, state.owMinDAS);
	}

	@Test
	void onChangeCursor2MinDasWrapsAtUpperBound() {
		state.cursor = 2;
		state.owMinDAS = 99;

		state.onChange(1);
		assertEquals(-1, state.owMinDAS);
	}

	@Test
	void onChangeCursor2MinDasWrapsAtLowerBound() {
		state.cursor = 2;
		state.owMinDAS = -1;

		state.onChange(-1);
		assertEquals(99, state.owMinDAS);
	}

	@Test
	void onChangeCursor3CyclesMaxDas() {
		state.cursor = 3;
		state.owMaxDAS = -1;

		state.onChange(1);
		assertEquals(0, state.owMaxDAS);
	}

	@Test
	void onChangeCursor4CyclesDasDelay() {
		state.cursor = 4;
		state.owDasDelay = -1;

		state.onChange(1);
		assertEquals(0, state.owDasDelay);
	}

	@Test
	void onChangeCursor5TogglesReverseUpDown() {
		state.cursor = 5;
		state.owReverseUpDown = false;

		state.onChange(1);
		assertTrue(state.owReverseUpDown);

		// Toggle is independent of change direction
		state.onChange(-1);
		assertFalse(state.owReverseUpDown);
	}

	@Test
	void onChangeCursor6CyclesMoveDiagonal() {
		state.cursor = 6;
		state.owMoveDiagonal = -1;

		state.onChange(1);
		assertEquals(0, state.owMoveDiagonal);

		state.onChange(1);
		assertEquals(1, state.owMoveDiagonal);

		state.onChange(1);
		assertEquals(-1, state.owMoveDiagonal);
	}

	@Test
	void onChangeCursor7CyclesOutlineType() {
		state.cursor = 7;
		state.owBlockOutlineType = -1;

		state.onChange(1);
		assertEquals(0, state.owBlockOutlineType);

		state.onChange(1);
		assertEquals(1, state.owBlockOutlineType);

		// 3 is max, then wraps to -1
		state.owBlockOutlineType = 3;
		state.onChange(1);
		assertEquals(-1, state.owBlockOutlineType);
	}

	@Test
	void onChangeCursor8CyclesShowOutlineOnly() {
		state.cursor = 8;
		state.owBlockShowOutlineOnly = -1;

		state.onChange(1);
		assertEquals(0, state.owBlockShowOutlineOnly);

		state.onChange(1);
		assertEquals(1, state.owBlockShowOutlineOnly);

		state.onChange(1);
		assertEquals(-1, state.owBlockShowOutlineOnly);
	}

	/* ---------- onDecide / onCancel / onPushButtonD ---------- */

	@Test
	void onCancelReturnsTrue() {
		assertTrue(state.onCancel());
	}

	@Test
	void onPushButtonDReturnsTrue() {
		assertTrue(state.onPushButtonD());
	}

	/* ---------- rotateLabel / triStateLabel ---------- */

	@Test
	void rotateLabelReturnsExpectedValues() throws Exception {
		assertEquals("LEFT", invokeRotateLabel(0));
		assertEquals("RIGHT", invokeRotateLabel(1));
		assertEquals("AUTO", invokeRotateLabel(-1));
		assertEquals("AUTO", invokeRotateLabel(42));
	}

	@Test
	void triStateLabelReturnsExpectedValues() throws Exception {
		assertEquals("e", invokeTriStateLabel(0));
		assertEquals("c", invokeTriStateLabel(1));
		assertEquals("AUTO", invokeTriStateLabel(-1));
		assertEquals("AUTO", invokeTriStateLabel(42));
	}

	private static String invokeRotateLabel(int v) throws Exception {
		Method m = StateConfigGameTuningSDL.class.getDeclaredMethod("rotateLabel", int.class);
		m.setAccessible(true);
		return (String) m.invoke(null, v);
	}

	private static String invokeTriStateLabel(int v) throws Exception {
		Method m = StateConfigGameTuningSDL.class.getDeclaredMethod("triStateLabel", int.class);
		m.setAccessible(true);
		return (String) m.invoke(null, v);
	}
}
