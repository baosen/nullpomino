package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Pins {@link PracticeMode}'s private {@code loadMap} / {@code saveMap}
 * — the field-state hooks used to round-trip preset map layouts. Save
 * delegates straight to {@link Field#writeProperty}; load resets the
 * field, reads back the property dump, and force-applies VISIBLE +
 * OUTLINE on every cell while clearing SELFPLACED.
 */
class PracticeModeMapPersistenceTest {

	@Test
	void saveMapWritesFieldShapeUnderTheGivenIdToProperties() throws Exception {
		// Field with a single block at (3, 18) -> saveMap dumps the
		// field property keys with the supplied id.
		PracticeMode mode = new PracticeMode();
		Field field = freshField(mode);
		field.setBlockColor(3, 18, Block.BLOCK_COLOR_RED);
		CustomProperties prop = new CustomProperties();

		invokeSaveMap(mode, field, prop, 7);

		// Field.writeProperty stores keys under id.field.* — round-trip
		// through readProperty to confirm the dump is materialised.
		Field roundtrip = newBareField();
		roundtrip.readProperty(prop, 7);
		assertEquals(Block.BLOCK_COLOR_RED, roundtrip.getBlock(3, 18).color,
				"saveMap dumps the field shape for the given id");
	}

	@Test
	void loadMapResetsFieldBeforeReadingNewMap() throws Exception {
		// Preload field with a different block, then save a fresh map
		// to a separate property dump and load it back -- the prior
		// block must be wiped because loadMap calls field.reset().
		PracticeMode mode = new PracticeMode();
		Field field = freshField(mode);
		// Preserve the original map (single block at 3,18) into props.
		field.setBlockColor(3, 18, Block.BLOCK_COLOR_RED);
		CustomProperties saved = new CustomProperties();
		field.writeProperty(saved, 0);

		// Spoil the field with a stray block before loading.
		field.setBlockColor(5, 17, Block.BLOCK_COLOR_BLUE);

		invokeLoadMap(mode, field, saved, 0);

		assertTrue(field.getBlockEmpty(5, 17),
				"loadMap.reset() wipes the spoiled block before readProperty");
		assertEquals(Block.BLOCK_COLOR_RED, field.getBlock(3, 18).color,
				"loadMap reads the saved block back into place");
	}

	@Test
	void loadMapForcesVisibleAndOutlineOnEveryCellAndClearsSelfplaced() throws Exception {
		// Save a field, mutate the saved-properties block to have
		// SELFPLACED set + clear VISIBLE/OUTLINE, then load it back.
		// loadMap should overwrite those attribute bits.
		PracticeMode mode = new PracticeMode();
		Field field = freshField(mode);
		Block b = new Block(Block.BLOCK_COLOR_BLUE);
		b.attribute = Block.BLOCK_ATTRIBUTE_SELFPLACED; // not visible, no outline
		field.setBlock(2, 18, b);
		CustomProperties saved = new CustomProperties();
		field.writeProperty(saved, 0);

		// Spoil the live field, then loadMap reads back.
		field.setBlockColor(2, 18, Block.BLOCK_COLOR_NONE);

		invokeLoadMap(mode, field, saved, 0);

		Block loaded = field.getBlock(2, 18);
		assertTrue(loaded.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"loadMap force-applies VISIBLE to every cell");
		assertTrue(loaded.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE),
				"loadMap force-applies OUTLINE to every cell");
		assertFalse(loaded.getAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED),
				"loadMap clears SELFPLACED on every cell so loaded maps "
						+ "are never confused with player-placed blocks");
	}

	@Test
	void saveMapAndLoadMapRoundTripPreservesBlockColors() throws Exception {
		// End-to-end: save then load with the same id should restore
		// the same colored layout (modulo the attribute-bit reapply).
		PracticeMode mode = new PracticeMode();
		Field field = freshField(mode);
		field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		field.setBlockColor(4, 19, Block.BLOCK_COLOR_GREEN);
		field.setBlockColor(9, 19, Block.BLOCK_COLOR_BLUE);

		CustomProperties prop = new CustomProperties();
		invokeSaveMap(mode, field, prop, 1);
		// Wipe the live field, then load back from id=1.
		field.reset();
		invokeLoadMap(mode, field, prop, 1);

		assertEquals(Block.BLOCK_COLOR_RED, field.getBlock(0, 19).color);
		assertEquals(Block.BLOCK_COLOR_GREEN, field.getBlock(4, 19).color);
		assertEquals(Block.BLOCK_COLOR_BLUE, field.getBlock(9, 19).color);
	}

	private static Field freshField(PracticeMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		gm.engine[0].createFieldIfNeeded();
		return gm.engine[0].field;
	}

	private static Field newBareField() {
		return new Field(10, 20, 4);
	}

	private static void invokeLoadMap(PracticeMode mode, Field field,
			CustomProperties prop, int id) throws Exception {
		Method m = PracticeMode.class.getDeclaredMethod(
				"loadMap", Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}

	private static void invokeSaveMap(PracticeMode mode, Field field,
			CustomProperties prop, int id) throws Exception {
		Method m = PracticeMode.class.getDeclaredMethod(
				"saveMap", Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}
}
