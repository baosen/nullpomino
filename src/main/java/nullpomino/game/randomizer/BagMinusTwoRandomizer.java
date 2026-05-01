package nullpomino.game.randomizer;

public class BagMinusTwoRandomizer extends BagRandomizer {

	@Override
	protected int dealLength() {
		return Math.max(1, pieces.length - 2);
	}
}
