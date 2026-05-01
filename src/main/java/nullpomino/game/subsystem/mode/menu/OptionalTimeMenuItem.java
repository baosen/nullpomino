package nullpomino.game.subsystem.mode.menu;

public class OptionalTimeMenuItem extends TimeMenuItem {
	private final String disabledLabel;

	public OptionalTimeMenuItem(String name, String displayName, int color,
			int defaultValue, int min, int max, String disabledLabel) {
		super(name, displayName, color, defaultValue, min, max);
		this.disabledLabel = disabledLabel;
	}

	@Override
	public String getValueString() {
		return (value == 0) ? disabledLabel : super.getValueString();
	}
}
