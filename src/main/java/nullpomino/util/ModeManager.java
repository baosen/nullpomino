// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import nullpomino.game.mode.GameMode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mode Management class
 */
public class ModeManager {
	/** Log */
	static Logger log = LoggerFactory.getLogger(ModeManager.class);

	private final List<GameMode> modes = new ArrayList<GameMode>();

	/**
	 * Constructor
	 */
	public ModeManager() {
	}

	/**
	 * Are loadedMode nameGet the
	 * @param netplay falseIf normalMode Only, When true,For net playMode Only obtained
	 * @return Mode nameAn array of
	 */
	public String[] getModeNames(boolean netplay) {
		return modesMatching(netplay)
				.map(GameMode::getName)
				.toArray(String[]::new);
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
			log.warn("Mode class {} load failed", clazz.getName(), e);
		}
	}

	private Stream<GameMode> modesMatching(boolean netplay) {
		return modes.stream().filter(mode -> matchesNetplay(mode, netplay));
	}

	private static boolean matchesNetplay(GameMode mode, boolean netplay) {
		return (mode != null) && (mode.isNetplayMode() == netplay);
	}
}
