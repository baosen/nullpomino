package nullpomino.game.subsystem.mode.menu;

import nullpomino.util.CustomProperties;

public class IntegerMenuItem extends AbstractMenuItem<Integer> {
	public int min, max;

	public IntegerMenuItem(String name, String displayName, int color,
			int defaultValue, int min, int max) {
		super(name, displayName, color, defaultValue);
		this.min = min;
		this.max = max;
	}

	@Override
	public void change(int dir, int fast) {
		value += dir;
		if (value < min)
			value = max;
		if (value > max)
			value = min;
	}

	@Override
	public void save(int playerID, CustomProperties prop, String modeName) {
		saveValue(playerID, prop, modeName, value);
	}

	@Override
	public void load(int playerID, CustomProperties prop, String modeName) {
		value = loadInt(playerID, prop, modeName);
	}

	@Override
	public String getValueString() {
		return String.valueOf(value);
	}
}
