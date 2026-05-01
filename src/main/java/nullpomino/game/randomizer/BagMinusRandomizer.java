package nullpomino.game.randomizer;

public class BagMinusRandomizer extends BagRandomizer {

	@Override
	protected int dealLength() {
		return Math.max(1, pieces.length - 1);
	}
}
