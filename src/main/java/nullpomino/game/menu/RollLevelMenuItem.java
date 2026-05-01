package nullpomino.game.menu;

public class RollLevelMenuItem extends LevelMenuItem {
	public RollLevelMenuItem(String name, String displayName, int color,
			int defaultValue, int min, int max) {
		super(name, displayName, color, defaultValue, min, max);
	}

	@Override
	public String getValueString() {
		if(value == 10) return "ROLL";
		if(value == 11) return "M-ROLL";
		return super.getValueString();
	}
}
