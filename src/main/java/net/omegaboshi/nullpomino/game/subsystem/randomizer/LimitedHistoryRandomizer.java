package net.omegaboshi.nullpomino.game.subsystem.randomizer;

public abstract class LimitedHistoryRandomizer extends Randomizer {

	int[] history;
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
		int selected = 0;
		if (firstPiece && !isPieceSZOOnly()) {
			do {
				selected = randomPieceIndex();
			} while (isSZOPiece(pieces[selected]));
			firstPiece = false;
		} else {
			for (int i = 0; i < numrolls; i++) {
				selected = randomPieceIndex();
				if (!contains(history, pieces[selected])) {
					break;
				}
			}
		}
		shiftRight(history, history.length);
		history[0] = pieces[selected];
		return pieces[selected];
	}
}
