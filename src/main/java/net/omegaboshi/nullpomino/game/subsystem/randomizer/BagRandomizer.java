package net.omegaboshi.nullpomino.game.subsystem.randomizer;

public class BagRandomizer extends Randomizer {

	int[] bag;
	int pt;
	int dealLength;

	public BagRandomizer() {
		super();
	}

	public BagRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	public void init() {
		bag = new int[bagSize()];
		pt = 0;
		dealLength = dealLength();
		fillBag(bag);
		shuffle();
	}

	public void shuffle() {
		shuffle(bag);
	}

	public int next() {
		int id = bag[pt];
		pt++;
		if (pt == dealLength) {
			pt = 0;
			shuffle();
		}
		return id;
	}

	protected int bagSize() {
		return pieces.length;
	}

	protected int dealLength() {
		return bag.length;
	}
}
