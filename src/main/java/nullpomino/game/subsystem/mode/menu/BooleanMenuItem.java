package nullpomino.game.subsystem.mode.menu;

import nullpomino.util.CustomProperties;

public class BooleanMenuItem extends AbstractMenuItem<Boolean> {
	public BooleanMenuItem(String name, String displayName, int color,
			boolean defaultValue) {
		super(name, displayName, color, defaultValue);
	}

	@Override
	public void change(int dir, int fast) {
		value = !value;
	}

	@Override
	public String getValueString() {
		return String.valueOf(value).toUpperCase();
	}

	@Override
	protected void saveValue(CustomProperties prop, String key, Boolean value) {
		prop.setProperty(key, value);
	}

	@Override
	protected Boolean loadValue(CustomProperties prop, String key) {
		return prop.getProperty(key, DEFAULT_VALUE);
	}
}
