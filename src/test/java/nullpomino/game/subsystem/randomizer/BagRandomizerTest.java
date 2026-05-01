package nullpomino.game.subsystem.randomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.randomizer.BagMinusRandomizer;
import nullpomino.game.randomizer.BagMinusTwoRandomizer;
import nullpomino.game.randomizer.BagBonusBagRandomizer;
import nullpomino.game.randomizer.BagBonusRandomizer;
import nullpomino.game.randomizer.BagNoSZORandomizer;
import nullpomino.game.randomizer.BagRandomizer;
import nullpomino.game.randomizer.DoubleBagRandomizer;
import nullpomino.game.randomizer.GameBoyRandomizer;
import nullpomino.game.randomizer.History4RollsRandomizer;
import nullpomino.game.randomizer.History6RollsRandomizer;
import nullpomino.game.randomizer.LinearDistWeightRandomizer;
import nullpomino.game.randomizer.MemorylessRandomizer;
import nullpomino.game.randomizer.NineBagRandomizer;
import nullpomino.game.randomizer.NintendoRandomizer;
import nullpomino.game.randomizer.Randomizer;
import nullpomino.game.randomizer.StrictHistoryRandomizer;
import nullpomino.game.component.Piece;

import org.junit.jupiter.api.Test;

class BagRandomizerTest {

	@Test
	void regularBagDrawsEachEnabledPieceOncePerCycle() {
		assertCycleCounts(new BagRandomizer(), 1);
	}

	@Test
	void multipliedBagsDrawEachEnabledPiecePerConfiguredCopy() {
		assertCycleCounts(new DoubleBagRandomizer(), 2);
		assertCycleCounts(new NineBagRandomizer(), 9);
	}

	@Test
	void bonusBagsDrawEachEnabledPiecePlusOneDuplicatePerCycle() {
		assertBonusCycle(new BagBonusRandomizer());
		assertBonusCycle(new BagBonusBagRandomizer());
	}

	@Test
	void minusBagsWorkAfterDefaultConstructorAndSetState() {
		assertDrawsEnabledPieces(new BagMinusRandomizer(), Piece.PIECE_STANDARD_COUNT * 3);
		assertDrawsEnabledPieces(new BagMinusTwoRandomizer(), Piece.PIECE_STANDARD_COUNT * 3);
	}

	@Test
	void nonBagRandomizersDrawEnabledPieces() {
		assertDrawsEnabledPieces(new MemorylessRandomizer(), Piece.PIECE_STANDARD_COUNT * 3);
		assertDrawsEnabledPieces(new NintendoRandomizer(), Piece.PIECE_STANDARD_COUNT * 3);
		assertDrawsEnabledPieces(new GameBoyRandomizer(), Piece.PIECE_STANDARD_COUNT * 3);
	}

	@Test
	void noSZOBagsAndLimitedHistoryAvoidSZOOnFirstPieceWhenPossible() {
		assertFirstPieceNotSZO(new BagNoSZORandomizer());
		assertFirstPieceNotSZO(new History4RollsRandomizer());
		assertFirstPieceNotSZO(new History6RollsRandomizer());
	}

	@Test
	void strictHistoryWorksWithNonContiguousEnabledPieces() {
		StrictHistoryRandomizer randomizer = new StrictHistoryRandomizer();
		boolean[] enabled = new boolean[Piece.PIECE_COUNT];
		enabled[Piece.PIECE_I] = true;
		enabled[Piece.PIECE_O] = true;
		enabled[Piece.PIECE_T] = true;
		enabled[Piece.PIECE_L3] = true;
		randomizer.setState(enabled, 1357L);

		for(int i = 0; i < 50; i++) {
			int piece = randomizer.next();
			assertTrue(enabled[piece], "unexpected disabled piece " + piece);
		}
	}

