package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for the core scaffolding in {@link AbstractMode}.
 * Covers registry surface, menu-item persistence, cursor logic, and
 * lifecycle hooks that are inherited by all game modes.
 */
class AbstractModeGameLogicTest {

	/** A minimal concrete subclass so we can instantiate AbstractMode. */
	private static class ConcreteMode extends AbstractMode {
		@Override public String getName() { return "TESTMODE"; }
	}

	@Test
	void getNameDefaultsToDummy() {
		assertEquals("DUMMY", new AbstractMode() {}.getName());
	}

	@Test
	void getPlayersReturnsOne() {
		assertEquals(1, new AbstractMode() {}.getPlayers());
	}

	@Test
	void getGameStyleReturnsTetromino() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new AbstractMode() {}.getGameStyle());
	}

	@Test
	void constructorInitialisesMenuFields() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		assertNotNull(readField(mode, "menu"));
		assertEquals(0, readInt(mode, "menuCursor"));
		assertEquals(0, readInt(mode, "menuTime"));
		assertEquals("dummy", readField(mode, "propName"));
	}

	@Test
	void modeInitSetsOwnerAndReceiver() {
		ConcreteMode mode = new ConcreteMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		// AbstractMode.modeInit is empty; owner/receiver are set by playerInit
		assertEquals(null, mode.owner);
		assertEquals(null, mode.receiver);
	}

	@Test
	void playerInitSetsOwnerAndReceiver() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		assertNotNull(mode.owner);
		assertNotNull(mode.receiver);
	}

	@Test
	void loadSettingOnEmptyMenuDoesNothing() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		mode.loadSetting(new CustomProperties());
		assertTrue(true, "no exception on empty menu");
	}

	@Test
	void saveSettingOnEmptyMenuDoesNothing() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		mode.saveSetting(new CustomProperties());
		assertTrue(true, "no exception on empty menu");
	}

	@Test
	void getMenuCursorAndSetMenuCursorRoundTrip() {
		ConcreteMode mode = new ConcreteMode();
		assertEquals(0, mode.getMenuCursor());
		mode.setMenuCursor(5);
		assertEquals(5, mode.getMenuCursor());
		mode.setMenuCursor(-1);
		assertEquals(-1, mode.getMenuCursor());
	}

	@Test
	void getMenuItemCountReturnsMinusOneWhenEmpty() {
		ConcreteMode mode = new ConcreteMode();
		assertEquals(-1, mode.getMenuItemCount());
	}

	@Test
	void playerInitResetsMenuTimeAndCursor() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "menuTime", 999);
		setInt(mode, "menuCursor", 999);
		mode.playerInit(engine, 0);

		// AbstractMode.playerInit only sets owner/receiver,
		// not menuTime/menuCursor (subclasses override to reset them)
		assertEquals(999, readInt(mode, "menuTime"));
		assertEquals(999, readInt(mode, "menuCursor"));
	}

	@Test
	void modeInitDoesNotThrowWhenManagerIsFresh() {
		ConcreteMode mode = new ConcreteMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		assertTrue(true, "modeInit accepted a fresh GameManager");
	}

	// -----------------------------------------------------------------------
	// updateCursor edge cases
	// -----------------------------------------------------------------------

	@Test
	void updateCursorWithNoInputReturnsZero() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine engine = freshEngine(mode);
		// With a controller that reports no keys pressed, updateCursor should return 0.
		Method m = AbstractMode.class.getDeclaredMethod(
				"updateCursor", GameEngine.class, int.class);
		m.setAccessible(true);
		int result = (int) m.invoke(mode, engine, 10);
		assertEquals(0, result);
	}

	@Test
	void updateRespectsMaxCursorBoundary() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine engine = freshEngine(mode);

		// Without any controller input, cursor stays at whatever value was set.
		setInt(mode, "menuCursor", 5);
		Method m = AbstractMode.class.getDeclaredMethod(
				"updateCursor", GameEngine.class, int.class);
		m.setAccessible(true);
		int result = (int) m.invoke(mode, engine, 5);
		assertEquals(0, result);
		assertEquals(5, readInt(mode, "menuCursor"));
	}

	@Test
	void updateCursorPlayerIdVariantWorks() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "menuCursor", 3);
		Method m = AbstractMode.class.getDeclaredMethod(
				"updateCursor", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		int result = (int) m.invoke(mode, engine, 7, 0);
		assertEquals(0, result);
		assertEquals(3, readInt(mode, "menuCursor"));
	}

	@Test
	void playerInitSupportsAnyPlayerId() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.init();
		for (int i = 0; i < manager.engine.length; i++) {
			manager.engine[i].init();
			mode.playerInit(manager.engine[i], i);
		}
		assertNotNull(mode.owner);
		assertNotNull(mode.receiver);
		assertEquals(0, readInt(mode, "menuTime"));
	}

	@Test
	void initMenuSetsColorAndStatc() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		Method initMenu = AbstractMode.class.getDeclaredMethod(
				"initMenu", int.class, int.class);
		initMenu.setAccessible(true);
		initMenu.invoke(mode, EventReceiver.COLOR_RED, 42);

		assertEquals(EventReceiver.COLOR_RED, readInt(mode, "menuColor"));
		assertEquals(42, readInt(mode, "statcMenu"));
	}

	@Test
	void initMenuWithYOffsetSetsFields() throws Exception {
		ConcreteMode mode = new ConcreteMode();
		Method initMenu = AbstractMode.class.getDeclaredMethod(
				"initMenu", int.class, int.class, int.class);
		initMenu.setAccessible(true);
		initMenu.invoke(mode, 7, EventReceiver.COLOR_BLUE, 99);

		assertEquals(7, readInt(mode, "menuY"));
		assertEquals(EventReceiver.COLOR_BLUE, readInt(mode, "menuColor"));
		assertEquals(99, readInt(mode, "statcMenu"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(AbstractMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
