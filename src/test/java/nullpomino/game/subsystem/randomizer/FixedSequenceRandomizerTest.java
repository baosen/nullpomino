package nullpomino.game.subsystem.randomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import net.omegaboshi.nullpomino.game.subsystem.randomizer.FixedSequenceRandomizer;
import nullpomino.game.component.Piece;

import org.junit.jupiter.api.Test;

class FixedSequenceRandomizerTest {

	@Test
	void readsSequenceTxtAndCyclesThroughPieces() throws Exception {
		Path sequenceFile = Paths.get("sequence.txt");
		Files.deleteIfExists(sequenceFile);
		Files.write(sequenceFile, "IL\nO".getBytes(StandardCharsets.UTF_8));

		try {
			FixedSequenceRandomizer randomizer = new FixedSequenceRandomizer();
			randomizer.init();

			assertEquals(Piece.PIECE_I, randomizer.next());
			assertEquals(Piece.PIECE_L, randomizer.next());
			assertEquals(Piece.PIECE_O, randomizer.next());
			assertEquals(Piece.PIECE_I, randomizer.next());
		} finally {
			Files.deleteIfExists(sequenceFile);
		}
	}
}
