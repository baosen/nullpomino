package nullpomino.game.randomizer;

import nullpomino.game.component.Piece;

public class History4RollsRandomizer extends LimitedHistoryRandomizer {

	public void init() {
		super.init();
		history = new int[] {Piece.PIECE_Z, Piece.PIECE_Z, Piece.PIECE_Z, Piece.PIECE_Z};
		numrolls = 4;
	}
}
