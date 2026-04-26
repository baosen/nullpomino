package nullpomino.game.subsystem.randomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagMinusRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagMinusTwoRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.BagRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.DoubleBagRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.NineBagRandomizer;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer;
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
	void minusBagsWorkAfterDefaultConstructorAndSetState() {
		assertDrawsEnabledPieces(new BagMinusRandomizer(), Piece.PIECE_STANDARD_COUNT * 3);
		assertDrawsEnabledPieces(new BagMinusTwoRandomizer(), Piece.PIECE_STANDARD_COUNT * 3);
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

	private static void assertDrawsEnabledPieces(Randomizer randomizer, int draws) {
		boolean[] enabled = standardPieces();
		randomizer.setState(enabled, 5678L);

		for(int i = 0; i < draws; i++) {
			int piece = randomizer.next();
			assertTrue(enabled[piece], "unexpected disabled piece " + piece);
		}
	}

	private static boolean[] standardPieces() {
		boolean[] enabled = new boolean[Piece.PIECE_COUNT];
		for(int i = 0; i < Piece.PIECE_STANDARD_COUNT; i++) {
			enabled[i] = true;
		}
		return enabled;
	}
}
