// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import nullpomino.game.subsystem.ai.AIPlayer;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.util.ClassFactory;
import nullpomino.util.GeneralUtil;

import org.apache.log4j.Logger;

/**
 * AIState selection screen
 */
public class StateConfigAISelectSDL extends BaseStateSDL {
	/** Log */
	static Logger log = Logger.getLogger(StateConfigAISelectSDL.class);

	/** 1Displayed on the screenMaximumAIcount */
	public static final int MAX_AI_IN_ONE_PAGE = 20;

	/** Player ID */
	public int player = 0;

	/** AIList of classes */
	protected String[] aiPathList;

	/** AIOfNameList */
	protected String[] aiNameList;

	/** Current AIClass of */
	protected String currentAI;

	/** AIOfID */
	protected int aiID = 0;

	/** AIMovement interval of */
	protected int aiMoveDelay = 0;

	/** AIThinking of waiting time */
	protected int aiThinkDelay = 0;

	/** AIUsing threads in */
	protected boolean aiUseThread = false;

	protected boolean aiShowHint = false;

	protected boolean aiPrethink = false;

	protected boolean aiShowState = false;

	/** Cursor position */
	protected int cursor = 0;

	/**
	 * Constructor
	 */
	public StateConfigAISelectSDL() {
		try {
			BufferedReader in = new BufferedReader(new FileReader("config/list/ai.lst"));
			aiPathList = loadAIList(in);
			aiNameList = loadAINames(aiPathList);
			in.close();
		} catch (IOException e) {
			log.warn("Failed to load AI list", e);
		}
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
		currentAI = NullpoMinoSDL.propGlobal.getProperty(player + ".ai", "");
		aiMoveDelay = NullpoMinoSDL.propGlobal.getProperty(player + ".aiMoveDelay", 0);
		aiThinkDelay = NullpoMinoSDL.propGlobal.getProperty(player + ".aiThinkDelay", 0);
		aiUseThread = NullpoMinoSDL.propGlobal.getProperty(player + ".aiUseThread", true);
		aiShowHint = NullpoMinoSDL.propGlobal.getProperty(player + ".aiShowHint", false);
		aiPrethink = NullpoMinoSDL.propGlobal.getProperty(player + ".aiPrethink", false);
		aiShowState = NullpoMinoSDL.propGlobal.getProperty(player + ".aiShowState", false);

		aiID = -1;
		for(int i = 0; i < aiPathList.length; i++) {
			if(currentAI.equals(aiPathList[i])) aiID = i;
		}
	}

	/**
	 * AIReads the list
	 * @param bf To read from a text file
	 * @return AIList
	 */
	public String[] loadAIList(BufferedReader bf) {
		ArrayList<String> aiArrayList = new ArrayList<String>();

		while(true) {
			String name = null;
			try {
				name = bf.readLine();
			} catch (Exception e) {
				break;
			}
			if(name == null) break;
			if(name.length() == 0) break;

			if(!name.startsWith("#"))
				aiArrayList.add(name);
		}

		return aiArrayList.toArray(new String[0]);
	}

	/**
	 * AIOfNameCreate a list
	 * @param aiPath AIList of classes
	 * @return AIOfNameList
	 */
	public String[] loadAINames(String[] aiPath) {
		String[] aiName = new String[aiPath.length];

		for(int i = 0; i < aiPath.length; i++) {
			aiName[i] = "(INVALID)";

			try {
				AIPlayer aiObj = ClassFactory.create(aiPath[i], AIPlayer.class);
				aiName[i] = aiObj.getName();
			} catch(ClassNotFoundException e) {
				log.warn("AI class " + aiPath[i] + " not found", e);
			} catch(Throwable e) {
				log.warn("AI class " + aiPath[i] + " load failed", e);
			}
		}

		return aiName;
	}

	/*
	 * Draw the screen
	 */
	@Override
	public void render() {
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);

		NormalFontSDL.printFontGrid(1, 1, (player + 1) + "P AI SETTING", NormalFontSDL.COLOR_ORANGE);

		NormalFontSDL.printFontGrid(1, 3 + cursor, "b", NormalFontSDL.COLOR_RED);

