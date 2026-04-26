package net.omegaboshi.nullpomino.game.subsystem.randomizer;

public class DoubleBagRandomizer extends BagRandomizer {

	public DoubleBagRandomizer() {
		super();
	}

	public DoubleBagRandomizer(boolean[] pieceEnable, long seed) {
		super(pieceEnable, seed);
	}

	@Override
	protected int bagSize() {
		return pieces.length * 2;
	}
}
