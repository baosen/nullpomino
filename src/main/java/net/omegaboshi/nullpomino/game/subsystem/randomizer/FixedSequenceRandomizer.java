package net.omegaboshi.nullpomino.game.subsystem.randomizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import nullpomino.game.component.Piece;

public class FixedSequenceRandomizer extends Randomizer {
	private static final Path DEFAULT_SEQUENCE_FILE = Paths.get("sequence.txt");
	private static final String DEFAULT_SEQUENCE = "I";

	private final Path sequenceFile;
	private int[] sequenceTranslated;
	private int id = -1;
	
	public FixedSequenceRandomizer() {
		this(DEFAULT_SEQUENCE_FILE);
	}

	public FixedSequenceRandomizer(boolean[] pieceEnable, long seed) {
		this(DEFAULT_SEQUENCE_FILE);
		setState(pieceEnable, seed);
	}

	public FixedSequenceRandomizer(Path sequenceFile) {
		this.sequenceFile = sequenceFile;
	}

	@Override
	public void init() {
		sequenceTranslated = translateSequence(readSequence());
		id = -1;
	}

	private String readSequence() {
		StringBuilder sequence = new StringBuilder();

		try {
			for(String line : Files.readAllLines(sequenceFile, StandardCharsets.UTF_8)) {
				sequence.append(line);
			}
		} catch (IOException e) {
			return DEFAULT_SEQUENCE;
		}

		return (sequence.length() == 0) ? DEFAULT_SEQUENCE : sequence.toString();
	}

	private int[] translateSequence(String sequence) {
		int[] translated = new int[sequence.length()];
		for(int i = 0; i < translated.length; i++) {
			translated[i] = pieceCharToId(sequence.charAt(i));
		}
		return translated;
	}
	
	private int pieceCharToId(char c) {
		for (int i = 0; i < Piece.PIECE_STANDARD_COUNT; i++) {
			if (c == Piece.PIECE_NAMES[i].charAt(0)) return i;
		}
		return Piece.PIECE_STANDARD_COUNT;
	}

	@Override
	public int next() {
		id++;
		return sequenceTranslated[id % sequenceTranslated.length];
	}
}
