package nullpomino.game.randomizer;

import java.util.Arrays;
import java.util.Random;

import nullpomino.game.component.Piece;

public abstract class Randomizer {

	protected Random r;
	public int[] pieces;

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

	protected final void fillBag(int[] bag) {
		for (int i = 0; i < bag.length; i++) {
			bag[i] = pieces[i % pieces.length];
		}
	}

	protected final void shuffle(int[] values) {
		for (int i = values.length; i > 1; i--) {
			int j = r.nextInt(i);
			swap(values, i - 1, j);
		}
	}

	protected final int randomPiece() {
		return pieces[randomPieceIndex()];
	}

	protected final int randomPieceIndex() {
		return randomIndex(pieces.length);
	}

	protected final int randomIndex(int bound) {
		return r.nextInt(bound);
	}

	protected final boolean contains(int[] values, int target) {
		for (int value : values) {
			if (value == target) return true;
		}
		return false;
	}

	protected final void shiftRight(int[] values, int length) {
		for (int i = length - 1; i > 0; i--) {
			values[i] = values[i - 1];
		}
	}

	protected final boolean isPieceSZOOnly() {
		for (int piece : pieces) {
			if (!isSZOPiece(piece))
				return false;
		}

		return true;
	}

	protected final boolean isSZOPiece(int piece) {
		return piece == Piece.PIECE_O || piece == Piece.PIECE_Z || piece == Piece.PIECE_S;
	}

	protected final int indexOfPiece(int piece) {
		for (int i = 0; i < pieces.length; i++) {
			if (pieces[i] == piece) return i;
		}
		return -1;
	}

	private static void swap(int[] values, int first, int second) {
		int temp = values[first];
		values[first] = values[second];
		values[second] = temp;
	}
}
