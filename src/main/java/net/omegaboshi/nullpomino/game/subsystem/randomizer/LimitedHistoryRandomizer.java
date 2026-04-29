package net.omegaboshi.nullpomino.game.subsystem.randomizer;

public abstract class LimitedHistoryRandomizer extends Randomizer {

	int[] history;
	int id;
	int numrolls;

	boolean firstPiece;

	public LimitedHistoryRandomizer() {
		super();
	}

	public LimitedHistoryRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);

	}

	public void init() {
		firstPiece = true;
	}

	public int next() {
		if (firstPiece && !isPieceSZOOnly()) {
			do {
				id = randomPieceIndex();
			} while (isSZOPiece(pieces[id]));
			firstPiece = false;
		} else {
			for (int i = 0; i < numrolls; i++) {
				id = randomPieceIndex();
				if (!contains(history, pieces[id])) {
					break;
				}
			}
		}
		shiftRight(history, history.length);
		history[0] = pieces[id];
		return pieces[id];
	}
}
