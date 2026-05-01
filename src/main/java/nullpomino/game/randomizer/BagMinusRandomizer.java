package nullpomino.game.randomizer;

public class BagMinusRandomizer extends BagRandomizer {

	public BagMinusRandomizer() {
		super();
	}

	public BagMinusRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	@Override
	protected int dealLength() {
		return Math.max(1, pieces.length - 1);
	}
}
