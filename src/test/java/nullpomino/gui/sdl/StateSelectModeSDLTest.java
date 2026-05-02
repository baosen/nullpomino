package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link StateSelectModeSDL}: the constant
 * page height, the linear name -> id lookup used by prepareModeList to
 * restore last selection, and the on-disk property-key escape used by
 * getModeDesc (' ' -> '_', '(' -> 'l', ')' -> 'r').
 *
 * <p>The mode-description fallback chain reads from
 * {@link NullpoMinoSDL#propModeDesc} first, then
 * {@link NullpoMinoSDL#propDefaultModeDesc}, then surfaces the escaped
 * key itself. Tests snapshot and restore both statics.
 */
class StateSelectModeSDLTest {

	private CustomProperties originalPropModeDesc;
	private CustomProperties originalPropDefaultModeDesc;

	@BeforeEach
	void setUp() {
		originalPropModeDesc = NullpoMinoSDL.propModeDesc;
		originalPropDefaultModeDesc = NullpoMinoSDL.propDefaultModeDesc;
		NullpoMinoSDL.propModeDesc = new CustomProperties();
		NullpoMinoSDL.propDefaultModeDesc = new CustomProperties();
	}

	@AfterEach
	void tearDown() {
		NullpoMinoSDL.propModeDesc = originalPropModeDesc;
		NullpoMinoSDL.propDefaultModeDesc = originalPropDefaultModeDesc;
	}

	@Test
	void constructorPinsPageHeightToTwentyFour() throws Exception {
		StateSelectModeSDL state = new StateSelectModeSDL();

		assertEquals(24, StateSelectModeSDL.PAGE_HEIGHT);
		assertEquals(24, readInt(state, DummyMenuScrollStateSDL.class, "pageHeight"));
	}

	@Test
	void getIDbyNameReturnsMatchingIndexWhenListIsPopulated() throws Exception {
		StateSelectModeSDL state = new StateSelectModeSDL();
		setListField(state, new String[] {"MARATHON", "LINE RACE", "ULTRA"});

		assertEquals(0, invokeGetIDbyName(state, "MARATHON"));
		assertEquals(1, invokeGetIDbyName(state, "LINE RACE"));
		assertEquals(2, invokeGetIDbyName(state, "ULTRA"));
	}

	@Test
	void getIDbyNameReturnsMinusOneForUnknownName() throws Exception {
		StateSelectModeSDL state = new StateSelectModeSDL();
		setListField(state, new String[] {"MARATHON"});

		assertEquals(-1, invokeGetIDbyName(state, "NOT IN LIST"));
	}

	@Test
	void getIDbyNameReturnsMinusOneWhenNameIsNull() throws Exception {
		StateSelectModeSDL state = new StateSelectModeSDL();
		setListField(state, new String[] {"MARATHON"});

		assertEquals(-1, invokeGetIDbyName(state, null));
	}

	@Test
	void getIDbyNameReturnsMinusOneWhenListIsNull() throws Exception {
		StateSelectModeSDL state = new StateSelectModeSDL();
		// list stays null (the no-args constructor leaves it unallocated).

		assertEquals(-1, invokeGetIDbyName(state, "MARATHON"));
	}

	@Test
	void getModeDescPrefersPropModeDescOverDefault() throws Exception {
		// Both populated; foreground wins. Pin the override-vs-fallback order.
		NullpoMinoSDL.propModeDesc.setProperty("MARATHON", "Foreground");
		NullpoMinoSDL.propDefaultModeDesc.setProperty("MARATHON", "Default");

		StateSelectModeSDL state = new StateSelectModeSDL();
		assertEquals("Foreground", invokeGetModeDesc(state, "MARATHON"));
	}

	@Test
	void getModeDescFallsBackToDefaultWhenForegroundIsMissing() throws Exception {
		NullpoMinoSDL.propDefaultModeDesc.setProperty("LINE_RACE", "Default text");

		StateSelectModeSDL state = new StateSelectModeSDL();
		assertEquals("Default text", invokeGetModeDesc(state, "LINE RACE"),
				"name has spaces -> escaped to LINE_RACE for the property lookup");
	}

	@Test
	void getModeDescEscapesSpacesParensInPropertyKey() throws Exception {
		// ' ' -> '_', '(' -> 'l', ')' -> 'r' — the legacy properties-file
		// safe form. AVALANCHE 1P (RC2) -> AVALANCHE_1P_lRC2r etc.
		NullpoMinoSDL.propDefaultModeDesc.setProperty("AVALANCHE_1P_lRC2r", "1P RC2 desc");

		StateSelectModeSDL state = new StateSelectModeSDL();
		assertEquals("1P RC2 desc",
				invokeGetModeDesc(state, "AVALANCHE 1P (RC2)"));
	}

	@Test
	void getModeDescReturnsEscapedKeyWhenNeitherPropertyContainsIt() throws Exception {
		// Both files miss the key; getModeDesc returns the escaped lookup
		// key verbatim so a missing description shows up as the safe-form
		// name on screen rather than blank.
		StateSelectModeSDL state = new StateSelectModeSDL();
		assertEquals("MARATHON", invokeGetModeDesc(state, "MARATHON"));
		assertEquals("LINE_RACE", invokeGetModeDesc(state, "LINE RACE"));
		assertEquals("AVALANCHE_1P_lRC2r",
				invokeGetModeDesc(state, "AVALANCHE 1P (RC2)"));
	}

	@Test
	void getModeDescPrefersPropModeDescEvenWhenItHoldsEmptyString() throws Exception {
		// Edge case: foreground key is set to empty string. CustomProperties
		// returns the empty string, which is non-null, so fallback skips.
		NullpoMinoSDL.propModeDesc.setProperty("ULTRA", "");
		NullpoMinoSDL.propDefaultModeDesc.setProperty("ULTRA", "Default");

		StateSelectModeSDL state = new StateSelectModeSDL();
		assertEquals("", invokeGetModeDesc(state, "ULTRA"));
	}

	private static int readInt(Object instance, Class<?> declaringClass, String name) throws Exception {
		Field f = declaringClass.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static void setListField(StateSelectModeSDL state, String[] list) throws Exception {
		// list is a protected field on DummyMenuScrollStateSDL.
		Field f = DummyMenuScrollStateSDL.class.getDeclaredField("list");
		f.setAccessible(true);
		f.set(state, list);
	}

	private static int invokeGetIDbyName(StateSelectModeSDL state, String name) throws Exception {
		Method m = StateSelectModeSDL.class.getDeclaredMethod("getIDbyName", String.class);
		m.setAccessible(true);
		return (int) m.invoke(state, name);
	}

	private static String invokeGetModeDesc(StateSelectModeSDL state, String str) throws Exception {
		Method m = StateSelectModeSDL.class.getDeclaredMethod("getModeDesc", String.class);
		m.setAccessible(true);
		return (String) m.invoke(state, str);
	}
}
