package nullpomino.game.menu;

import nullpomino.util.GeneralUtil;

public class OnOffMenuItem extends BooleanMenuItem {
	public OnOffMenuItem(String name, String displayName, int color,
			boolean defaultValue) {
		super(name, displayName, color, defaultValue);
	}

	@Override
	public String getValueString() {
		return GeneralUtil.getONorOFF(value);
	}
}
