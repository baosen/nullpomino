package net.omegaboshi.nullpomino.game.subsystem.randomizer;

import java.util.Arrays;
import java.util.Random;

import nullpomino.game.component.Piece;

public abstract class Randomizer {

	protected Random r;
	public int[] pieces;

	public Randomizer() {}

	public Randomizer(boolean[] pieceEnable, long seed) {
		setState(pieceEnable, seed);
	}

	public void init() {}

	public abstract int next();

	public void setState(boolean[] pieceEnable, long seed) {
		setPieceEnable(pieceEnable);
		reseed(seed);
		init();
	}

	public void setPieceEnable(boolean[] pieceEnable) {
		int[] enabledPieces = new int[Piece.PIECE_COUNT];
		int count = 0;
		for (int i = 0; i < Piece.PIECE_COUNT; i++) {
			if(pieceEnable[i]) enabledPieces[count++] = i;
		}
		pieces = Arrays.copyOf(enabledPieces, count);
	}

	public void reseed(long seed) {
		r = new Random(seed);
	}

	protected boolean isPieceSZOOnly()
	{
		for (int i=0; i<pieces.length; i++) {
			if (pieces[i] != Piece.PIECE_O && pieces[i] != Piece.PIECE_Z && pieces[i] != Piece.PIECE_S)
				return false;
		}

		return true;
	}
}
