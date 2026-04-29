// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.util.ArrayList;
import java.util.List;

import nullpomino.game.subsystem.mode.GameMode;

import org.apache.log4j.Logger;

/**
 * Mode Management class
 */
public class ModeManager {
	/** Log */
	static Logger log = Logger.getLogger(ModeManager.class);

	private final List<GameMode> modes = new ArrayList<GameMode>();

	/**
	 * Constructor
	 */
	public ModeManager() {
	}

	/**
	 * Copy constructor
	 * @param m Copy source
	 */
	public ModeManager(ModeManager m) {
		modes.addAll(m.modes);
	}

	/**
	 * Mode OfcountGet the(Usually+All net play)
	 * @return ModeOfcount(Usually+All net play)
	 */
	public int getSize() {
		return modes.size();
	}

	/**
	 * Mode OfcountGet the
	 * @param netplay falseIf normalMode Only, When true,For net playMode OnlycountObtained
	 * @return ModeOfcount
	 */
	public int getNumberOfModes(boolean netplay) {
		return filteredModes(netplay).size();
	}

	/**
	 * All that has been readMode nameGet the
	 * @return Mode nameAn array of
	 */
	public String[] getAllModeNames() {
		String[] strings = new String[getSize()];

		for(int i = 0; i < strings.length; i++) {
			strings[i] = getName(i);
		}

		return strings;
	}

	/**
	 * Are loadedMode nameGet the
	 * @param netplay falseIf normalMode Only, When true,For net playMode Only obtained
	 * @return Mode nameAn array of
	 */
	public String[] getModeNames(boolean netplay) {
		List<GameMode> filtered = filteredModes(netplay);
		String[] strings = new String[filtered.size()];
		for(int i = 0; i < filtered.size(); i++) {
			strings[i] = filtered.get(i).getName();
		}

		return strings;
	}

	/**
	 * Mode  nameGet the
	 * @param id ModeID
	 * @return Mode name (idIf the incorrect &quot;*INVALID MODE*&quot;)
	 */
	public String getName(int id) {
		GameMode mode = getMode(id);
		return (mode == null) ? "*INVALID MODE*" : mode.getName();
	}

	/**
	 * Mode  nameFromIDGet the
	 * @param name Mode name
	 * @return ModeID (If it is not found-1)
	 */
	public int getIDbyName(String name) {
		if(name == null) return -1;

		for(int i = 0; i < modes.size(); i++) {
			GameMode mode = modes.get(i);
			if((mode != null) && name.equals(mode.getName())) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Mode Gets an object
	 * @param id ModeID
	 * @return ModeObject (idIf the incorrectnull)
	 */
	public GameMode getMode(int id) {
		if((id < 0) || (id >= modes.size())) return null;
		return modes.get(id);
	}

	/**
	 * Mode Gets an object
	 * @param name Mode name
	 * @return ModeObject (Not foundnull)
	 */
	public GameMode getMode(String name) {
		return getMode(getIDbyName(name));
	}

	/**
	 * Instantiate every class in {@code classes} and append it to the loaded
	 * mode list. Preserves the input order; a class that fails to instantiate
	 * is logged and skipped.
	 */
	public void loadGameModes(List<Class<? extends GameMode>> classes) {
		for(Class<? extends GameMode> clazz : classes) {
			addGameMode(clazz);
		}
	}

	public void addMode(GameMode mode) {
		modes.add(mode);
	}

	private void addGameMode(Class<? extends GameMode> clazz) {
		try {
			addMode(clazz.getDeclaredConstructor().newInstance());
		} catch(Exception e) {
			log.warn("Mode class " + clazz.getName() + " load failed", e);
		}
	}

	private List<GameMode> filteredModes(boolean netplay) {
		List<GameMode> filtered = new ArrayList<GameMode>();
		for(GameMode mode : modes) {
			if(matchesNetplay(mode, netplay)) {
				filtered.add(mode);
			}
		}
		return filtered;
	}

	private static boolean matchesNetplay(GameMode mode, boolean netplay) {
		return (mode != null) && (mode.isNetplayMode() == netplay);
	}
}
