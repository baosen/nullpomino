package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link SquareMode}'s private {@code grayoutBrokenBlocks}, which
 * walks the field's full vertical extent (including hidden rows above
 * y=0) and recolors any block carrying the {@code BROKEN} attribute to
 * {@link Block#BLOCK_COLOR_GRAY}. Empty cells, null cells, and blocks
 * without the BROKEN flag are left untouched.
 */
class SquareModeGrayoutBrokenBlocksTest {

	@Test
	void brokenBlocksAreRecoloredGray() throws Exception {
		// Single broken block in the middle of the field -> recolored.
		SquareMode mode = new SquareMode();
		Field field = freshField(mode);
		Block broken = new Block(Block.BLOCK_COLOR_RED);
		broken.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
		field.setBlock(3, 18, broken);

		invokeGrayout(mode, field);

		assertEquals(Block.BLOCK_COLOR_GRAY, field.getBlock(3, 18).color,
				"BROKEN block recolored to gray");
	}

	@Test
	void blocksWithoutBrokenFlagKeepTheirColor() throws Exception {
		// Plain colored block (no BROKEN attribute) -> color unchanged.
		SquareMode mode = new SquareMode();
		Field field = freshField(mode);
		field.setBlockColor(3, 18, Block.BLOCK_COLOR_BLUE);

		invokeGrayout(mode, field);

		assertEquals(Block.BLOCK_COLOR_BLUE, field.getBlock(3, 18).color,
				"non-BROKEN block keeps its color");
	}

	@Test
	void hiddenRowsAboveZeroAreAlsoScanned() throws Exception {
		// Place a broken block in the hidden upper region (y < 0). The
		// loop starts at -hiddenHeight, so this row is included.
		SquareMode mode = new SquareMode();
		Field field = freshField(mode);
		Block broken = new Block(Block.BLOCK_COLOR_GREEN);
		broken.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
		field.setBlock(2, -1, broken);

		invokeGrayout(mode, field);

		assertEquals(Block.BLOCK_COLOR_GRAY, field.getBlock(2, -1).color,
				"BROKEN block in hidden row also gets recolored");
	}

	@Test
	void mixedBrokenAndNormalBlocksOnlyTouchBroken() throws Exception {
		// Three blocks side-by-side; only the middle is BROKEN. Walk
		// must touch only that one.
		SquareMode mode = new SquareMode();
		Field field = freshField(mode);
		field.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);
		Block broken = new Block(Block.BLOCK_COLOR_BLUE);
		broken.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
		field.setBlock(3, 18, broken);
		field.setBlockColor(4, 18, Block.BLOCK_COLOR_GREEN);

		invokeGrayout(mode, field);

		assertEquals(Block.BLOCK_COLOR_RED, field.getBlock(2, 18).color);
		assertEquals(Block.BLOCK_COLOR_GRAY, field.getBlock(3, 18).color,
				"only the BROKEN-flagged block recolored");
		assertEquals(Block.BLOCK_COLOR_GREEN, field.getBlock(4, 18).color);
	}

	@Test
	void emptyFieldIsNoOp() throws Exception {
		// No blocks placed -> walk visits empty/null cells which are
		// skipped (the !isEmpty + getAttribute guards short-circuit).
		// We can't observe a no-op directly but we can confirm no NPE.
		SquareMode mode = new SquareMode();
		Field field = freshField(mode);

		invokeGrayout(mode, field);
	}

	private static Field freshField(SquareMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		gm.engine[0].createFieldIfNeeded();
		return gm.engine[0].field;
	}

	private static void invokeGrayout(SquareMode mode, Field field) throws Exception {
		Method m = SquareMode.class.getDeclaredMethod(
				"grayoutBrokenBlocks", Field.class);
		m.setAccessible(true);
		m.invoke(mode, field);
	}
}
