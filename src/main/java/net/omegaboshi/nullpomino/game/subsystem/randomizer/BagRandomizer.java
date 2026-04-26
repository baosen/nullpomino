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
		for (int i = 0; i < bag.length; i++) {
			bag[i] = pieces[i % pieces.length];
		}
		shuffle();
	}

	public void shuffle() {
		for (int i = bag.length; i > 1; i--) {
			int j = r.nextInt(i);
			int temp = bag[i-1];
			bag[i-1] = bag[j];
			bag[j] = temp;
		}
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
