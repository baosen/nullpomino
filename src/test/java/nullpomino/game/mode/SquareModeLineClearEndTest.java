package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link SquareMode#lineClearEnd}'s TNT-avalanche cut-empty-line
 * behaviour. The method only fires when three conditions are true:
 * lineGravityType is CASCADE, lineGravityTotalLines > 0, and
 * tntAvalanche is on. When all three hold, it walks the field
 * bottom-to-top, cuts the first empty line it finds, decrements the
 * pending-line counter by one, and returns true (signalling the
 * engine should re-cascade). Otherwise returns false.
 */
class SquareModeLineClearEndTest {

	@Test
	void noopWhenLineGravityTypeIsNotCascade() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.lineGravityType = GameEngine.LineGravity.NATIVE;
		engine.lineGravityTotalLines = 5;
		setBoolean(mode, "tntAvalanche", true);

		boolean handled = mode.lineClearEnd(engine, 0);

		assertFalse(handled,
				"non-CASCADE gravity type -> no TNT avalanche fires");
		assertEquals(5, engine.lineGravityTotalLines,
				"lineGravityTotalLines must stay untouched");
	}

	@Test
	void noopWhenLineGravityTotalLinesIsZero() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.lineGravityType = GameEngine.LineGravity.CASCADE;
		engine.lineGravityTotalLines = 0;
		setBoolean(mode, "tntAvalanche", true);

		boolean handled = mode.lineClearEnd(engine, 0);

		assertFalse(handled,
				"no pending lines -> no TNT avalanche fires");
	}

	@Test
	void noopWhenTntAvalancheIsOff() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.lineGravityType = GameEngine.LineGravity.CASCADE;
		engine.lineGravityTotalLines = 5;
		setBoolean(mode, "tntAvalanche", false);

		boolean handled = mode.lineClearEnd(engine, 0);

		assertFalse(handled,
				"tntAvalanche=false -> no TNT avalanche fires");
	}

	@Test
	void cutsTheFirstEmptyLineFromTheBottomAndDecrementsPendingLines() throws Exception {
		// Stack the field so row at 'height-1' is empty (no cells filled
		// at the bottom row), but row 'height-2' has a colored block.
		// TNT avalanche should walk bottom-up, find the empty row, and
		// cut it.
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.lineGravityType = GameEngine.LineGravity.CASCADE;
		engine.lineGravityTotalLines = 3;
		setBoolean(mode, "tntAvalanche", true);
		// Plant a block in row (height-2) so that row isn't empty.
		int height = engine.field.getHeight();
		engine.field.setBlockColor(0, height - 2, Block.BLOCK_COLOR_RED);

		boolean handled = mode.lineClearEnd(engine, 0);

		assertTrue(handled,
				"empty bottom row found -> handled=true (engine re-cascades)");
		assertEquals(2, engine.lineGravityTotalLines,
				"one line consumed -> counter decrements by 1");
	}

	@Test
	void firstEmptyLineFoundFromBottomCutsThatRowAndStops() throws Exception {
		// Plant blocks in the bottom row only — the next-up empty row
		// is the one that gets cut.
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.lineGravityType = GameEngine.LineGravity.CASCADE;
		engine.lineGravityTotalLines = 3;
		setBoolean(mode, "tntAvalanche", true);
		int height = engine.field.getHeight();
		int width = engine.field.getWidth();
		// Fill the bottom row.
		for(int x = 0; x < width; x++) {
			engine.field.setBlockColor(x, height - 1, Block.BLOCK_COLOR_RED);
		}

		boolean handled = mode.lineClearEnd(engine, 0);

		// One empty row was cut (somewhere above the bottom).
		assertTrue(handled);
		assertEquals(2, engine.lineGravityTotalLines);
	}

	private static GameEngine freshEngine(SquareMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void setBoolean(Object instance, String name, boolean value)
			throws Exception {
		Field f = findField(instance.getClass(), name);
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
}
