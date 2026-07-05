// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

/**
 * Mode select screen (SDL)
 */
public class StateSelectModeSDL extends DummyMenuScrollStateSDL {
	/** Number of game modes in one page */
	public static final int PAGE_HEIGHT = 24;

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
		list = NullpoMinoSDL.modeManager.getModeNames(false);
		maxCursor = list.length - 1;

		String lastmode = NullpoMinoSDL.propGlobal.getProperty("name.mode", null);
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
		closeBtn = newCloseButton();
	}

	/*
	 * Draw the screen
	 */
	@Override
	public void onRenderSuccess() {
		NormalFontSDL.printFontGrid(1, 1, "SELECT MODE (" + (cursor + 1) + "/" + list.length + ")",
									NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printTTFFont(16, 440, getModeDesc(list[cursor]));
	}

	/*
	 * Decide
	 */
	@Override
	protected boolean onDecide() {
		ResourceHolderSDL.soundManager.play("decide");
		NullpoMinoSDL.propGlobal.setProperty("name.mode", list[cursor]);
		NullpoMinoSDL.saveConfig();
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTRULEFROMLIST);
		return false;
	}

	/*
	 * Cancel
	 */
	@Override
	protected boolean onCancel() {
		NullpoMinoSDL.goBack();
		return false;
	}
}
