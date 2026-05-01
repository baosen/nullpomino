package nullpomino.game.randomizer;

public class BagNoSZORandomizer extends BagRandomizer {

	boolean firstBag;

	public void init() {
		firstBag = true;
		super.init();
	}

	public void shuffle() {
		if (firstBag && !isPieceSZOOnly()) {
			do {
				super.shuffle();
			} while (isSZOPiece(bag[0]));
			firstBag = false;
		} else {
			super.shuffle();
		}
	}

}
