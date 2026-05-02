package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers the private {@code loadMap} and {@code saveMap} helpers in
 * {@link SPFMode}: property round-trip, attribute forcing, and reset-on-load.
 */
class SPFModeHelpersTest {

	@Test
	void saveMapWritesFieldStringUnderMapDotId() throws Exception {
		SPFMode mode = new SPFMode();
		freshEngine(mode);
		Field field = new Field();
		// Put a single block at (0, 0) so fieldToString is non-empty.
		Block blk = new Block(Block.BLOCK_COLOR_RED);
		field.setBlock(0, 0, blk);
		CustomProperties prop = new CustomProperties();

		invokeSaveMap(mode, field, prop, 7);

		String stored = prop.getProperty("map.7", null);
		assertNotNull(stored, "saveMap must write under key 'map.7'");
		assertFalse(stored.isEmpty(), "stored string must not be empty for a non-blank field");
	}

	@Test
	void loadMapRestoresFieldFromSavedString() throws Exception {
		SPFMode mode = new SPFMode();
		freshEngine(mode);
		Field original = new Field();
		Block blk = new Block(Block.BLOCK_COLOR_BLUE);
		original.setBlock(2, 3, blk);
		CustomProperties prop = new CustomProperties();
		invokeSaveMap(mode, original, prop, 0);

		Field restored = new Field();
		invokeLoadMap(mode, restored, prop, 0);

		// The loaded field must contain a block at (2,3) with the same color.
		assertFalse(restored.getBlockEmpty(2, 3), "block at (2,3) must be present after load");
		assertEquals(Block.BLOCK_COLOR_BLUE, restored.getBlock(2, 3).color);
	}

	@Test
	void loadMapForcesVisibleAndOutlineAttributesOnAllBlocks() throws Exception {
		SPFMode mode = new SPFMode();
		freshEngine(mode);
		Field field = new Field();
		Block blk = new Block(Block.BLOCK_COLOR_RED);
		// Explicitly clear VISIBLE and OUTLINE before saving.
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, false);
		field.setBlock(1, 1, blk);
		CustomProperties prop = new CustomProperties();
		invokeSaveMap(mode, field, prop, 3);

		Field loaded = new Field();
		invokeLoadMap(mode, loaded, prop, 3);

		Block result = loaded.getBlock(1, 1);
		assertNotNull(result);
		assertTrue(result.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"loadMap must set VISIBLE on every block");
		assertTrue(result.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE),
				"loadMap must set OUTLINE on every block");
	}

	@Test
	void loadMapClearsSelfPlacedAttribute() throws Exception {
		SPFMode mode = new SPFMode();
		freshEngine(mode);
		Field field = new Field();
		Block blk = new Block(Block.BLOCK_COLOR_GREEN);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED, true);
		field.setBlock(0, 2, blk);
		CustomProperties prop = new CustomProperties();
		invokeSaveMap(mode, field, prop, 1);

		Field loaded = new Field();
		invokeLoadMap(mode, loaded, prop, 1);

		Block result = loaded.getBlock(0, 2);
		assertNotNull(result);
		assertFalse(result.getAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED),
				"loadMap must clear SELFPLACED on every block");
	}

	@Test
	void loadMapWithEmptyPropertyResetsField() throws Exception {
		SPFMode mode = new SPFMode();
		freshEngine(mode);
		Field field = new Field();
		Block blk = new Block(Block.BLOCK_COLOR_RED);
		field.setBlock(3, 3, blk);
		// Property has no "map.9" key -> defaults to empty string -> reset.
		CustomProperties prop = new CustomProperties();

		invokeLoadMap(mode, field, prop, 9);

		assertTrue(field.getBlockEmpty(3, 3),
				"loadMap with missing key must reset field (block at 3,3 gone)");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(SPFMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeLoadMap(SPFMode mode, Field field, CustomProperties prop, int id)
			throws Exception {
		Method m = SPFMode.class.getDeclaredMethod("loadMap", Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}

	private static void invokeSaveMap(SPFMode mode, Field field, CustomProperties prop, int id)
			throws Exception {
		Method m = SPFMode.class.getDeclaredMethod("saveMap", Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}
}
