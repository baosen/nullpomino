package nullpomino.game.randomizer;

public class NineBagRandomizer extends BagRandomizer {

	public NineBagRandomizer() {
		super();
	}

	public NineBagRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	@Override
	protected int bagSize() {
		return pieces.length * 9;
	}
}
