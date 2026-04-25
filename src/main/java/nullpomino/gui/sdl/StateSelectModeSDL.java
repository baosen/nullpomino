// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import java.util.LinkedList;

/**
 * Mode select screen (SDL)
 */
public class StateSelectModeSDL extends DummyMenuScrollStateSDL {
	/** Number of game modes in one page */
	public static final int PAGE_HEIGHT = 24;

	/** true if top-level folder */
	public static boolean isTopLevel;

	/** Current folder name */
	protected String strCurrentFolder;

	/**
	 * Constructor
	 */
	public StateSelectModeSDL() {
		super();
		pageHeight = PAGE_HEIGHT;
	}

	/**
	 * Prepare mode list
	 */
	protected void prepareModeList() {
		strCurrentFolder = StateSelectModeFolderSDL.strCurrentFolder;

		// Get mode list
		LinkedList<String> listMode = null;
		if(isTopLevel) {
			listMode = StateSelectModeFolderSDL.listTopLevelModes;
			list = new String[listMode.size() + 1];
			for(int i = 0; i < listMode.size(); i++) {
				list[i] = listMode.get(i);
			}
			list[list.length - 1] = "[MORE...]";
		} else {
			listMode = StateSelectModeFolderSDL.mapFolder.get(strCurrentFolder);
			if(listMode != null) {
				list = new String[listMode.size()];
				for(int i = 0; i < list.length; i++) {
					list[i] = listMode.get(i);
				}
			} else {
				list = NullpoMinoSDL.modeManager.getModeNames(false);
			}
		}
		maxCursor = list.length - 1;

		// Set cursor postion
		String lastmode = null;
		if(isTopLevel) {
			lastmode = NullpoMinoSDL.propGlobal.getProperty("name.mode.toplevel", null);
		} else if(strCurrentFolder.length() > 0) {
			lastmode = NullpoMinoSDL.propGlobal.getProperty("name.mode." + strCurrentFolder, null);
		} else {
			lastmode = NullpoMinoSDL.propGlobal.getProperty("name.mode", null);
		}
		cursor = getIDbyName(lastmode);
		if(cursor < 0) cursor = 0;
		if(cursor > list.length - 1) cursor = list.length - 1;
	}

	/**
	 * Get mode ID (not including netplay modes)
	 * @param name Name of mode
	 * @return ID (-1 if not found)
	 */
	protected int getIDbyName(String name) {
		if((name == null) || (list == null)) return -1;

		for(int i = 0; i < list.length; i++) {
			if(name.equals(list[i])) {
				return i;
			}
		}

		return -1;
	}

	/**
	 * Get game mode description
	 * @param str Mode name
	 * @return Description
	 */
	protected String getModeDesc(String str) {
		String str2 = str.replace(' ', '_');
		str2 = str2.replace('(', 'l');
		str2 = str2.replace(')', 'r');
		String result = NullpoMinoSDL.propModeDesc.getProperty(str2);
		if(result == null) {
			result = NullpoMinoSDL.propDefaultModeDesc.getProperty(str2, str2);
		}
		return result;
	}

	/*
	 * Enter
	 */
	@Override
	public void enter() {
		prepareModeList();
	}

	/*
	 * Draw the screen
	 */
	@Override
	public void onRenderSuccess() {
		if(!isTopLevel && (strCurrentFolder.length() > 0)) {
			NormalFontSDL.printFontGrid(1, 1, strCurrentFolder + " (" + (cursor + 1) + "/" + list.length + ")",
					NormalFontSDL.COLOR_ORANGE);
		} else {
			NormalFontSDL.printFontGrid(1, 1, "MODE SELECT (" + (cursor + 1) + "/" + list.length + ")",
										NormalFontSDL.COLOR_ORANGE);
		}

		NormalFontSDL.printTTFFont(16, 440, getModeDesc(list[cursor]));
	}

	/*
	 * Decide
	 */
	@Override
	protected boolean onDecide() {
		ResourceHolderSDL.soundManager.play("decide");

		if(isTopLevel && (cursor == list.length - 1)) {
			// More...
			NullpoMinoSDL.propGlobal.setProperty("name.mode.toplevel", list[cursor]);
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODEFOLDER);
		} else {
			// Go to mode selector
			if(isTopLevel) {
				NullpoMinoSDL.propGlobal.setProperty("name.mode.toplevel", list[cursor]);
			}
			if(strCurrentFolder.length() > 0) {
				NullpoMinoSDL.propGlobal.setProperty("name.mode." + strCurrentFolder, list[cursor]);
			}
			NullpoMinoSDL.propGlobal.setProperty("name.mode", list[cursor]);
			NullpoMinoSDL.saveConfig();
			NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTRULEFROMLIST);
		}

		return false;
	}

	/*
	 * Cancel
	 */
	@Override
	protected boolean onCancel() {
		// Whichever screen pushed us onto the stack — title (top-level) or
		// the folder picker — is exactly where goBack will return us.
		NullpoMinoSDL.goBack();
		return false;
	}
}
