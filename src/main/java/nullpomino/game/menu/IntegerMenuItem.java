package nullpomino.game.menu;

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
		changeBy(dir);
	}

	protected final void changeBy(int delta) {
		value += delta;
		if (value < min)
			value = max;
		if (value > max)
			value = min;
	}

	@Override
	protected void saveValue(CustomProperties prop, String key, Integer value) {
		prop.setProperty(key, value);
	}

	@Override
	protected Integer loadValue(CustomProperties prop, String key) {
		return prop.getProperty(key, DEFAULT_VALUE);
	}

	@Override
	public String getValueString() {
		return String.valueOf(value);
	}
}
