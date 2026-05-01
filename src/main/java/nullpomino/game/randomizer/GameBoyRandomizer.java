package nullpomino.game.randomizer;

public class GameBoyRandomizer extends Randomizer {

	int id;
	int roll;

	public void init() {
		id = randomPieceIndex();
		roll = 6 * pieces.length - 3;
	}

	public int next() {
		id = (id + (randomIndex(roll) / 5) + 1) % pieces.length;
		return pieces[id];
	}

}
