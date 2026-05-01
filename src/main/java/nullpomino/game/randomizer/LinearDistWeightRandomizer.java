package nullpomino.game.randomizer;

public class LinearDistWeightRandomizer extends DistanceWeightRandomizer {

	public int getWeight(int i) {
		return weights[i];
	}

	public boolean isAtDistanceLimit(int i) {
		return false;
	}

}
