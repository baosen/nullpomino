package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BlockTest {

	@BeforeEach
	void resetRainbowPhase() {
		Block.rainbowPhase = 0;
	}

	@Test
	void defaultConstructorAppliesResetValues() {
		Block b = new Block();

		assertEquals(Block.BLOCK_COLOR_NONE, b.color);
		assertEquals(0, b.skin);
		assertEquals(0, b.attribute);
		assertEquals(0, b.elapsedFrames);
		assertEquals(0f, b.darkness);
		assertEquals(1f, b.alpha);
		assertEquals(-1, b.pieceNum);
		assertEquals(0, b.item);
		assertEquals(0, b.hard);
		assertEquals(0, b.countdown);
		assertEquals(0, b.secondaryColor);
		assertEquals(0, b.bonusValue);
	}

	@Test
	void colorOnlyConstructorOnlyOverridesColor() {
		Block b = new Block(Block.BLOCK_COLOR_RED);

		assertEquals(Block.BLOCK_COLOR_RED, b.color);
		assertEquals(0, b.skin);
		assertEquals(0, b.attribute);
		assertEquals(1f, b.alpha);
	}

	@Test
	void colorAndSkinConstructorAssignsBoth() {
		Block b = new Block(Block.BLOCK_COLOR_BLUE, 3);

		assertEquals(Block.BLOCK_COLOR_BLUE, b.color);
		assertEquals(3, b.skin);
		assertEquals(0, b.attribute);
	}

	@Test
	void fullConstructorAssignsColorSkinAndAttribute() {
		int attr = Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_BONE;
		Block b = new Block(Block.BLOCK_COLOR_GREEN, 2, attr);

		assertEquals(Block.BLOCK_COLOR_GREEN, b.color);
		assertEquals(2, b.skin);
		assertEquals(attr, b.attribute);
	}

	@Test
	void copyConstructorMakesIndependentInstance() {
		Block src = new Block(Block.BLOCK_COLOR_PURPLE, 1, Block.BLOCK_ATTRIBUTE_VISIBLE);
		src.elapsedFrames = 7;
		src.darkness = 0.25f;
		src.alpha = 0.5f;
		src.pieceNum = 42;
		src.item = Block.BLOCK_ITEM_RANDOM;
		src.hard = 3;
		src.countdown = 4;
		src.secondaryColor = Block.BLOCK_COLOR_RED;
		src.bonusValue = 100;

		Block dst = new Block(src);

		assertNotSame(src, dst);
		assertEquals(src.color, dst.color);
		assertEquals(src.skin, dst.skin);
		assertEquals(src.attribute, dst.attribute);
		assertEquals(src.elapsedFrames, dst.elapsedFrames);
		assertEquals(src.darkness, dst.darkness);
		assertEquals(src.alpha, dst.alpha);
		assertEquals(src.pieceNum, dst.pieceNum);
		assertEquals(src.item, dst.item);
		assertEquals(src.hard, dst.hard);
		assertEquals(src.countdown, dst.countdown);
		assertEquals(src.secondaryColor, dst.secondaryColor);
		assertEquals(src.bonusValue, dst.bonusValue);

		src.color = Block.BLOCK_COLOR_NONE;
		assertEquals(Block.BLOCK_COLOR_PURPLE, dst.color);
	}

	@Test
	void getAttributeReportsBitMembership() {
		Block b = new Block();
		b.attribute = Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_OUTLINE;

		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE));
		assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_BONE));
	}

	@Test
	void setAttributeTogglesIndividualBits() {
		Block b = new Block();

		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_BONE, true);
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_BONE));

		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false);
		assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_BONE));
	}

	@Test
	void isEmptyTreatsBelowGrayAsEmpty() {
		assertTrue(new Block(Block.BLOCK_COLOR_NONE).isEmpty());
		assertTrue(new Block(Block.BLOCK_COLOR_INVALID).isEmpty());
		assertFalse(new Block(Block.BLOCK_COLOR_GRAY).isEmpty());
		assertFalse(new Block(Block.BLOCK_COLOR_PURPLE).isEmpty());
	}

	@Test
	void isGemBlockCoversGemRangeAndRainbow() {
		assertTrue(new Block(Block.BLOCK_COLOR_GEM_RED).isGemBlock());
		assertTrue(new Block(Block.BLOCK_COLOR_GEM_PURPLE).isGemBlock());
		assertTrue(new Block(Block.BLOCK_COLOR_GEM_RAINBOW).isGemBlock());
		assertFalse(new Block(Block.BLOCK_COLOR_RED).isGemBlock());
		assertFalse(new Block(Block.BLOCK_COLOR_RAINBOW).isGemBlock());
		assertFalse(new Block(Block.BLOCK_COLOR_SQUARE_GOLD_1).isGemBlock());
	}

	@Test
	void isGoldSquareBlockSpansGoldRangeOnly() {
		assertTrue(new Block(Block.BLOCK_COLOR_SQUARE_GOLD_1).isGoldSquareBlock());
		assertTrue(new Block(Block.BLOCK_COLOR_SQUARE_GOLD_9).isGoldSquareBlock());
		assertFalse(new Block(Block.BLOCK_COLOR_GEM_PURPLE).isGoldSquareBlock());
		assertFalse(new Block(Block.BLOCK_COLOR_SQUARE_SILVER_1).isGoldSquareBlock());
	}

	@Test
	void isSilverSquareBlockSpansSilverRangeOnly() {
		assertTrue(new Block(Block.BLOCK_COLOR_SQUARE_SILVER_1).isSilverSquareBlock());
		assertTrue(new Block(Block.BLOCK_COLOR_SQUARE_SILVER_9).isSilverSquareBlock());
		assertFalse(new Block(Block.BLOCK_COLOR_SQUARE_GOLD_9).isSilverSquareBlock());
		assertFalse(new Block(Block.BLOCK_COLOR_RAINBOW).isSilverSquareBlock());
	}

	@Test
	void isNormalBlockSpansGrayThroughPurple() {
		assertTrue(new Block(Block.BLOCK_COLOR_GRAY).isNormalBlock());
		assertTrue(new Block(Block.BLOCK_COLOR_PURPLE).isNormalBlock());
		assertFalse(new Block(Block.BLOCK_COLOR_NONE).isNormalBlock());
		assertFalse(new Block(Block.BLOCK_COLOR_GEM_RED).isNormalBlock());
	}

	@Test
	void getDrawColorMapsRainbowVariantsThroughPhase() {
		Block.rainbowPhase = 9;

		assertEquals(Block.BLOCK_COLOR_RED + 3, new Block(Block.BLOCK_COLOR_RAINBOW).getDrawColor());
		assertEquals(Block.BLOCK_COLOR_GEM_RED + 3, new Block(Block.BLOCK_COLOR_GEM_RAINBOW).getDrawColor());
		assertEquals(Block.BLOCK_COLOR_BLUE, new Block(Block.BLOCK_COLOR_BLUE).getDrawColor());
	}

	@Test
	void scriptedItemsKeepTheirBlockColorAndUseDistinctAnimatedDrawColors() {
		Block.rainbowPhase = 0;
		Block freeFall = new Block(Block.BLOCK_COLOR_BLUE);
		freeFall.item = Block.BLOCK_ITEM_FREE_FALL;
		Block delEven = new Block(Block.BLOCK_COLOR_BLUE);
		delEven.item = Block.BLOCK_ITEM_DEL_EVEN;

		assertEquals(3, Block.MAX_ITEM);
		assertEquals(Block.BLOCK_COLOR_BLUE, freeFall.color);
		assertEquals(Block.BLOCK_COLOR_BLUE, delEven.color);
		assertEquals(Block.BLOCK_COLOR_RED, freeFall.getDrawColor());
		assertEquals(Block.BLOCK_COLOR_GREEN, delEven.getDrawColor());
	}

	@Test
	void blockToCharEncodesColorsBelowAndAboveTen() {
		assertEquals('0', new Block(Block.BLOCK_COLOR_NONE).blockToChar());
		assertEquals('9', new Block(Block.BLOCK_COLOR_GEM_RED).blockToChar());
		assertEquals('A', new Block(Block.BLOCK_COLOR_GEM_ORANGE).blockToChar());
		assertEquals('Z', new Block(Block.BLOCK_COLOR_GEM_RAINBOW).blockToChar());
	}

	@Test
	void blockToCharClampsNegativeColorsToZero() {
		Block b = new Block(Block.BLOCK_COLOR_INVALID);
		assertEquals('0', b.blockToChar());
	}

	@Test
	void toStringReturnsSingleCharRepresentation() {
		assertEquals("0", new Block(Block.BLOCK_COLOR_NONE).toString());
		assertEquals("A", new Block(Block.BLOCK_COLOR_GEM_ORANGE).toString());
	}

	@Test
	void charToBlockColorDecodesRadix36AndAsciiTail() {
		assertEquals(0, Block.charToBlockColor('0'));
		assertEquals(9, Block.charToBlockColor('9'));
		assertEquals(10, Block.charToBlockColor('A'));
		assertEquals(35, Block.charToBlockColor('Z'));
		assertEquals(10, Block.charToBlockColor('a'));
		assertEquals(36, Block.charToBlockColor('['));
		assertEquals(37, Block.charToBlockColor('\\'));
	}

	@Test
	void updateRainbowPhaseWrapsModuloTwentyOne() {
		Block.updateRainbowPhase(0);
		assertEquals(0, Block.rainbowPhase);
		Block.updateRainbowPhase(20);
		assertEquals(20, Block.rainbowPhase);
		Block.updateRainbowPhase(21);
		assertEquals(0, Block.rainbowPhase);
		Block.updateRainbowPhase(43);
		assertEquals(1, Block.rainbowPhase);
	}

	@Test
	void updateRainbowPhaseFromNullEngineIncrementsAndWraps() {
		Block.rainbowPhase = 0;
		Block.updateRainbowPhase(null);
		assertEquals(1, Block.rainbowPhase);

		Block.rainbowPhase = 20;
		Block.updateRainbowPhase(null);
		assertEquals(0, Block.rainbowPhase);
	}

	@Test
	void updateRainbowPhaseFromActiveEngineSyncsToStatisticsTime() {
		// timerActive=true takes the engine.statistics.time path so the
		// rainbow stays in lockstep with the in-game clock instead of
		// drifting with frame-by-frame increments.
		nullpomino.game.play.GameManager gm =
				new nullpomino.game.play.GameManager(new nullpomino.game.event.EventReceiver());
		gm.init();
		nullpomino.game.play.GameEngine eng = gm.engine[0];
		eng.init();
		eng.timerActive = true;
		eng.statistics.time = 42;

		Block.rainbowPhase = 0;
		Block.updateRainbowPhase(eng);

		assertEquals(42 % 21, Block.rainbowPhase);
	}

	@Test
	void gemToNormalColorMapsGemRangeAndPassesOthersThrough() {
		assertEquals(Block.BLOCK_COLOR_RED, Block.gemToNormalColor(Block.BLOCK_COLOR_GEM_RED));
		assertEquals(Block.BLOCK_COLOR_PURPLE, Block.gemToNormalColor(Block.BLOCK_COLOR_GEM_PURPLE));
		assertEquals(Block.BLOCK_COLOR_RAINBOW, Block.gemToNormalColor(Block.BLOCK_COLOR_GEM_RAINBOW));
		assertEquals(Block.BLOCK_COLOR_BLUE, Block.gemToNormalColor(Block.BLOCK_COLOR_BLUE));
		assertEquals(Block.BLOCK_COLOR_NONE, Block.gemToNormalColor(Block.BLOCK_COLOR_NONE));
	}

	@Test
	void copyOverwritesEveryFieldOnReceiver() {
		Block src = new Block(Block.BLOCK_COLOR_CYAN, 5, Block.BLOCK_ATTRIBUTE_BONE);
		src.elapsedFrames = 11;
		src.darkness = -0.1f;
		src.alpha = 0.75f;
		src.pieceNum = 9;
		src.item = Block.BLOCK_ITEM_RANDOM;
		src.hard = 2;
		src.countdown = 3;
		src.secondaryColor = Block.BLOCK_COLOR_YELLOW;
		src.bonusValue = 250;

		Block dst = new Block();
		dst.copy(src);

		assertEquals(Block.BLOCK_COLOR_CYAN, dst.color);
		assertEquals(5, dst.skin);
		assertEquals(Block.BLOCK_ATTRIBUTE_BONE, dst.attribute);
		assertEquals(11, dst.elapsedFrames);
		assertEquals(-0.1f, dst.darkness);
		assertEquals(0.75f, dst.alpha);
		assertEquals(9, dst.pieceNum);
		assertEquals(Block.BLOCK_ITEM_RANDOM, dst.item);
		assertEquals(2, dst.hard);
		assertEquals(3, dst.countdown);
		assertEquals(Block.BLOCK_COLOR_YELLOW, dst.secondaryColor);
		assertEquals(250, dst.bonusValue);
	}
}
