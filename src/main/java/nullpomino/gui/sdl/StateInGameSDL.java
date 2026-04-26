// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import org.apache.log4j.Logger;

import nullpomino.game.component.Controller;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.subsystem.ai.DummyAI;
import nullpomino.game.subsystem.mode.GameMode;
import nullpomino.game.subsystem.wallkick.Wallkick;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;
import net.omegaboshi.nullpomino.game.subsystem.randomizer.Randomizer;

/**
 * Game screen state (Local play)
 */
public class StateInGameSDL extends BaseStateSDL {
	/** Log */
	static Logger log = Logger.getLogger(StateInGameSDL.class);

	/** Game main class */
	protected GameManager gameManager;

	/** Game paused flag */
	protected boolean pause = false;

	/** Hide pause menu */
	protected boolean pauseMessageHide = false;

	/** Frame step enabled flag */
	protected boolean enableframestep = false;

	/** Fast forward */
	protected int fastforward = 0;

	/** Pause menu cursor position */
	protected int cursor = 0;

	/** Number of frames remaining until pause key can be used */
	protected int pauseFrame = 0;

	/** Previous ingame flag (Used by title-bar text change) */
	protected boolean prevInGameFlag = false;

	/** Current game mode name */
	protected String modeName;

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
		NullpoMinoSDL.disableAutoInputUpdate = true;
		NullpoMinoSDL.isInGame = true;
		enableframestep = NullpoMinoSDL.propConfig.getProperty("option.enableframestep", false);
		fastforward = 0;
		cursor = 0;
		prevInGameFlag = false;
	}

	/**
	 * Start a new game (Rule will be user-selected one))
	 */
	public void startNewGame() {
		startNewGame(null);
	}

	/**
	 * Start a new game
	 * @param strRulePath Rule file path (null if you want to use user-selected one)
	 */
	public void startNewGame(String strRulePath) {
		gameManager = new GameManager(new RendererSDL());
		pause = false;

		gameManager.receiver.setGraphics(NullpoMinoSDL.renderer);

		// Mode
		modeName = NullpoMinoSDL.propGlobal.getProperty("name.mode", "");
		GameMode modeObj = NullpoMinoSDL.modeManager.getMode(modeName);
		if(modeObj == null) {
			log.warn("Couldn't find mode:" + modeName);
		} else {
			gameManager.mode = modeObj;
		}

		gameManager.init();

		// Initialization for each player
		for(int i = 0; i < gameManager.getPlayers(); i++) {
			// Tuning settings
			gameManager.engine[i].owRotateButtonDefaultRight = NullpoMinoSDL.propGlobal.getProperty(i + ".tuning.owRotateButtonDefaultRight", -1);
			gameManager.engine[i].owSkin = NullpoMinoSDL.propGlobal.getProperty(i + ".tuning.owSkin", -1);
			gameManager.engine[i].owMinDAS = NullpoMinoSDL.propGlobal.getProperty(i + ".tuning.owMinDAS", -1);
			gameManager.engine[i].owMaxDAS = NullpoMinoSDL.propGlobal.getProperty(i + ".tuning.owMaxDAS", -1);
			gameManager.engine[i].owDasDelay = NullpoMinoSDL.propGlobal.getProperty(i + ".tuning.owDasDelay", -1);
			gameManager.engine[i].owReverseUpDown = NullpoMinoSDL.propGlobal.getProperty(i + ".tuning.owReverseUpDown", false);
			gameManager.engine[i].owMoveDiagonal = NullpoMinoSDL.propGlobal.getProperty(i + ".tuning.owMoveDiagonal", -1);
			gameManager.engine[i].owBlockOutlineType = NullpoMinoSDL.propGlobal.getProperty(i + ".tuning.owBlockOutlineType", -1);
			gameManager.engine[i].owBlockShowOutlineOnly = NullpoMinoSDL.propGlobal.getProperty(i + ".tuning.owBlockShowOutlineOnly", -1);

			// Rule
			RuleOptions ruleopt = null;
			String rulename = strRulePath;
			if(rulename == null) {
				rulename = NullpoMinoSDL.propGlobal.getProperty(i + ".rule", "");
				if(gameManager.mode.getGameStyle() > 0) {
					rulename = NullpoMinoSDL.propGlobal.getProperty(i + ".rule." + gameManager.mode.getGameStyle(), "");
				}
			}
			if((rulename != null) && (rulename.length() > 0)) {
				log.debug("Load rule options from " + rulename);
				ruleopt = GeneralUtil.loadRule(rulename);
			} else {
				log.debug("Load rule options from setting file");
				ruleopt = new RuleOptions();
				ruleopt.readProperty(NullpoMinoSDL.propGlobal, i);
			}
			gameManager.engine[i].ruleopt = ruleopt;

			// NEXTOrder generation algorithm
			if((ruleopt.strRandomizer != null) && (ruleopt.strRandomizer.length() > 0)) {
				Randomizer randomizerObject = GeneralUtil.loadRandomizer(ruleopt.strRandomizer);
				gameManager.engine[i].randomizer = randomizerObject;
			}

			// Wallkick
			if((ruleopt.strWallkick != null) && (ruleopt.strWallkick.length() > 0)) {
				Wallkick wallkickObject = GeneralUtil.loadWallkick(ruleopt.strWallkick);
				gameManager.engine[i].wallkick = wallkickObject;
			}

			// AI
			String aiName = NullpoMinoSDL.propGlobal.getProperty(i + ".ai", "");
			if(aiName.length() > 0) {
				DummyAI aiObj = GeneralUtil.loadAIPlayer(aiName);
				gameManager.engine[i].ai = aiObj;
				gameManager.engine[i].aiMoveDelay = NullpoMinoSDL.propGlobal.getProperty(i + ".aiMoveDelay", 0);
				gameManager.engine[i].aiThinkDelay = NullpoMinoSDL.propGlobal.getProperty(i + ".aiThinkDelay", 0);
				gameManager.engine[i].aiUseThread = NullpoMinoSDL.propGlobal.getProperty(i + ".aiUseThread", true);
				gameManager.engine[i].aiShowHint = NullpoMinoSDL.propGlobal.getProperty(i + ".aiShowHint", false);
				gameManager.engine[i].aiPrethink = NullpoMinoSDL.propGlobal.getProperty(i + ".aiPrethink", false);
				gameManager.engine[i].aiShowState = NullpoMinoSDL.propGlobal.getProperty(i + ".aiShowState", false);
			}
			gameManager.showInput = NullpoMinoSDL.propConfig.getProperty("option.showInput", false);

			// Called at initialization
			gameManager.engine[i].init();
		}

		updateTitleBarCaption();
	}

	/**
	 * Load and play the replay
	 * @param prop Replay dataProperty set that contains the
	 */
	public void startReplayGame(CustomProperties prop) {
		gameManager = new GameManager(new RendererSDL());
		gameManager.replayMode = true;
		gameManager.replayProp = prop;
		pause = false;

		gameManager.receiver.setGraphics(NullpoMinoSDL.renderer);

		// Mode
		modeName = prop.getProperty("name.mode", "");
		GameMode modeObj = NullpoMinoSDL.modeManager.getMode(modeName);
		if(modeObj == null) {
			log.warn("Couldn't find mode:" + modeName);
		} else {
			gameManager.mode = modeObj;
		}

		gameManager.init();

		// Initialization for each player
		for(int i = 0; i < gameManager.getPlayers(); i++) {
			// Rule
			RuleOptions ruleopt = new RuleOptions();
			ruleopt.readProperty(prop, i);
			gameManager.engine[i].ruleopt = ruleopt;

			// NEXTOrder generation algorithm
			if((ruleopt.strRandomizer != null) && (ruleopt.strRandomizer.length() > 0)) {
				Randomizer randomizerObject = GeneralUtil.loadRandomizer(ruleopt.strRandomizer);
				gameManager.engine[i].randomizer = randomizerObject;
			}

			// Wallkick
			if((ruleopt.strWallkick != null) && (ruleopt.strWallkick.length() > 0)) {
				Wallkick wallkickObject = GeneralUtil.loadWallkick(ruleopt.strWallkick);
				gameManager.engine[i].wallkick = wallkickObject;
			}

			// AI (For added replay)
			String aiName = NullpoMinoSDL.propGlobal.getProperty(i + ".ai", "");
			if(aiName.length() > 0) {
				DummyAI aiObj = GeneralUtil.loadAIPlayer(aiName);
				gameManager.engine[i].ai = aiObj;
				gameManager.engine[i].aiMoveDelay = NullpoMinoSDL.propGlobal.getProperty(i + ".aiMoveDelay", 0);
				gameManager.engine[i].aiThinkDelay = NullpoMinoSDL.propGlobal.getProperty(i + ".aiThinkDelay", 0);
				gameManager.engine[i].aiUseThread = NullpoMinoSDL.propGlobal.getProperty(i + ".aiUseThread", true);
				gameManager.engine[i].aiShowHint = NullpoMinoSDL.propGlobal.getProperty(i + ".aiShowHint", false);
				gameManager.engine[i].aiPrethink = NullpoMinoSDL.propGlobal.getProperty(i + ".aiPrethink", false);
				gameManager.engine[i].aiShowState = NullpoMinoSDL.propGlobal.getProperty(i + ".aiShowState", false);
			}
			gameManager.showInput = NullpoMinoSDL.propConfig.getProperty("option.showInput", false);

			// Called at initialization
			gameManager.engine[i].init();
		}

		updateTitleBarCaption();
	}

	/**
	 * Update title bar text
	 */
	public void updateTitleBarCaption() {
		String strTitle = "NullpoMino - " + modeName;

		if((gameManager != null) && (gameManager.engine != null) && (gameManager.engine.length > 0) && (gameManager.engine[0] != null)) {
			if(pause && !enableframestep)
				strTitle = "[PAUSE] NullpoMino - " + modeName;
			else if(gameManager.engine[0].isInGame && !gameManager.replayMode && !gameManager.replayRerecord)
				strTitle = "[PLAY] NullpoMino - " + modeName;
			else if(gameManager.replayMode && gameManager.replayRerecord)
				strTitle = "[RERECORD] NullpoMino - " + modeName;
			else if(gameManager.replayMode && !gameManager.replayRerecord)
				strTitle = "[REPLAY] NullpoMino - " + modeName;
			else
				strTitle = "[MENU] NullpoMino - " + modeName;
		}

		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, strTitle);
	}

	/*
	 * Called when leaving this state
	 */
	@Override
	public void leave() {
		gameManager.shutdown();
		gameManager = null;
		NullpoMinoSDL.disableAutoInputUpdate = false;
		NullpoMinoSDL.isInGame = false;
	}

	/*
	 * Draw the game screen
	 */
	@Override
	public void render() {
		if(gameManager != null) {
			gameManager.renderAll();

			if((gameManager.engine.length > 0) && (gameManager.engine[0] != null)) {
				int offsetX = gameManager.receiver.getFieldDisplayPositionX(gameManager.engine[0], 0);
				int offsetY = gameManager.receiver.getFieldDisplayPositionY(gameManager.engine[0], 0);

				// Pause menu
				if(pause && !enableframestep && !pauseMessageHide) {
					NormalFontSDL.printFont(offsetX + 12, offsetY + 188 + (cursor * 16), "b", NormalFontSDL.COLOR_RED);

					NormalFontSDL.printFont(offsetX + 28, offsetY + 188, "CONTINUE", (cursor == 0));
					NormalFontSDL.printFont(offsetX + 28, offsetY + 204, "RETRY", (cursor == 1));
					NormalFontSDL.printFont(offsetX + 28, offsetY + 220, "END", (cursor == 2));
					if(gameManager.replayMode && !gameManager.replayRerecord)
						NormalFontSDL.printFont(offsetX + 28, offsetY + 236, "RERECORD", (cursor == 3));
				}
				// Fast forward
				if(fastforward != 0)
					NormalFontSDL.printFont(offsetX, offsetY + 376, "e" + (fastforward + 1), NormalFontSDL.COLOR_ORANGE);
				if(gameManager.replayShowInvisible)
					NormalFontSDL.printFont(offsetX, offsetY + 392, "SHOW INVIS", NormalFontSDL.COLOR_ORANGE);
			}
		}
	}

	/*
	 * Update game state
	 */
	@Override
	public void update() {
		// Update key input states
		for(int i = 0; i < 2; i++) {
			int joynum = NullpoMinoSDL.joyUseNumber[i];

			boolean ingame = (gameManager != null) && (gameManager.engine.length > i) &&
							 (gameManager.engine[i] != null) && (gameManager.engine[i].isInGame) &&
							 (!pause || enableframestep);

			if((NullpoMinoSDL.joystickMax > 0) && (joynum >= 0) && (joynum < NullpoMinoSDL.joystickMax)) {
				GameKeySDL.gamekey[i].update(
						NullpoMinoSDL.keyPressedState,
						NullpoMinoSDL.joyPressedState[joynum],
						NullpoMinoSDL.joyAxisX[joynum],
						NullpoMinoSDL.joyAxisY[joynum],
						NullpoMinoSDL.joyHatState[joynum],
						ingame);
			} else {
				GameKeySDL.gamekey[i].update(NullpoMinoSDL.keyPressedState, ingame);
			}
		}

		// Title bar update
		if((gameManager != null) && (gameManager.engine != null) && (gameManager.engine.length > 0) && (gameManager.engine[0] != null)) {
			boolean nowInGame = gameManager.engine[0].isInGame;
			if(prevInGameFlag != nowInGame) {
				prevInGameFlag = nowInGame;
				updateTitleBarCaption();
			}
		}

		// Pause
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_PAUSE) || GameKeySDL.gamekey[1].isPushKey(GameKeySDL.BUTTON_PAUSE)) {
			if(!pause) {
				if((gameManager != null) && (gameManager.isGameActive()) && (pauseFrame <= 0)) {
					ResourceHolderSDL.soundManager.play("pause");
					pause = true;
					cursor = 0;
					if(!enableframestep) {
						pauseFrame = 5;
						ResourceHolderSDL.bgmPause();
					}
				}
			} else {
				ResourceHolderSDL.soundManager.play("pause");
				pause = false;
				pauseFrame = 0;
				if(!enableframestep) ResourceHolderSDL.bgmResume();
			}
			updateTitleBarCaption();
		}
		// Pause menu
		else if(pause && !enableframestep && !pauseMessageHide) {
			int maxPauseCursor = (gameManager.replayMode && !gameManager.replayRerecord) ? 3 : 2;

			// Mouse wheel and clicks track the same cursor the keyboard does.
			// Polling every pause frame keeps MouseInputSDL's hold counters
			// fresh so a click registers on the 0→1 transition.
			MouseInputSDL.mouseInput.update();
			boolean mouseConfirm = false;
			boolean mouseCancel = MouseInputSDL.mouseInput.isMouseBackClicked()
					|| MouseInputSDL.mouseInput.isMouseRightClicked();

			int wheel = (int) NullpoMinoSDL.mouseWheelDelta;
			if(wheel != 0) {
				int cycle = maxPauseCursor + 1;
				cursor = ((cursor - wheel) % cycle + cycle) % cycle;
				ResourceHolderSDL.soundManager.play("cursor");
			}

			// Cursor marker at offsetX+12, text at offsetX+28, widest item is
			// 8 chars wide → 144 px covers everything. Rows are 16 px tall
			// starting at offsetY+188.
			int pauseOffsetX = 0, pauseOffsetY = 0;
			if(gameManager != null && gameManager.engine.length > 0 && gameManager.engine[0] != null) {
				pauseOffsetX = gameManager.receiver.getFieldDisplayPositionX(gameManager.engine[0], 0);
				pauseOffsetY = gameManager.receiver.getFieldDisplayPositionY(gameManager.engine[0], 0);
			}
			int menuLeft = pauseOffsetX + 12;
			int menuTop = pauseOffsetY + 188;
			int menuRight = menuLeft + 144;
			int menuBottom = menuTop + (maxPauseCursor + 1) * 16;
			int mx = MouseInputSDL.mouseInput.getMouseX();
			int my = MouseInputSDL.mouseInput.getMouseY();

			// Hover: slide the cursor to the row under the pointer.
			if(MouseInputSDL.mouseInput.isMouseMoved()
					&& mx >= menuLeft && mx < menuRight
					&& my >= menuTop && my < menuBottom) {
				int row = (my - menuTop) / 16;
				if(row >= 0 && row <= maxPauseCursor && row != cursor) {
					cursor = row;
					ResourceHolderSDL.soundManager.play("cursor");
				}
			}

			// Click confirms the row under the pointer.
			if(MouseInputSDL.mouseInput.isMouseClicked()
					&& mx >= menuLeft && mx < menuRight
					&& my >= menuTop && my < menuBottom) {
				int row = (my - menuTop) / 16;
				if(row >= 0 && row <= maxPauseCursor) {
					if(row != cursor) ResourceHolderSDL.soundManager.play("cursor");
					cursor = row;
					mouseConfirm = true;
				}
			}

			// Cursor movement
			if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_UP)) {
				cursor--;

				if(cursor < 0) {
					if(gameManager.replayMode && !gameManager.replayRerecord)
						cursor = 3;
					else
						cursor = 2;
				}

				ResourceHolderSDL.soundManager.play("cursor");
			}
			if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_DOWN)) {
				cursor++;
				if(cursor > 3) cursor = 0;

				if((!gameManager.replayMode || gameManager.replayRerecord) && (cursor > 2))
					cursor = 0;

				ResourceHolderSDL.soundManager.play("cursor");
			}

			// Confirm
			if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_A) || mouseConfirm) {
				ResourceHolderSDL.soundManager.play("decide");
				if(cursor == 0) {
					// Resumption
					pause = false;
					pauseFrame = 0;
					GameKeySDL.gamekey[0].clear();
					ResourceHolderSDL.bgmResume();
				} else if(cursor == 1) {
					// Retry
					ResourceHolderSDL.bgmStop();
					pause = false;
					gameManager.reset();
				} else if(cursor == 2) {
					// End — walk back through the menus that launched the
					// game (rule select, mode select, title) via the shared
					// back stack instead of jumping straight to title.
					ResourceHolderSDL.bgmStop();
					NullpoMinoSDL.goBack();
					return;
				} else if(cursor == 3) {
					// Replay re-record
					gameManager.replayRerecord = true;
					ResourceHolderSDL.soundManager.play("tspin1");
					cursor = 0;
				}
				updateTitleBarCaption();
			}
			// Unpause by cancel key or mouse back / right-click
			else if((GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_B) || mouseCancel) && (pauseFrame <= 0)) {
				ResourceHolderSDL.soundManager.play("pause");
				pause = false;
				pauseFrame = 5;
				GameKeySDL.gamekey[0].clear();
				ResourceHolderSDL.bgmResume();
				updateTitleBarCaption();
			}
		}
		if(pauseFrame > 0) pauseFrame--;

		// Hide pause menu
		pauseMessageHide = GameKeySDL.gamekey[0].isPressKey(GameKeySDL.BUTTON_C);

		if(gameManager.replayMode && !gameManager.replayRerecord && gameManager.engine[0].gameActive) {
			// Replay speed
			if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_LEFT)) {
				if(fastforward > 0) {
					fastforward--;
				}
			}
			if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_RIGHT)) {
				if(fastforward < 98) {
					fastforward++;
				}
			}

			// Replay re-record
			if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_D)) {
				gameManager.replayRerecord = true;
				ResourceHolderSDL.soundManager.play("tspin1");
				cursor = 0;
			}
			// Replay re-record
			if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_E)) {
				gameManager.replayShowInvisible = !gameManager.replayShowInvisible;
				ResourceHolderSDL.soundManager.play("tspin1");
				cursor = 0;
			}
		} else {
			fastforward = 0;
		}

		if(gameManager != null) {
			// BGM
			if(ResourceHolderSDL.bgmPlaying != gameManager.bgmStatus.bgm) {
				ResourceHolderSDL.bgmStart(gameManager.bgmStatus.bgm);
			}
			if(ResourceHolderSDL.bgmIsPlaying()) {
				int basevolume = NullpoMinoSDL.propConfig.getProperty("option.bgmvolume", 128);
				float basevolume2 = (float)basevolume / 128;
				int newvolume = (int)(128 * (gameManager.bgmStatus.volume * basevolume2));
				if(newvolume < 0) newvolume = 0;
				if(newvolume > 128) newvolume = 128;
				if(NullpoMinoSDL.mixerLib != null && ResourceHolderSDL.bgmTrack != null)
					NullpoMinoSDL.mixerLib.MIX_SetTrackGain(ResourceHolderSDL.bgmTrack, newvolume / 128.0f);
				if(newvolume <= 0) ResourceHolderSDL.bgmStop();
			}
		}

		// Result screen mouse + Escape input. Slides statc[0] (the
		// RETRY/END selector) on hover, confirms on click, and treats
		// Escape / mouse back / right-click as "pick END". Done before
		// updateAll() so the engine sees the new statc[0] in the same
		// frame; quitflag is honoured by the "Return to title" check
		// further down.
		if(gameManager != null && !pause) {
			boolean anyResult = false;
			for(int i = 0; i < gameManager.getPlayers(); i++) {
				GameEngine engine = gameManager.engine[i];
				if(engine != null && engine.stat == GameEngine.Status.RESULT) { anyResult = true; break; }
			}
			if(anyResult) {
				MouseInputSDL.mouseInput.update();
				boolean cancelEnd = NullpoMinoSDL.isEscapePushedThisFrame()
						|| MouseInputSDL.mouseInput.isMouseBackClicked()
						|| MouseInputSDL.mouseInput.isMouseRightClicked();
				for(int i = 0; i < gameManager.getPlayers(); i++) {
					GameEngine engine = gameManager.engine[i];
					if(engine == null || engine.stat != GameEngine.Status.RESULT) continue;
					handleResultMouse(engine, i);
					if(cancelEnd) {
						ResourceHolderSDL.soundManager.play("decide");
						engine.quitflag = true;
					}
				}
			}
		}

		// Execute game loops
		if(!pause || (GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_FRAMESTEP) && enableframestep)) {
			if(gameManager != null) {
				for(int i = 0; i < Math.min(gameManager.getPlayers(), 2); i++) {
					if(!gameManager.replayMode || gameManager.replayRerecord || !gameManager.engine[i].gameActive) {
						GameKeySDL.gamekey[i].inputStatusUpdate(gameManager.engine[i].ctrl);
					}
				}

				// Pre-game SETTING screen mouse + Escape. Hover slides the
				// menu cursor row-by-row, wheel cycles the current item's
				// value (LEFT/RIGHT), click confirms (BUTTON_A — usually
				// "start the game"), and right-click / mouse back / Escape
				// cancels (BUTTON_B). Synthetic button presses are OR'd
				// into ctrl.buttonPress[] before updateAll() so the mode's
				// onSetting reads them via the standard isPush /
				// isMenuRepeatKey path.
				if(gameManager.engine.length > 0
						&& gameManager.engine[0] != null
						&& gameManager.engine[0].stat == GameEngine.Status.SETTING
						&& gameManager.mode != null) {
					injectSettingMouseInput(gameManager.engine[0]);
				}

				for(int i = 0; i <= fastforward; i++) gameManager.updateAll();
			}
		}

		// Mouse back button while a replay is playing acts like Escape /
		// BUTTON_GIVEUP — return to the previous screen. Skipped when an
		// engine is in SETTING or RESULT or the game is paused, since those
		// branches above already consume the click for their own cancel
		// handling.
		boolean replayMouseBack = false;
		if(shouldPollReplayBack(gameManager, pause)) {
			MouseInputSDL.mouseInput.update();
			replayMouseBack = MouseInputSDL.mouseInput.isMouseBackClicked();
		}

		if(gameManager != null) {
			// Retry button
			if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_RETRY) || GameKeySDL.gamekey[1].isPushKey(GameKeySDL.BUTTON_RETRY)) {
				ResourceHolderSDL.bgmStop();
				pause = false;
				gameManager.reset();
			}

			// Return to title
			if(gameManager.getQuitFlag() ||
			   GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_GIVEUP) ||
			   GameKeySDL.gamekey[1].isPushKey(GameKeySDL.BUTTON_GIVEUP) ||
			   replayMouseBack)
			{
				ResourceHolderSDL.bgmStop();
				NullpoMinoSDL.goBack();
				return;
			}
		}
	}

	/**
	 * Whether the in-game update loop should poll the mouse back button
	 * for "exit replay playback". Hoisted out so the gating predicate
	 * (replay-only, not re-recording, not paused, no engine in
	 * SETTING/RESULT — those branches already consume the click) is
	 * directly testable without driving the rest of {@code update()}.
	 */
	static boolean shouldPollReplayBack(GameManager gameManager, boolean pause) {
		if(gameManager == null) return false;
		if(!gameManager.replayMode) return false;
		if(gameManager.replayRerecord) return false;
		if(pause) return false;
		for(int i = 0; i < gameManager.getPlayers(); i++) {
			GameEngine engine = gameManager.engine[i];
			if(engine != null && (engine.stat == GameEngine.Status.RESULT || engine.stat == GameEngine.Status.SETTING)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Synthesize controller input from mouse + Escape on the pre-game
	 * SETTING screen. Hover updates the mode's cursor directly via
	 * {@link nullpomino.game.subsystem.mode.GameMode#setMenuCursor};
	 * wheel / click / cancel are OR'd into ctrl.buttonPress[] (which
	 * inputStatusUpdate just populated from the keyboard) so keyboard and
	 * mouse can drive the same menu without one clobbering the other.
	 */
	private void injectSettingMouseInput(GameEngine engine) {
		MouseInputSDL.mouseInput.update();
		Controller ctrl = engine.ctrl;

		// Hover: each menu item is rendered as two rows (label + value),
		// 16 px per row, so item index = (mouseY - baseY) / 32. baseY
		// matches drawMenuFont's offsetY + 4 / + 52 split. We only set the
		// cursor when the mode exposes one (getMenuCursor != -1) and only
		// when the row actually changed to avoid fighting keyboard input
		// every frame.
		if(MouseInputSDL.mouseInput.isMouseMoved()) {
			int currentCursor = gameManager.mode.getMenuCursor();
			if(currentCursor >= 0) {
				int offsetY = gameManager.receiver.getFieldDisplayPositionY(engine, 0);
				int baseY = offsetY + (engine.displaysize == -1 ? 4 : 52);
				int my = MouseInputSDL.mouseInput.getMouseY();
				if(my >= baseY) {
					int item = (my - baseY) / 32;
					int itemMax = gameManager.mode.getMenuItemCount();
					int upper = itemMax > 0 ? itemMax - 1 : 19;
					if(item >= 0 && item <= upper && item != currentCursor) {
						gameManager.mode.setMenuCursor(item);
						ResourceHolderSDL.soundManager.play("cursor");
					}
				}
			}
		}

		// Wheel cycles the value of the highlighted item. Up = forward
		// (BUTTON_RIGHT, "next / increase"); down = backward (BUTTON_LEFT,
		// "prev / decrease"). The mode's onSetting plays its own "change"
		// SE in response.
		int wheel = (int) NullpoMinoSDL.mouseWheelDelta;
		if(wheel > 0) ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		else if(wheel < 0) ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		if(MouseInputSDL.mouseInput.isMouseClicked()) {
			ctrl.buttonPress[Controller.BUTTON_A] = true;
		}

		if(MouseInputSDL.mouseInput.isMouseBackClicked()
				|| MouseInputSDL.mouseInput.isMouseRightClicked()
				|| NullpoMinoSDL.isEscapePushedThisFrame()) {
			ctrl.buttonPress[Controller.BUTTON_B] = true;
		}
	}

	/**
	 * Mouse input handler for {@link GameEngine.Status#RESULT}. Mirrors the
	 * coordinates RendererSDL uses to draw the RETRY / END buttons (both at
	 * y=offsetY+340, RETRY at offsetX+12 width 80, END at offsetX+108 width
	 * 48). Hover slides statc[0] under the pointer; click confirms by
	 * setting quitflag (END) or kicking the manager into reset (RETRY) —
	 * the surrounding update() loop handles both.
	 */
	private void handleResultMouse(GameEngine engine, int playerID) {
		int offsetX = gameManager.receiver.getFieldDisplayPositionX(engine, playerID);
		int offsetY = gameManager.receiver.getFieldDisplayPositionY(engine, playerID);
		int rowTop = offsetY + 340;
		int rowBottom = rowTop + 16;
		int retryLeft = offsetX + 12, retryRight = retryLeft + 80;
		int endLeft = offsetX + 108, endRight = endLeft + 48;

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		if(my < rowTop || my >= rowBottom) return;

		int hovered = -1;
		if(mx >= retryLeft && mx < retryRight) hovered = 0;
		else if(mx >= endLeft && mx < endRight) hovered = 1;
		if(hovered < 0) return;

		if(MouseInputSDL.mouseInput.isMouseMoved() && engine.statc[0] != hovered) {
			engine.statc[0] = hovered;
			ResourceHolderSDL.soundManager.play("cursor");
		}
		if(MouseInputSDL.mouseInput.isMouseClicked()) {
			engine.statc[0] = hovered;
			ResourceHolderSDL.soundManager.play("decide");
			if(hovered == 0) gameManager.reset();
			else engine.quitflag = true;
		}
	}
}
