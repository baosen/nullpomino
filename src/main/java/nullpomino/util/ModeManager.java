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

	/** Mode Dynamic array of */
	public ArrayList<GameMode> modelist = new ArrayList<GameMode>();

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
		modelist.addAll(m.modelist);
	}

	/**
	 * Mode OfcountGet the(Usually+All net play)
	 * @return ModeOfcount(Usually+All net play)
	 */
	public int getSize() {
		return modelist.size();
	}

	/**
	 * Mode OfcountGet the
	 * @param netplay falseIf normalMode Only, When true,For net playMode OnlycountObtained
	 * @return ModeOfcount
	 */
	public int getNumberOfModes(boolean netplay) {
		int count = 0;

		for(GameMode mode : modelist) {
			if((mode != null) && (mode.isNetplayMode() == netplay)) {
				count++;
			}
		}

		return count;
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
		ArrayList<String> strings = new ArrayList<String>();
		for(GameMode mode : modelist) {
			if((mode != null) && (mode.isNetplayMode() == netplay)) {
				strings.add(mode.getName());
			}
		}

		return strings.toArray(new String[0]);
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

		for(int i = 0; i < modelist.size(); i++) {
			GameMode mode = modelist.get(i);
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
		if((id < 0) || (id >= modelist.size())) return null;
		return modelist.get(id);
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

	private void addGameMode(Class<? extends GameMode> clazz) {
		try {
			modelist.add(clazz.getDeclaredConstructor().newInstance());
		} catch(Exception e) {
			log.warn("Mode class " + clazz.getName() + " load failed", e);
		}
	}
}
