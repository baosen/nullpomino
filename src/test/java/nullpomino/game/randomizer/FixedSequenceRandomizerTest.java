package nullpomino.game.randomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import nullpomino.game.component.Piece;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FixedSequenceRandomizerTest {

	@TempDir
	Path tempDir;

	@Test
	void readsSequenceTxtAndCyclesThroughPieces() throws Exception {
		Path sequenceFile = tempDir.resolve("sequence.txt");
		Files.write(sequenceFile, "IL\nO".getBytes(StandardCharsets.UTF_8));

		FixedSequenceRandomizer randomizer = new FixedSequenceRandomizer(sequenceFile);
		randomizer.init();

		assertEquals(Piece.PIECE_I, randomizer.next());
		assertEquals(Piece.PIECE_L, randomizer.next());
		assertEquals(Piece.PIECE_O, randomizer.next());
		assertEquals(Piece.PIECE_I, randomizer.next());
	}

	@Test
	void missingSequenceFileFallsBackToI() {
		FixedSequenceRandomizer randomizer = new FixedSequenceRandomizer(tempDir.resolve("missing.txt"));
		randomizer.init();

		assertEquals(Piece.PIECE_I, randomizer.next());
		assertEquals(Piece.PIECE_I, randomizer.next());
	}

	@Test
	void initResetsCursorToStartOfSequence() throws Exception {
		Path sequenceFile = tempDir.resolve("sequence.txt");
		Files.write(sequenceFile, "LO".getBytes(StandardCharsets.UTF_8));
		FixedSequenceRandomizer randomizer = new FixedSequenceRandomizer(sequenceFile);
		randomizer.init();
		randomizer.next();

		randomizer.init();

		assertEquals(Piece.PIECE_L, randomizer.next());
	}
}
