package net.omegaboshi.nullpomino.game.subsystem.randomizer;

import nullpomino.game.component.Piece;

public abstract class DistanceWeightRandomizer extends Randomizer {

	int[] initWeights = {3, 3, 0, 0, 3, 3, 0, 2, 2, 2, 2};
	int[] weights;
	int[] cumulative;

	boolean firstPiece = true;

	public DistanceWeightRandomizer() {
		super();
	}

	public DistanceWeightRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	public void init() {
		weights = new int[pieces.length];
		for (int i = 0; i < pieces.length; i++) {
			weights[i] = initWeights[pieces[i]];
		}
		cumulative = new int[pieces.length];
		firstPiece = true;
	}

	public int next() {
		int sum = 0;
		for (int i = 0; i < pieces.length; i++) {
			sum += getWeight(i);
			cumulative[i] = sum;
		}
		int roll = randomIndex(sum);
		int selected = 0;
		for (int i = 0; i < pieces.length; i++) {
			if (roll < cumulative[i]) {
				selected = i;
				break;
			}
		}
		weights[selected] = 0;
		for (int i = 0; i < pieces.length; i++) {
			if (firstPiece && pieces[i] == Piece.PIECE_O) {
				weights[i] = 3;
			} else if (!isAtDistanceLimit(i)) {
				weights[i]++;
			}
		}
		firstPiece = false;
		return pieces[selected];
	}

	protected abstract int getWeight(int i);

	protected abstract boolean isAtDistanceLimit(int i);

}