		String aiName = "";
		if(aiID < 0) aiName = "(DISABLE)";
		else aiName = aiNameList[aiID].toUpperCase();
		NormalFontSDL.printFontGrid(2, 3, "AI TYPE:" + aiName, (cursor == 0));
		NormalFontSDL.printFontGrid(2, 4, "AI MOVE DELAY:" + aiMoveDelay, (cursor == 1));
		NormalFontSDL.printFontGrid(2, 5, "AI THINK DELAY:" + aiThinkDelay, (cursor == 2));
		NormalFontSDL.printFontGrid(2, 6, "AI USE THREAD:" + GeneralUtil.getONorOFF(aiUseThread), (cursor == 3));
		NormalFontSDL.printFontGrid(2, 7, "AI SHOW HINT:" + GeneralUtil.getONorOFF(aiShowHint), (cursor == 4));
		NormalFontSDL.printFontGrid(2, 8, "AI PRE-THINK:" + GeneralUtil.getONorOFF(aiPrethink), (cursor == 5));
		NormalFontSDL.printFontGrid(2, 9, "AI SHOW INFO:" + GeneralUtil.getONorOFF(aiShowState), (cursor == 6));

		NormalFontSDL.printFontGrid(1, 28, "A:OK B:CANCEL", NormalFontSDL.COLOR_GREEN);
	}

	/*
	 * Update of the internal state
	 */
	@Override
	public void update() {
		// Cursor movement
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_UP)) {
			cursor--;
			if(cursor < 0) cursor = 6;
			ResourceHolderSDL.soundManager.play("cursor");
		}
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_DOWN)) {
			cursor++;
			if(cursor > 6) cursor = 0;
			ResourceHolderSDL.soundManager.play("cursor");
		}

		// Page Up / Page Down
		cursor = PageNavigationSDL.jumpToEnd(PageNavigationSDL.checkPageEvent(), cursor, 0, 6);

		// Configuration changes
		int change = 0;
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_LEFT)) change = -1;
		if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_RIGHT)) change = 1;

		if(change != 0) {
			ResourceHolderSDL.soundManager.play("change");

			switch(cursor) {
			case 0:
				aiID += change;
				if(aiID < -1) aiID = aiNameList.length - 1;
				if(aiID > aiNameList.length - 1) aiID = -1;
				break;
			case 1:
				aiMoveDelay += change;
				if(aiMoveDelay < -1) aiMoveDelay = 99;
				if(aiMoveDelay > 99) aiMoveDelay = -1;
				break;
			case 2:
				aiThinkDelay += change * 10;
				if(aiThinkDelay < 0) aiThinkDelay = 1000;
				if(aiThinkDelay > 1000) aiThinkDelay = 0;
				break;
			case 3:
				aiUseThread = !aiUseThread;
				break;
			case 4:
				aiShowHint = !aiShowHint;
				break;
			case 5:
				aiPrethink = !aiPrethink;
				break;
			case 6:
				aiShowState = !aiShowState;
				break;
			}
		}

		// Decision button
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_A)) {
			ResourceHolderSDL.soundManager.play("decide");

			if(aiID >= 0) NullpoMinoSDL.propGlobal.setProperty(player + ".ai", aiPathList[aiID]);
			else NullpoMinoSDL.propGlobal.setProperty(player + ".ai", "");
			NullpoMinoSDL.propGlobal.setProperty(player + ".aiMoveDelay", aiMoveDelay);
			NullpoMinoSDL.propGlobal.setProperty(player + ".aiThinkDelay", aiThinkDelay);
			NullpoMinoSDL.propGlobal.setProperty(player + ".aiUseThread", aiUseThread);
			NullpoMinoSDL.propGlobal.setProperty(player + ".aiShowHint",aiShowHint);
			NullpoMinoSDL.propGlobal.setProperty(player + ".aiPrethink",aiPrethink);
			NullpoMinoSDL.propGlobal.setProperty(player + ".aiShowState",aiShowState);
			NullpoMinoSDL.saveConfig();

			NullpoMinoSDL.goBack();
			return;
		}

		// Cancel button
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_B)) {
			NullpoMinoSDL.goBack();
			return;
		}
	}
}
