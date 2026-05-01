package nullpomino.game.randomizer;

import nullpomino.game.component.Piece;

public class History6RollsRandomizer extends LimitedHistoryRandomizer {

	public void init() {
		super.init();
		history = new int[] {Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_S, Piece.PIECE_Z};
		numrolls = 6;
	}
}
