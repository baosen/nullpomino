package nullpomino.game.menu;

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

	public final void save(int playerID, CustomProperties prop,
			String modeName) {
		saveValue(prop, propertyKey(playerID, modeName), value);
	}

	public final void load(int playerID, CustomProperties prop,
			String modeName) {
		value = loadValue(prop, propertyKey(playerID, modeName));
	}

	protected abstract void saveValue(CustomProperties prop, String key,
			T value);

	protected abstract T loadValue(CustomProperties prop, String key);

	protected final String propertyKey(int playerID, String modeName) {
		return modeName + "." + name + playerSuffix(playerID);
	}

	private static String playerSuffix(int playerID) {
		return playerID < 0 ? "" : ".p" + playerID;
	}
}
