package nullpomino.game.subsystem.mode.menu;

public class LevelMenuItem extends IntegerMenuItem {
	public LevelMenuItem(String name, String displayName, int color,
			int defaultValue, int min, int max) {
		super(name, displayName, color, defaultValue, min, max);
	}

	@Override
	public String getValueString() {
		return String.valueOf(value * 100);
	}
}
