package nullpomino.game.menu;

import nullpomino.util.GeneralUtil;

public class TimeMenuItem extends IntegerMenuItem {
	public int increment;

	public TimeMenuItem(String name, String displayName, int color,
			int defaultValue, int min, int max) {
		super(name, displayName, color, defaultValue, min, max);
		increment = 60;
	}

	public TimeMenuItem(String name, String displayName, int color,
			int defaultValue, int min, int max, int increment) {
		super(name, displayName, color, defaultValue, min, max);
		this.increment = increment;
	}

	@Override
	public void change(int dir, int fast) {
		changeBy(dir * increment);
	}

	@Override
	public String getValueString() {
		return GeneralUtil.getTime(value);
	}
}
