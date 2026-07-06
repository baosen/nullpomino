package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link FieldCascade}.
 *
 * <p>The gravity routines re-scan the whole field after every group fall,
 * so a group that lands inside a not-yet-visited cell of the current scan
 * is revisited while it still carries {@code BLOCK_ATTRIBUTE_CASCADE_FALL}.
 * These tests build exactly those shapes (connected groups whose members
 * land in later scan positions) to drive the {@code TEMP_MARK &&
 * CASCADE_FALL} guards, plus the "supported by a marked / unmarked block"
 * decisions in the fall checks.
 */
class FieldCascadeBranchGapTest {

	/** Field with no hidden rows so row indices match visible coordinates. */
	private static Field field() {
		return new Field(10, 10, 0, false);
	}

	private static Block block(int color, int connect) {
		Block b = new Block(color, 0, connect);
		return b;
	}

	// ──────────────────────────────────────────────────────────
	// doCascadeGravity (fast, bottom-up scan)
	// ──────────────────────────────────────────────────────────

	/**
	 * L-shaped connected group: A(0,5) under C(0,4), D(1,4) to C's right.
	 * When the outer scan (bottom-up) processes A at (row 5, col 0) the
	 * whole group falls one cell, which drops D into (1,5) — a cell of the
	 * *current* row that the column loop has not visited yet. The scan then
	 * re-marks the fallen group, whose blocks all carry CASCADE_FALL:
	 * the check loop and the fall loop must both skip them.
	 * C resting on marked A also exercises the "below is occupied but
	 * TEMP_MARKed" (not blocking) branch of the support check.
	 */
	@Test
	void fastCascadeRevisitsGroupThatFellIntoCurrentRow() {
		Field f = field();
		f.setBlock(0, 5, block(Block.BLOCK_COLOR_RED,
				Block.BLOCK_ATTRIBUTE_CONNECT_UP));
		f.setBlock(0, 4, block(Block.BLOCK_COLOR_RED,
				Block.BLOCK_ATTRIBUTE_CONNECT_DOWN | Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT));
		f.setBlock(1, 4, block(Block.BLOCK_COLOR_RED,
				Block.BLOCK_ATTRIBUTE_CONNECT_LEFT));

		assertTrue(FieldCascade.doCascadeGravity(f), "group should fall");

		// One call moves the group exactly one cell (revisit must not double-move).
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 6), "A fell one cell");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 5), "C fell one cell");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(1, 5), "D fell one cell");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 4));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(1, 4));

		// Attributes are wiped after the pass.
		assertFalse(f.getBlock(1, 5).getAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL));
		assertFalse(f.getBlock(1, 5).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK));

		// Iterate to rest: group lands on the floor intact.
		int guard = 0;
		while (FieldCascade.doCascadeGravity(f)) {
			assertTrue(++guard < 30, "cascade must terminate");
		}
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 9));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 8));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(1, 8));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(1, 9),
				"D stays attached one row above the floor");
	}

	/** A connected group resting (via any member) on an unmarked block must not fall. */
	@Test
	void fastCascadeGroupBlockedByUnmarkedSupportDoesNotFall() {
		Field f = field();
		// Support block, no connections.
		f.setBlock(1, 9, block(Block.BLOCK_COLOR_GRAY, 0));
		// Horizontal pair on row 8; right member sits on the support.
		f.setBlock(0, 8, block(Block.BLOCK_COLOR_BLUE,
				Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT));
		f.setBlock(1, 8, block(Block.BLOCK_COLOR_BLUE,
				Block.BLOCK_ATTRIBUTE_CONNECT_LEFT));

		assertFalse(FieldCascade.doCascadeGravity(f), "supported group must not move");
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 8));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(1, 8));
		assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(1, 9));
	}

	// ──────────────────────────────────────────────────────────
	// doCascadeSlow (top-down scan)
	// ──────────────────────────────────────────────────────────

	/**
	 * A lone falling block lands in a row the top-down scan has not reached
	 * yet, so the scan revisits it while it carries CASCADE_FALL: both the
	 * support-check loop and the fall loop must skip it, and the block must
	 * move only one cell per call.
	 */
	@Test
	void slowCascadeMovesLoneBlockExactlyOneCellPerCall() {
		Field f = field();
		f.setBlock(3, 2, block(Block.BLOCK_COLOR_GREEN, 0));

		assertTrue(FieldCascade.doCascadeSlow(f));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(3, 2));
		assertEquals(Block.BLOCK_COLOR_GREEN, f.getBlockColor(3, 3),
				"block falls exactly one cell despite being revisited");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(3, 4),
				"revisit while CASCADE_FALL must not double-move");

		int guard = 0;
		while (FieldCascade.doCascadeSlow(f)) {
			assertTrue(++guard < 30, "cascade must terminate");
		}
		assertEquals(Block.BLOCK_COLOR_GREEN, f.getBlockColor(3, 9));
	}

	/** Slow cascade: a block resting on an unmarked block is blocked. */
	@Test
	void slowCascadeBlockRestingOnUnmarkedBlockDoesNotFall() {
		Field f = field();
		f.setBlock(0, 9, block(Block.BLOCK_COLOR_GRAY, 0)); // support on floor
		f.setBlock(0, 8, block(Block.BLOCK_COLOR_RED, 0));  // resting on it

		assertFalse(FieldCascade.doCascadeSlow(f));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 8));
		assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(0, 9));
	}

	/**
	 * Slow cascade: a vertically connected pair falls as one unit. The top
	 * member rests on the marked bottom member (occupied-but-marked support
	 * branch), and after the fall the top-down scan revisits both members
	 * at their new positions while they carry CASCADE_FALL.
	 */
	@Test
	void slowCascadeConnectedVerticalPairFallsTogether() {
		Field f = field();
		f.setBlock(0, 3, block(Block.BLOCK_COLOR_CYAN,
				Block.BLOCK_ATTRIBUTE_CONNECT_DOWN));
		f.setBlock(0, 4, block(Block.BLOCK_COLOR_CYAN,
				Block.BLOCK_ATTRIBUTE_CONNECT_UP));

		assertTrue(FieldCascade.doCascadeSlow(f));
		assertEquals(Block.BLOCK_COLOR_CYAN, f.getBlockColor(0, 4));
		assertEquals(Block.BLOCK_COLOR_CYAN, f.getBlockColor(0, 5));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 3));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 6),
				"pair moves one cell per call, not two");

		int guard = 0;
		while (FieldCascade.doCascadeSlow(f)) {
			assertTrue(++guard < 30, "cascade must terminate");
		}
		assertEquals(Block.BLOCK_COLOR_CYAN, f.getBlockColor(0, 9));
		assertEquals(Block.BLOCK_COLOR_CYAN, f.getBlockColor(0, 8));
	}

	// ──────────────────────────────────────────────────────────
	// canCascade
	// ──────────────────────────────────────────────────────────

	/**
	 * A floating vertically connected pair can cascade: the upper member's
	 * support is the marked lower member, which must not count as blocking.
	 */
	@Test
	void canCascadeTrueForFloatingConnectedVerticalPair() {
		Field f = field();
		f.setBlock(2, 3, block(Block.BLOCK_COLOR_YELLOW,
				Block.BLOCK_ATTRIBUTE_CONNECT_DOWN));
		f.setBlock(2, 4, block(Block.BLOCK_COLOR_YELLOW,
				Block.BLOCK_ATTRIBUTE_CONNECT_UP));

		assertTrue(FieldCascade.canCascade(f));
	}

	/**
	 * canCascade never clears CASCADE_FALL itself; a marked block that
	 * already carries the attribute is skipped by the support check, so a
	 * lone floating block pre-flagged with CASCADE_FALL still reports true
	 * (nothing marks it as blocked).
	 */
	@Test
	void canCascadeSkipsMarkedBlockAlreadyFlaggedCascadeFall() {
		Field f = field();
		Block b = block(Block.BLOCK_COLOR_PURPLE, 0);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_CASCADE_FALL, true);
		f.setBlock(4, 5, b);

		assertTrue(FieldCascade.canCascade(f));
	}

	/** Sanity: a grounded stack cannot cascade. */
	@Test
	void canCascadeFalseForGroundedStack() {
		Field f = field();
		f.setBlock(5, 9, block(Block.BLOCK_COLOR_ORANGE, 0));
		f.setBlock(5, 8, block(Block.BLOCK_COLOR_ORANGE, 0));

		assertFalse(FieldCascade.canCascade(f));
	}
}
