// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

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
	 * Property fileGame from the list that was written toMode Read
	 * @param prop Property file
	 */
	public void loadGameModes(CustomProperties prop) {
		for(int count = 0; ; count++) {
			// Read the name of a class
			String name = prop.getProperty(String.valueOf(count), null);
			if(name == null) return;
			addGameMode(name);
		}
	}

	/**
	 * Game from the list that was written to a text fileMode Read
	 * @param bf I read a text fileBufferedReader
	 */
	public void loadGameModes(BufferedReader bf) {
		try {
			String name;
			while((name = bf.readLine()) != null) {
				if(name.length() == 0) return;
				if(!name.startsWith("#")) addGameMode(name);
			}
		} catch (IOException e) {
			log.warn("IOException on readLine()", e);
		}
	}

	private void addGameMode(String name) {
		try {
			modelist.add(ClassFactory.create(name, GameMode.class));
		} catch(ClassNotFoundException e) {
			log.warn("Mode class " + name + " not found", e);
		} catch(Exception e) {
			log.warn("Mode class " + name + " load failed", e);
		}
	}
}
