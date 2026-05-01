package nullpomino.game.randomizer;

public class QuadraticDistWeightRandomizer extends DistanceWeightRandomizer {

	public int getWeight(int i) {
		return weights[i]*weights[i];
	}

	public boolean isAtDistanceLimit(int i) {
		return false;
	}

}
