package nullpomino.game.randomizer;

public class NineBagRandomizer extends BagRandomizer {

	@Override
	protected int bagSize() {
		return pieces.length * 9;
	}
}
