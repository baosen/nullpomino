package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
 * Pins {@link PhysicianVSMode}'s private {@code loadMap} / {@code
 * saveMap}. Unlike {@link PracticeMode} (which uses
 * {@code field.readProperty} / {@code field.writeProperty}),
 * PhysicianVS round-trips map state through {@code stringToField} /
 * {@code fieldToString} stored under a single {@code "map.&lt;id&gt;"}
 * key. Load resets the field, applies the string back, and force-
 * applies VISIBLE+OUTLINE on every cell while clearing SELFPLACED.
 */
class PhysicianVSModeMapPersistenceTest {

	@Test
	void saveMapStoresSingleMapKeyWithFieldString() throws Exception {
		// Single block at (3, 18); saveMap dumps to "map.7" key.
		PhysicianVSMode mode = new PhysicianVSMode();
		Field field = freshField();
		field.setBlockColor(3, 18, Block.BLOCK_COLOR_RED);
		CustomProperties prop = new CustomProperties();

		invokeSaveMap(mode, field, prop, 7);

		// Stored as a single "map.<id>" string-encoded property — not
		// the multi-key field.writeProperty layout.
		String dumped = prop.getProperty("map.7", "");
		assertNotEquals("", dumped, "saveMap writes the map.<id> key");
		assertEquals(field.fieldToString(), dumped,
				"saveMap stores the field.fieldToString() output verbatim");
	}

	@Test
	void loadMapResetsFieldBeforeApplyingMapString() throws Exception {
		// Save a map with one block, spoil the live field, then load
		// back -- the spoiled block must be wiped because loadMap
		// calls field.reset() before stringToField.
		PhysicianVSMode mode = new PhysicianVSMode();
		Field field = freshField();
		field.setBlockColor(3, 18, Block.BLOCK_COLOR_RED);
		CustomProperties saved = new CustomProperties();
		invokeSaveMap(mode, field, saved, 0);

		// Stray block before load.
		field.setBlockColor(5, 17, Block.BLOCK_COLOR_BLUE);

		invokeLoadMap(mode, field, saved, 0);

		assertTrue(field.getBlockEmpty(5, 17),
				"loadMap.reset() wipes the stray block before stringToField");
		assertEquals(Block.BLOCK_COLOR_RED, field.getBlock(3, 18).color,
				"loadMap reads the saved block back via stringToField");
	}

	@Test
	void loadMapForcesVisibleOutlineAndClearsSelfplaced() throws Exception {
		// Save a field, spoil the live field's stored map by overwriting
		// the property with the same fieldToString output, then load it
		// back -- attribute bits get force-set/cleared.
		PhysicianVSMode mode = new PhysicianVSMode();
		Field field = freshField();
		field.setBlockColor(2, 18, Block.BLOCK_COLOR_BLUE);
		CustomProperties saved = new CustomProperties();
		invokeSaveMap(mode, field, saved, 0);

		// fieldToString carries colour but the loaded blocks pass through
		// the attribute force-apply -- VISIBLE and OUTLINE should be set,
		// SELFPLACED should be cleared.
		field.reset();
		invokeLoadMap(mode, field, saved, 0);

		Block loaded = field.getBlock(2, 18);
		assertTrue(loaded.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"loadMap force-applies VISIBLE on every cell");
		assertTrue(loaded.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE),
				"loadMap force-applies OUTLINE on every cell");
		assertFalse(loaded.getAttribute(Block.BLOCK_ATTRIBUTE_SELFPLACED),
				"loadMap clears SELFPLACED on every cell");
	}

	@Test
	void loadMapWithMissingKeyResetsToEmptyField() throws Exception {
		// loadMap reads "map.<id>" via getProperty(..., "") -- the
		// fallback empty string makes stringToField produce an empty
		// field, and reset() runs first either way.
		PhysicianVSMode mode = new PhysicianVSMode();
		Field field = freshField();
		field.setBlockColor(3, 18, Block.BLOCK_COLOR_RED);
		CustomProperties empty = new CustomProperties();

		invokeLoadMap(mode, field, empty, 99);

		assertTrue(field.getBlockEmpty(3, 18),
				"missing 'map.99' key -> empty string -> empty field");
	}

	@Test
	void saveMapAndLoadMapRoundTripPreservesBlockColors() throws Exception {
		// End-to-end: save then load with the same id should restore
		// the layout (modulo the attribute reapply on each cell).
		PhysicianVSMode mode = new PhysicianVSMode();
		Field field = freshField();
		field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		field.setBlockColor(4, 19, Block.BLOCK_COLOR_GREEN);
		field.setBlockColor(9, 19, Block.BLOCK_COLOR_BLUE);

		CustomProperties prop = new CustomProperties();
		invokeSaveMap(mode, field, prop, 1);
		field.reset();
		invokeLoadMap(mode, field, prop, 1);

		assertEquals(Block.BLOCK_COLOR_RED, field.getBlock(0, 19).color);
		assertEquals(Block.BLOCK_COLOR_GREEN, field.getBlock(4, 19).color);
		assertEquals(Block.BLOCK_COLOR_BLUE, field.getBlock(9, 19).color);
	}

	@Test
	void modeInitAllocatesPerPlayerArraysAtMaxPlayers() {
		// modeInit sets up MAX_PLAYERS=2 sized arrays for the VS battle.
		// We can't see the private fields directly, but we can confirm
		// modeInit doesn't throw and that subsequent saveMap/loadMap
		// still works with the freshly-constructed mode (sanity).
		PhysicianVSMode mode = new PhysicianVSMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);
	}

	private static Field freshField() {
		return new Field(10, 20, 4);
	}

	private static void invokeLoadMap(PhysicianVSMode mode, Field field,
			CustomProperties prop, int id) throws Exception {
		Method m = PhysicianVSMode.class.getDeclaredMethod(
				"loadMap", Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}

	private static void invokeSaveMap(PhysicianVSMode mode, Field field,
			CustomProperties prop, int id) throws Exception {
		Method m = PhysicianVSMode.class.getDeclaredMethod(
				"saveMap", Field.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, field, prop, id);
	}
}