	@Test
	void setStateFullyResetsDistanceWeightRandomizers() {
		boolean[] enabled = standardPieces();
		LinearDistWeightRandomizer fresh = new LinearDistWeightRandomizer();
		fresh.setState(enabled, 9876L);

		LinearDistWeightRandomizer reused = new LinearDistWeightRandomizer();
		reused.setState(enabled, 2468L);
		draw(reused, Piece.PIECE_STANDARD_COUNT);
		reused.setState(enabled, 9876L);

		assertArrayEquals(draw(fresh, 20), draw(reused, 20));
	}

	@Test
	void setPieceEnableCompactsEnabledPiecesInPieceIdOrder() {
		BagRandomizer randomizer = new BagRandomizer();
		boolean[] enabled = new boolean[Piece.PIECE_COUNT];
		enabled[Piece.PIECE_O] = true;
		enabled[Piece.PIECE_T] = true;
		enabled[Piece.PIECE_L3] = true;

		randomizer.setPieceEnable(enabled);

		assertArrayEquals(new int[] {
				Piece.PIECE_O,
				Piece.PIECE_T,
				Piece.PIECE_L3
		}, randomizer.pieces);
	}

	private static void assertCycleCounts(Randomizer randomizer, int expectedCopies) {
		boolean[] enabled = standardPieces();
		randomizer.setState(enabled, 1234L);

		int[] counts = new int[Piece.PIECE_COUNT];
		for(int i = 0; i < Piece.PIECE_STANDARD_COUNT * expectedCopies; i++) {
			int piece = randomizer.next();
			assertTrue(enabled[piece], "unexpected disabled piece " + piece);
			counts[piece]++;
		}

		for(int piece = 0; piece < Piece.PIECE_STANDARD_COUNT; piece++) {
			assertEquals(expectedCopies, counts[piece],
					"piece " + Piece.PIECE_NAMES[piece] + " count");
		}
	}

	private static void assertBonusCycle(Randomizer randomizer) {
		boolean[] enabled = standardPieces();
		randomizer.setState(enabled, 4321L);

		int[] counts = new int[Piece.PIECE_COUNT];
		for(int i = 0; i < Piece.PIECE_STANDARD_COUNT + 1; i++) {
			int piece = randomizer.next();
			assertTrue(enabled[piece], "unexpected disabled piece " + piece);
			counts[piece]++;
		}

		int duplicatedPieces = 0;
		for(int piece = 0; piece < Piece.PIECE_STANDARD_COUNT; piece++) {
			assertTrue(counts[piece] >= 1,
					"piece " + Piece.PIECE_NAMES[piece] + " must appear at least once");
			if(counts[piece] == 2) duplicatedPieces++;
			else assertEquals(1, counts[piece],
					"piece " + Piece.PIECE_NAMES[piece] + " count");
		}
		assertEquals(1, duplicatedPieces);
	}

	private static void assertDrawsEnabledPieces(Randomizer randomizer, int draws) {
		boolean[] enabled = standardPieces();
		randomizer.setState(enabled, 5678L);

		for(int i = 0; i < draws; i++) {
			int piece = randomizer.next();
			assertTrue(enabled[piece], "unexpected disabled piece " + piece);
		}
	}

	private static void assertFirstPieceNotSZO(Randomizer randomizer) {
		randomizer.setState(standardPieces(), 1122L);
		assertFalse(isSZO(randomizer.next()));
	}

	private static int[] draw(Randomizer randomizer, int draws) {
		int[] pieces = new int[draws];
		for(int i = 0; i < pieces.length; i++) {
			pieces[i] = randomizer.next();
		}
		return pieces;
	}

	private static boolean isSZO(int piece) {
		return piece == Piece.PIECE_S || piece == Piece.PIECE_Z || piece == Piece.PIECE_O;
	}

	private static boolean[] standardPieces() {
		boolean[] enabled = new boolean[Piece.PIECE_COUNT];
		for(int i = 0; i < Piece.PIECE_STANDARD_COUNT; i++) {
			enabled[i] = true;
		}
		return enabled;
	}
}
