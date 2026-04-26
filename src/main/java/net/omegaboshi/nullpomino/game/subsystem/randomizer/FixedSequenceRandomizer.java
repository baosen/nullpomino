package net.omegaboshi.nullpomino.game.subsystem.randomizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import nullpomino.game.component.Piece;

public class FixedSequenceRandomizer extends Randomizer {
	private static final File SEQUENCE_FILE = new File("sequence.txt");

	private int[] sequenceTranslated;
	private int id = -1;
	
	public FixedSequenceRandomizer() {
		super();
	}

	public FixedSequenceRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	@Override
	public void init() {
		String sequence = readSequence();
		sequenceTranslated = new int[sequence.length()];
		for (int i = 0; i < sequenceTranslated.length; i++) {
			sequenceTranslated[i] = pieceCharToId(sequence.charAt(i));
		}
	}

	private String readSequence() {
		StringBuilder sequence = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new FileReader(SEQUENCE_FILE))) {
			String text;
			while ((text = reader.readLine()) != null) {
				sequence.append(text);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sequence.toString();
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
