package net.omegaboshi.nullpomino.game.subsystem.randomizer;

public class BagBonusBagRandomizer extends BagBonusRandomizer {

	int[] bonusbag;
	int bonuspt;

	public BagBonusBagRandomizer() {
		super();
	}

	public BagBonusBagRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	public void init() {
		initBag();
		bonusbag = new int[pieces.length];
		bonuspt = 0;
		for (int i = 0; i < pieces.length; i++) {
			bonusbag[i] = pieces[i];
		}
		shuffleBonus();
		shuffle();
	}

	public void shuffle() {
		shuffleWithBonus(bonusbag[bonuspt]);
	}

	public void shuffleBonus() {
		for (int i = pieces.length; i > 1; i--) {
			int j = r.nextInt(i);
			int temp = bonusbag[i-1];
			bonusbag[i-1] = bonusbag[j];
			bonusbag[j] = temp;
		}
	}

	public int next() {
		int id = bag[pt];
		pt++;
		if (pt == baglen) {
			pt = 0;
			bonuspt++;
			if (bonuspt == pieces.length) {
				bonuspt = 0;
				shuffleBonus();
			}
			shuffle();
		}
		return id;
	}
}
