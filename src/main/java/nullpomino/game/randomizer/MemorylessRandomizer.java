package nullpomino.game.randomizer;

public class MemorylessRandomizer extends Randomizer {

	public MemorylessRandomizer() {
		super();
	}

	public MemorylessRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	public int next() {
		return randomPiece();
	}

}
