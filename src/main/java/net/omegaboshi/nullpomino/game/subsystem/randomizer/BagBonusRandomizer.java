package net.omegaboshi.nullpomino.game.subsystem.randomizer;

public class BagBonusRandomizer extends Randomizer {

	int[] bag;
	int baglen;
	int pt;
	int bonus;

	public BagBonusRandomizer() {
		super();
	}

	public BagBonusRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	public void init() {
		initBag();
		shuffle();
	}

	protected void initBag() {
		baglen = pieces.length+1;
		bag = new int[baglen];
		pt = 0;
		bonus = pieces.length;
		for (int i = 0; i < pieces.length; i++) {
			bag[i] = pieces[i];
		}
	}

	public void shuffle() {
		shuffleWithBonus(pieces[r.nextInt(pieces.length)]);
	}

	protected void shuffleWithBonus(int bonusPiece) {
		bag[bonus] = bonusPiece;
		for (int i = baglen; i > 1; i--) {
			int j = r.nextInt(i);
			int temp = bag[i-1];
			bag[i-1] = bag[j];
			bag[j] = temp;
			updateBonusPosition(i - 1, j);
		}
	}

	public int next() {
		int id = bag[pt];
		pt++;
		if (pt == baglen) {
			pt = 0;
			shuffle();
		}
		return id;
	}

	private void updateBonusPosition(int first, int second) {
		if (bonus == first) {
			bonus = second;
		} else if(bonus == second) {
			bonus = first;
		}
	}
}
