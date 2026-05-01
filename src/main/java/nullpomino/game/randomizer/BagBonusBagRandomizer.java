package nullpomino.game.randomizer;

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
		fillBag(bonusbag);
		shuffleBonus();
		shuffle();
	}

	public void shuffle() {
		shuffleWithBonus(bonusbag[bonuspt]);
	}

	public void shuffleBonus() {
		shuffle(bonusbag);
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
