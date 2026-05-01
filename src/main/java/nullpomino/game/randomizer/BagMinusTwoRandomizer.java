package nullpomino.game.randomizer;

public class BagMinusTwoRandomizer extends BagRandomizer {

	public BagMinusTwoRandomizer() {
		super();
	}

	public BagMinusTwoRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	@Override
	protected int dealLength() {
		return Math.max(1, pieces.length - 2);
	}
}
