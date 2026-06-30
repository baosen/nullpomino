package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Targets the still-uncovered <em>branch</em> paths inside
 * {@link SPFMode#checkSquares(GameEngine, int, boolean)} that the existing SPF
 * test suite does not reach: the right/down boundary-scan colour-mismatch and
 * multi-cell-continue branches, the {@code maxX <= minX} no-square skip, and the
 * four expansion loops' colour-mismatch breaks, edge-connect breaks and inner
 * per-cell mismatch / connect rejections.
 *
 * <p>(The two {@code test == null} branches of the boundary scans — lines
 * 1222-1223 and 1239-1240 — are unreachable dead code: the enclosing
 * {@code while (maxX < width)} / {@code while (maxY < height)} guarantees the
 * lookup is always in range, so {@code getBlock} never returns {@code null}.)
 */
class SPFModeCheckSquaresBranchTest {

	private static final int RED = Block.BLOCK_COLOR_RED;
	private static final int BLUE = Block.BLOCK_COLOR_BLUE;

	/** Fresh single-engine SPF setup with a created field. */
	private static GameEngine eng(SPFMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		manager.engine[0].createFieldIfNeeded();
		return manager.engine[0];
	}

	/** Places a block of {@code color} at (x,y) with the given attributes set true. */
	private static Block put(GameEngine engine, int x, int y, int color, int... attrsTrue) {
		Block b = new Block(color);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, false);
		for (int a : attrsTrue)
			b.setAttribute(a, true);
		engine.field.setBlock(x, y, b);
		return b;
	}

	/** The connect-attributed top-left cell of a gem square (no UP/LEFT). */
	private static Block topLeft(GameEngine engine, int x, int y, int color) {
		return put(engine, x, y, color,
				Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, Block.BLOCK_ATTRIBUTE_CONNECT_DOWN);
	}

	/** Builds a rectangular pre-existing gem square with the expected connect flags. */
	private static void buildGemSquare(GameEngine engine, int minX, int minY,
			int maxX, int maxY, int color) {
		for (int x = minX; x <= maxX; x++)
			for (int y = minY; y <= maxY; y++) {
				Block b = new Block(color);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, false);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, x != minX);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, x != maxX);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, y != minY);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, y != maxY);
				b.bonusValue = Math.min(maxX - minX + 1, maxY - minY + 1);
				engine.field.setBlock(x, y, b);
			}
	}

	private static void check(SPFMode mode, GameEngine engine) throws Exception {
		Method m = SPFMode.class.getDeclaredMethod(
				"checkSquares", GameEngine.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, true);
	}

	/**
	 * A 3x3 pre-existing square exercises the right-scan {@code maxX++} continue
	 * (1232) and down-scan {@code maxY++} continue (1249) — a 2x2 square breaks
	 * out before the middle cell. Surrounded by empty cells, all four expansion
	 * loops break on the colour check (1292, 1318, 1344, 1370).
	 */
	@Test
	void threeByThreeScanContinueAndExpandColourBreaks() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = eng(mode);
		buildGemSquare(engine, 3, 5, 5, 7, RED);

		check(mode, engine);

		// Square is left intact (nothing expanded into the empty surroundings).
		assertEquals(RED, engine.field.getBlockColor(4, 6));
	}

	/**
	 * A lone connect-flagged top-left with a differently-coloured cell to the
	 * right and below hits the right-scan colour break (1227-1228) and the
	 * down-scan colour break (1244-1245); the collapsed bounds then take the
	 * {@code maxX <= minX} no-square continue (1280).
	 */
	@Test
	void scanColourMismatchBreaksAndNoSquareContinue() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = eng(mode);
		topLeft(engine, 3, 6, RED);
		put(engine, 4, 6, BLUE);   // right neighbour, wrong colour
		put(engine, 3, 7, BLUE);   // down neighbour, wrong colour

		check(mode, engine);

		assertEquals(RED, engine.field.getBlockColor(3, 6));
	}

	/**
	 * Each expansion direction has its edge-connect break exercised: a 2x2 square
	 * with same-colour neighbours that carry a connect flag the expander treats as
	 * "already linked", hitting 1295 (up), 1321 (left), 1347 (right), 1373 (down).
	 */
	@Test
	void expansionEdgeConnectBreaks() throws Exception {
		// --- up: above row same colour, left edge already CONNECT_LEFT (1295) ---
		SPFMode mode = new SPFMode();
		GameEngine engine = eng(mode);
		buildGemSquare(engine, 3, 6, 4, 7, RED);
		put(engine, 3, 5, RED, Block.BLOCK_ATTRIBUTE_CONNECT_LEFT);
		put(engine, 4, 5, RED);
		check(mode, engine);

		// --- left: left col same colour, top edge already CONNECT_UP (1321) ---
		mode = new SPFMode();
		engine = eng(mode);
		buildGemSquare(engine, 3, 6, 4, 7, RED);
		put(engine, 2, 6, RED, Block.BLOCK_ATTRIBUTE_CONNECT_UP);
		put(engine, 2, 7, RED);
		check(mode, engine);

		// --- right: right col same colour, top edge already CONNECT_UP (1347) ---
		mode = new SPFMode();
		engine = eng(mode);
		buildGemSquare(engine, 3, 6, 4, 7, RED);
		put(engine, 5, 6, RED, Block.BLOCK_ATTRIBUTE_CONNECT_UP);
		put(engine, 5, 7, RED);
		check(mode, engine);

		// --- down: below row same colour, left edge already CONNECT_LEFT (1373) -
		mode = new SPFMode();
		engine = eng(mode);
		buildGemSquare(engine, 3, 6, 4, 7, RED);
		put(engine, 3, 8, RED, Block.BLOCK_ATTRIBUTE_CONNECT_LEFT);
		put(engine, 4, 8, RED);
		check(mode, engine);

		assertNotNull(engine.field.getBlock(3, 6));
	}

	/**
	 * Expand-up inner rejections: a 3-wide square whose first row above carries a
	 * middle cell already CONNECT_UP (1305, no expand) and whose second row above
	 * has a wrong-colour middle cell (1301-1302, done).
	 */
	@Test
	void expandUpInnerRejections() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = eng(mode);
		buildGemSquare(engine, 3, 6, 5, 7, RED);
		// row minY-1 = 5: edges RED, middle RED with CONNECT_UP -> 1305
		put(engine, 3, 5, RED);
		put(engine, 4, 5, RED, Block.BLOCK_ATTRIBUTE_CONNECT_UP);
		put(engine, 5, 5, RED);
		// row minY-2 = 4: edges RED, middle BLUE -> 1301/1302
		put(engine, 3, 4, RED);
		put(engine, 4, 4, BLUE);
		put(engine, 5, 4, RED);

		check(mode, engine);
		assertEquals(RED, engine.field.getBlockColor(4, 6));
	}

	/**
	 * Expand-left inner rejections: a 3-tall square whose first column to the left
	 * carries a middle cell already CONNECT_LEFT (1331) and whose second column
	 * has a wrong-colour middle cell (1327-1328).
	 */
	@Test
	void expandLeftInnerRejections() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = eng(mode);
		buildGemSquare(engine, 4, 5, 5, 7, RED);
		// col minX-1 = 3: edges RED, middle RED with CONNECT_LEFT -> 1331
		put(engine, 3, 5, RED);
		put(engine, 3, 6, RED, Block.BLOCK_ATTRIBUTE_CONNECT_LEFT);
		put(engine, 3, 7, RED);
		// col minX-2 = 2: edges RED, middle BLUE -> 1327/1328
		put(engine, 2, 5, RED);
		put(engine, 2, 6, BLUE);
		put(engine, 2, 7, RED);

		check(mode, engine);
		assertEquals(RED, engine.field.getBlockColor(4, 6));
	}

	/**
	 * Expand-right inner rejections: a 3-tall square whose first column to the
	 * right carries a middle cell already CONNECT_RIGHT (1357) and whose second
	 * column has a wrong-colour middle cell (1353-1354).
	 */
	@Test
	void expandRightInnerRejections() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = eng(mode);
		buildGemSquare(engine, 3, 5, 4, 7, RED);
		// col maxX+1 = 5: edges RED, middle RED with CONNECT_RIGHT -> 1357
		put(engine, 5, 5, RED);
		put(engine, 5, 6, RED, Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT);
		put(engine, 5, 7, RED);
		// col maxX+2 = 6: edges RED, middle BLUE -> 1353/1354
		put(engine, 6, 5, RED);
		put(engine, 6, 6, BLUE);
		put(engine, 6, 7, RED);

		check(mode, engine);
		assertEquals(RED, engine.field.getBlockColor(3, 6));
	}

	/**
	 * Expand-down inner rejections: a 3-wide square whose first row below carries
	 * a middle cell already CONNECT_DOWN (1383) and whose second row below has a
	 * wrong-colour middle cell (1379-1380).
	 */
	@Test
	void expandDownInnerRejections() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = eng(mode);
		buildGemSquare(engine, 3, 5, 5, 6, RED);
		// row maxY+1 = 7: edges RED, middle RED with CONNECT_DOWN -> 1383
		put(engine, 3, 7, RED);
		put(engine, 4, 7, RED, Block.BLOCK_ATTRIBUTE_CONNECT_DOWN);
		put(engine, 5, 7, RED);
		// row maxY+2 = 8: edges RED, middle BLUE -> 1379/1380
		put(engine, 3, 8, RED);
		put(engine, 4, 8, BLUE);
		put(engine, 5, 8, RED);

		check(mode, engine);
		assertEquals(RED, engine.field.getBlockColor(4, 5));
	}
}
