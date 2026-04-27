package nullpomino.gui.sdl;

import java.util.HashMap;
import java.util.LinkedList;

import nullpomino.gui.sdl.ModeFolderRegistry.Folder;

/**
 * Mode folder select (SDL)
 */
public class StateSelectModeFolderSDL extends DummyMenuScrollStateSDL {
	/** Number of folders in one page */
	public static final int PAGE_HEIGHT = 24;

	/** Top-level mode list */
	public static LinkedList<String> listTopLevelModes;

	/** Folder names list */
	public static LinkedList<String> listFolder;

	/** HashMap of mode folder (FolderName->ModeNames) */
	public static HashMap<String, LinkedList<String>> mapFolder;

	/** Current folder name */
	public static String strCurrentFolder;

	/**
	 * Constructor
	 */
	public StateSelectModeFolderSDL() {
		super();
		pageHeight = PAGE_HEIGHT;

		loadFolderListFile();
		prepareFolderList();
	}

	/**
	 * Populate the folder data from {@link ModeFolderRegistry}. The static
	 * fields are kept for the existing consumers ({@link StateSelectModeSDL}
	 * reads them directly); only the source changes from a parsed text file
	 * to literal Java data.
	 */
	public static void loadFolderListFile() {
		if(listTopLevelModes == null) listTopLevelModes = new LinkedList<String>();
		else listTopLevelModes.clear();

		if(listFolder == null) listFolder = new LinkedList<String>();
		else listFolder.clear();

		if(mapFolder == null) mapFolder = new HashMap<String, LinkedList<String>>();
		else mapFolder.clear();

		strCurrentFolder = NullpoMinoSDL.propGlobal.getProperty("name.folder", "");

		listTopLevelModes.addAll(ModeFolderRegistry.TOP_LEVEL);

		for(Folder folder : ModeFolderRegistry.FOLDERS) {
			listFolder.add(folder.name());
			mapFolder.put(folder.name(), new LinkedList<String>(folder.modes()));
		}
	}

	/**
	 * Prepare folder list
	 */
	protected void prepareFolderList() {
		list = new String[listFolder.size() + 1];
		maxCursor = list.length - 1;
		for(int i = 0; i < listFolder.size(); i++) {
			list[i] = listFolder.get(i);

			if(strCurrentFolder.equals(list[i])) {
				cursor = i;
			}
		}
		list[list.length - 1] = "[ALL MODES]";
	}

	/**
	 * Get folder description
	 * @param str Folder name
	 * @return Description
	 */
	protected String getFolderDesc(String str) {
		String str2 = str.replace(' ', '_');
		str2 = str2.replace('(', 'l');
		str2 = str2.replace(')', 'r');
		String result = NullpoMinoSDL.propModeDesc.getProperty("Folder_" + str2);
		if(result == null) {
			result = NullpoMinoSDL.propDefaultModeDesc.getProperty("Folder_" + str2, "Folder_" + str2);
		}
		return result;
	}

	/*
	 * Render screen
	 */
	@Override
	protected void onRenderSuccess() {
		NormalFontSDL.printFontGrid(1, 1, "SELECT MODE FOLDER (" + (cursor + 1) + "/" + list.length + ")", NormalFontSDL.COLOR_ORANGE);
		NormalFontSDL.printTTFFont(16, 440, getFolderDesc(list[cursor]));
	}

	/*
	 * Decide
	 */
	@Override
	protected boolean onDecide() {
		ResourceHolderSDL.soundManager.play("decide");
		if(cursor < listFolder.size()) {
			strCurrentFolder = list[cursor];
		} else {
			strCurrentFolder = "";
		}
		NullpoMinoSDL.propGlobal.setProperty("name.folder", strCurrentFolder);
		NullpoMinoSDL.saveConfig();
		StateSelectModeSDL.isTopLevel = false;
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		return false;
	}

	/*
	 * Cancel
	 */
	@Override
	protected boolean onCancel() {
		// If the back stack pops us to SELECTMODE, it should render as the
		// top-level mode list rather than whatever folder's contents were
		// last shown there — the user is walking back out of the folder.
		StateSelectModeSDL.isTopLevel = true;
		NullpoMinoSDL.goBack();
		return false;
	}
}
