package nullpomino.game.randomizer;

public class DoubleBagRandomizer extends BagRandomizer {

	@Override
	protected int bagSize() {
		return pieces.length * 2;
	}
}
