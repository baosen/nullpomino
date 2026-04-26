package nullpomino.game.subsystem.mode.menu;

import nullpomino.util.CustomProperties;

public abstract class AbstractMenuItem<T> {
	public final String name;
	public final String displayName;
	public final int color;
	public final T DEFAULT_VALUE;
	public T value;

	public AbstractMenuItem(String name, String displayName, int color,
			T defaultValue) {
		this.name = name;
		this.displayName = displayName;
		this.color = color;
		DEFAULT_VALUE = defaultValue;
		value = defaultValue;
	}

	public abstract String getValueString();

	/**
	 * Change the attribute.
	 *
	 * @param dir
	 *            Direction pressed: -1 = left, 1 = right.
	 *            If 0, update without changing any settings.
	 * @param fast
	 *            0 by default, +1 if E held, +2 if F held.
	 */
	public abstract void change(int dir, int fast);

	public abstract void save(int playerID, CustomProperties prop,
			String modeName);

	public abstract void load(int playerID, CustomProperties prop,
			String modeName);

	protected final String propertyKey(int playerID, String modeName) {
		return modeName + "." + name + playerSuffix(playerID);
	}

	protected final void saveValue(int playerID, CustomProperties prop,
			String modeName, int value) {
		prop.setProperty(propertyKey(playerID, modeName), value);
	}

	protected final void saveValue(int playerID, CustomProperties prop,
			String modeName, boolean value) {
		prop.setProperty(propertyKey(playerID, modeName), value);
	}

	protected final int loadInt(int playerID, CustomProperties prop,
			String modeName) {
		return prop.getProperty(propertyKey(playerID, modeName), (Integer) DEFAULT_VALUE);
	}

	protected final boolean loadBoolean(int playerID, CustomProperties prop,
			String modeName) {
		return prop.getProperty(propertyKey(playerID, modeName), (Boolean) DEFAULT_VALUE);
	}

	private static String playerSuffix(int playerID) {
		return playerID < 0 ? "" : ".p" + playerID;
	}
}
