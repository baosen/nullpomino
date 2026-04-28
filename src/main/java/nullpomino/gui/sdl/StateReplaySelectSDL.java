// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;

import nullpomino.game.component.Statistics;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.apache.log4j.Logger;


/**
 * State selection screen replay
 */
public class StateReplaySelectSDL extends DummyMenuScrollStateSDL {
	/** Log */
	static Logger log = Logger.getLogger(StateReplaySelectSDL.class);

	/** 1Displayed on the screenMaximumFilecount */
	public static final int PAGE_HEIGHT = 20;

	/** Mode  name */
	protected String[] modenameList;

	/** Rule name */
	protected String[] rulenameList;

	/** ScoreInformation such as the */
	protected Statistics[] statsList;

	public StateReplaySelectSDL () {
		pageHeight = PAGE_HEIGHT;
		nullError = "REPLAY DIRECTORY NOT FOUND";
		emptyError = "NO REPLAY FILE";
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
		list = getReplayFileList();
		if (list != null) { maxCursor = list.length-1; }
		setReplayRuleAndModeList();
	}

	/**
	 * Gets the list of files replay
	 * @return Replay fileFilenameArray of. If there is no directorynull
	 */
	protected String[] getReplayFileList() {
		File dir = new File(NullpoMinoSDL.propGlobal.getProperty("custom.replay.directory", "replay"));

		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File dir1, String name) {
				return name.endsWith(".rep");
			}
		};

		String[] list = dir.list(filter);

		if(list != null && !System.getProperty("os.name").startsWith("Windows")) {
			// Sort if not windows
			Arrays.sort(list);
		}

		return list;
	}

	/**
	 * Set the details of replay
	 */
	protected void setReplayRuleAndModeList() {
		if(list == null) return;

		modenameList = new String[list.length];
		rulenameList = new String[list.length];
		statsList = new Statistics[list.length];

		for(int i = 0; i < list.length; i++) {
			CustomProperties prop;
			try {
				prop = CustomProperties.loadFromFile(
						NullpoMinoSDL.propGlobal.getProperty("custom.replay.directory", "replay") + "/" + list[i]);
			} catch (IOException e) {
				log.warn("Failed to load replay file from " + list[i], e);
				prop = new CustomProperties();
			}

			modenameList[i] = prop.getProperty("name.mode", "");
			rulenameList[i] = prop.getProperty("name.rule", "");

			statsList[i] = new Statistics();
			statsList[i].readProperty(prop, 0);
		}
	}

	/*
	 * Draw the screen
	 */
	@Override
	protected void onRenderSuccess() {
		String title = "SELECT REPLAY FILE";
		title += " (" + (cursor + 1) + "/" + (list.length) + ")";

		NormalFontSDL.printFontGrid(1, 1, title, NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 24, "MODE:" + modenameList[cursor] + " RULE:" + rulenameList[cursor], NormalFontSDL.COLOR_CYAN);
		NormalFontSDL.printFontGrid(1, 25,
									"SCORE:" + statsList[cursor].score + " LINE:" + statsList[cursor].lines
									, NormalFontSDL.COLOR_CYAN);
		NormalFontSDL.printFontGrid(1, 26,
									"LEVEL:" + (statsList[cursor].level + statsList[cursor].levelDispAdd) +
									" TIME:" + GeneralUtil.getTime(statsList[cursor].time)
									, NormalFontSDL.COLOR_CYAN);
		NormalFontSDL.printFontGrid(1, 27,
									"GAME RATE:" + ( (statsList[cursor].gamerate == 0f) ? "UNKNOWN" : ((100*statsList[cursor].gamerate) + "%") )
									, NormalFontSDL.COLOR_CYAN);
	}

	@Override
	protected boolean onDecide() {
		ResourceHolderSDL.soundManager.play("decide");

		CustomProperties prop;
		try {
			prop = CustomProperties.loadFromFile(
					NullpoMinoSDL.propGlobal.getProperty("custom.replay.directory", "replay") + "/" + list[cursor]);
		} catch (IOException e) {
			log.error("Failed to load replay file from " + list[cursor], e);
			return true;
		}

		StateInGameSDL s = (StateInGameSDL)NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_INGAME];
		s.startReplayGame(prop);

		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_INGAME);

		return false;
	}

	@Override
	protected boolean onCancel() {
		NullpoMinoSDL.goBack();
		return false;
	}
}
