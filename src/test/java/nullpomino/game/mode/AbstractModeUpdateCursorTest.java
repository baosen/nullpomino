package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AbstractMode}'s shared menu-navigation primitive
 * {@code updateCursor}, the cursor input the mode-config screens of
 * every concrete mode delegate to. UP wraps from 0 to maxCursor; DOWN
 * wraps from maxCursor to 0; LEFT returns -1; RIGHT returns +1; no
 * input returns 0. UP/DOWN do not return non-zero — only LEFT/RIGHT
 * do, which lets modes treat the return as a per-frame "value
 * delta" while still letting cursor navigation flow through.
 */
class AbstractModeUpdateCursorTest {

	private static final class StubMode extends AbstractMode {
	}

	@Test
	void noInputReturnsZeroAndLeavesCursorAlone() throws Exception {
		StubMode mode = new StubMode();
		mode.setMenuCursor(3);
		GameEngine engine = freshEngine();

		int change = invokeUpdateCursor(mode, engine, 9);

		assertEquals(0, change, "no input -> change = 0");
		assertEquals(3, mode.getMenuCursor(), "cursor unchanged with no input");
	}

	@Test
	void upArrowDecrementsCursorAndReturnsZero() throws Exception {
		StubMode mode = new StubMode();
		mode.setMenuCursor(5);
		GameEngine engine = freshEngine();
		press(engine, Controller.BUTTON_UP);

		int change = invokeUpdateCursor(mode, engine, 9);

		assertEquals(0, change,
				"UP/DOWN affect cursor but return 0 (only L/R indicate value delta)");
		assertEquals(4, mode.getMenuCursor(), "UP -> cursor decrements");
	}

	@Test
	void upArrowAtCursorZeroWrapsToMaxCursor() throws Exception {
		// Cursor=0 + UP -> wrap to maxCursor.
		StubMode mode = new StubMode();
		mode.setMenuCursor(0);
		GameEngine engine = freshEngine();
		press(engine, Controller.BUTTON_UP);

		invokeUpdateCursor(mode, engine, 7);

		assertEquals(7, mode.getMenuCursor(),
				"UP at cursor 0 wraps to maxCursor=7");
	}

	@Test
	void downArrowIncrementsCursorAndReturnsZero() throws Exception {
		StubMode mode = new StubMode();
		mode.setMenuCursor(2);
		GameEngine engine = freshEngine();
		press(engine, Controller.BUTTON_DOWN);

		int change = invokeUpdateCursor(mode, engine, 9);

		assertEquals(0, change);
		assertEquals(3, mode.getMenuCursor(), "DOWN -> cursor increments");
	}

	@Test
	void downArrowAtMaxCursorWrapsToZero() throws Exception {
		StubMode mode = new StubMode();
		mode.setMenuCursor(7);
		GameEngine engine = freshEngine();
		press(engine, Controller.BUTTON_DOWN);

		invokeUpdateCursor(mode, engine, 7);

		assertEquals(0, mode.getMenuCursor(),
				"DOWN at cursor=maxCursor=7 wraps to 0");
	}

	@Test
	void leftArrowReturnsMinusOneAndLeavesCursorAlone() throws Exception {
		StubMode mode = new StubMode();
		mode.setMenuCursor(4);
		GameEngine engine = freshEngine();
		press(engine, Controller.BUTTON_LEFT);

		int change = invokeUpdateCursor(mode, engine, 9);

		assertEquals(-1, change, "LEFT -> -1");
		assertEquals(4, mode.getMenuCursor(),
				"LEFT does not move cursor (only L/R adjust the value)");
	}

	@Test
	void rightArrowReturnsOneAndLeavesCursorAlone() throws Exception {
		StubMode mode = new StubMode();
		mode.setMenuCursor(4);
		GameEngine engine = freshEngine();
		press(engine, Controller.BUTTON_RIGHT);

		int change = invokeUpdateCursor(mode, engine, 9);

		assertEquals(1, change, "RIGHT -> +1");
		assertEquals(4, mode.getMenuCursor(),
				"RIGHT does not move cursor");
	}

	@Test
	void upAndLeftTogetherMoveCursorAndReportLeft() throws Exception {
		// Both UP and LEFT pressed in the same frame: cursor decrements
		// (UP wins for cursor movement) AND change reports -1 (LEFT).
		StubMode mode = new StubMode();
		mode.setMenuCursor(5);
		GameEngine engine = freshEngine();
		press(engine, Controller.BUTTON_UP);
		press(engine, Controller.BUTTON_LEFT);

		int change = invokeUpdateCursor(mode, engine, 9);

		assertEquals(-1, change, "LEFT still wins for the change return value");
		assertEquals(4, mode.getMenuCursor(),
				"UP still decrements the cursor in the same frame");
	}

	@Test
	void rightWinsOverLeftWhenBothArePressedSameFrame() throws Exception {
		// updateCursor checks LEFT first, returning -1 if pressed; only
		// then checks RIGHT. So LEFT has priority when both are held —
		// this characterisation pins the precedence.
		StubMode mode = new StubMode();
		GameEngine engine = freshEngine();
		press(engine, Controller.BUTTON_LEFT);
		press(engine, Controller.BUTTON_RIGHT);

		int change = invokeUpdateCursor(mode, engine, 9);

		assertEquals(-1, change,
				"both L+R held -> LEFT wins (-1); RIGHT not even checked");
	}

	@Test
	void singleArgUpdateCursorDelegatesToTwoArg() throws Exception {
		// updateCursor(engine, max) is a thin wrapper for
		// updateCursor(engine, max, 0). Behaviour should be identical
		// for the same inputs.
		StubMode mode = new StubMode();
		mode.setMenuCursor(3);
		GameEngine engine = freshEngine();
		press(engine, Controller.BUTTON_DOWN);

		Method m = AbstractMode.class.getDeclaredMethod(
				"updateCursor", GameEngine.class, int.class);
		m.setAccessible(true);
		int change = (int) m.invoke(mode, engine, 9);

		assertEquals(0, change);
		assertEquals(4, mode.getMenuCursor(),
				"single-arg overload behaves like two-arg with playerID=0");
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		// Replace the controller with a fresh one so isPress sees only
		// the buttons we set explicitly.
		gm.engine[0].ctrl = new Controller();
		return gm.engine[0];
	}

	private static void press(GameEngine engine, int button) {
		engine.ctrl.buttonPress[button] = true;
		engine.ctrl.buttonTime[button] = 1;
	}

	private static int invokeUpdateCursor(AbstractMode mode, GameEngine engine,
			int maxCursor) throws Exception {
		Method m = AbstractMode.class.getDeclaredMethod(
				"updateCursor", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, engine, maxCursor, 0);
	}
}
