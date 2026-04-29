package net.omegaboshi.nullpomino.game.subsystem.randomizer;

public class BagNoSZORandomizer extends BagRandomizer {

	boolean firstBag;

	public BagNoSZORandomizer() {
		super();
	}
	
	public void init() {
		firstBag = true;
		super.init();
	}

	public BagNoSZORandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
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
