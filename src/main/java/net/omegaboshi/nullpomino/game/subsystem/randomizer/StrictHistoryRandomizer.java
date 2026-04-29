package net.omegaboshi.nullpomino.game.subsystem.randomizer;

import nullpomino.game.component.Piece;

public class StrictHistoryRandomizer extends Randomizer {

	int[] history;

	boolean[] curHist;
	int[] notHist;
	int histLen;

	public StrictHistoryRandomizer() {
		super();
	}

	public StrictHistoryRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	public void init() {
		history = new int[] {
				indexOfPiece(Piece.PIECE_S),
				indexOfPiece(Piece.PIECE_Z),
				indexOfPiece(Piece.PIECE_O),
				indexOfPiece(Piece.PIECE_O)
		};
		curHist = new boolean[pieces.length];
		notHist = new int[pieces.length];
		histLen = Math.min(4, pieces.length - 1);
	}

	public int next() {
		for (int i = 0; i < pieces.length; i++) {
			curHist[i] = false;
		}
		for (int i = 0; i < histLen; i++) {
			if (history[i] >= 0) {
				curHist[history[i]] = true;
			}
		}
		int notHistPos = 0;
		for (int i = 0; i < pieces.length; i++) {
			if (!curHist[i]) {
				notHist[notHistPos] = i;
				notHistPos++;
			}
		}
		int id = notHist[randomIndex(notHistPos)];
		shiftRight(history, histLen);
		history[0] = id;
		return pieces[id];
	}
}
