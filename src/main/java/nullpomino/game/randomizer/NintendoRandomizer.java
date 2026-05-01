package nullpomino.game.randomizer;

public class NintendoRandomizer extends Randomizer {

	int prev;
	int roll;

	public NintendoRandomizer() {
		super();
	}

	public NintendoRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	public void init() {
		prev = pieces.length;
		roll = pieces.length+1;
	}

	public int next() {
		int id = randomIndex(roll);
		if (id == prev || id == pieces.length) {
			id = randomPieceIndex();
		}
		prev = id;
		return pieces[id];
	}

}
