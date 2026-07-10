// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nullpomino.game.component.Controller;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.ai.DummyAI;
import nullpomino.game.mode.GameMode;
import nullpomino.game.wallkick.Wallkick;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.widget.ButtonSDL;
import nullpomino.gui.sdl.widget.WidgetSDL;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;
import nullpomino.game.randomizer.Randomizer;

/**
 * Game screen state (Local play)
 */
public class StateInGameSDL extends BaseStateSDL {
	/** Log */
	static Logger log = LoggerFactory.getLogger(StateInGameSDL.class);

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

	/**
	 * Top-right "back" close button, shown on the pre-game SETTING screen
	 * (mode.onSetting()/renderSetting() - e.g. PRACTICE MODE SETTINGS) and
	 * while watching a replay (shouldPollReplayBack), but hidden during
	 * normal gameplay. No inline action: on SETTING it folds into the
	 * synthetic BUTTON_B press in injectSettingMouseInput() so it goes
	 * through the same per-mode cancel/quitflag logic as Escape; during
	 * replay playback its click feeds replayMouseBack -> goBack().
	 */
	private ButtonSDL closeBtn;

	/**
	 * Replay playback frozen by the timeline's pause button. Separate from
	 * {@link #pause}: this one halts updateAll() without opening the pause
	 * menu, so the board stays fully visible while scrubbing.
	 */
	protected boolean replayPaused = false;

	/** True while the timeline handle is being dragged. */
	private boolean scrubbing = false;

	/** Preview frame while dragging; committed via seekReplay() on release. */
	private int scrubTarget = 0;

	/**
	 * The pre-game settings screen isn't in the recorded frame count
	 * (replayTimer stays 0 during SETTING), so its position is tracked live:
	 * {@code settingElapsed} is the current frame within the settings screen and
	 * {@code settingFrames} is its full length (the running max, used for the
	 * SETTING band width and the extended timeline domain). prevTickSetting
	 * tracks the SETTING edge so a fresh settings screen restarts the count.
	 */
	private int settingFrames = 0;
	private int settingElapsed = 0;
	private boolean prevTickSetting = false;

	/**
	 * Replay play/pause button. Label stays empty — the icon is drawn as an
	 * overlay in render() because ButtonSDL's label path runs safeString, which
	 * would mangle the atlas's arrow glyph 'b'.
	 */
	private ButtonSDL playPauseBtn;

	/** Timeline bar geometry, recomputed each frame from the field position. */
	private int barX, barW;
	/**
	 * One bottom row: the play/pause button hugging the left screen edge (below
	 * the mode's feedback text like "DOUBLE") and the bar filling the rest of
	 * the row to near the right screen edge. The current frame number (the
	 * mode already shows the clock as TIME in the score area) sits just above
	 * the bar's right end, right-aligned so growing digits extend leftward
	 * and never run off screen. Bar is vertically centered on the button.
	 */
	private static final int BTN_W = 28, BTN_H = 20, BTN_Y = 458;
	/** Gap on each side of the bar: button->bar (left) and bar->screen edge (right). */
	private static final int BAR_MARGIN = 8;
	private static final int BAR_Y = BTN_Y + (BTN_H - 8) / 2, BAR_H = 8;
	/** Taller hit zone than the 8px track so the bar is easy to grab. */
	private static final int BAR_HIT_TOP = BTN_Y - 2, BAR_HIT_BOTTOM = BTN_Y + BTN_H + 2;

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
		closeBtn = ButtonSDL.newCloseButton(null);
		replayPaused = false;
		scrubbing = false;
		settingFrames = 0;
		settingElapsed = 0;
		prevTickSetting = false;
		playPauseBtn = new ButtonSDL(0, BTN_Y, BTN_W, BTN_H, "");
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
			log.warn("Couldn't find mode:{}", modeName);
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
				log.debug("Load rule options from {}", rulename);
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
				gameManager.engine[i].aiUseThread = NullpoMinoSDL.propGlobal.getProperty(i + ".aiUseThread", true)
						&& !NullpoMinoSDL.webMode;
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
		replayPaused = false;
		scrubbing = false;

		gameManager.receiver.setGraphics(NullpoMinoSDL.renderer);

		// Mode
		modeName = prop.getProperty("name.mode", "");
		GameMode modeObj = NullpoMinoSDL.modeManager.getMode(modeName);
		if(modeObj == null) {
			log.warn("Couldn't find mode:{}", modeName);
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
				gameManager.engine[i].aiUseThread = NullpoMinoSDL.propGlobal.getProperty(i + ".aiUseThread", true)
						&& !NullpoMinoSDL.webMode;
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
		String modeTitle = NullpoMinoSDL.GAME_NAME + " - " + modeName;
		String strTitle = modeTitle;

		if((gameManager != null) && (gameManager.engine != null) && (gameManager.engine.length > 0) && (gameManager.engine[0] != null)) {
			if(pause && !enableframestep)
				strTitle = "[PAUSE] " + modeTitle;
			else if(gameManager.engine[0].isInGame && !gameManager.replayMode && !gameManager.replayRerecord)
				strTitle = "[PLAY] " + modeTitle;
			else if(gameManager.replayMode && gameManager.replayRerecord)
				strTitle = "[RERECORD] " + modeTitle;
			else if(gameManager.replayMode && !gameManager.replayRerecord)
				strTitle = "[REPLAY] " + modeTitle;
			else
				strTitle = "[MENU] " + modeTitle;
		}

		SDL3.INSTANCE.SDL_SetWindowTitle(NullpoMinoSDL.window, strTitle);
	}

	/*
	 * Called when leaving this state
	 */
	@Override
	public void leave() {
		if(gameManager != null) {
			gameManager.shutdown();
		}
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

				if(gameManager.engine[0].stat == GameEngine.Status.SETTING
						|| shouldPollReplayBack(gameManager, pause)) {
					closeBtn.render();
				}

				if(shouldRenderReplayTimeline(gameManager, pause) && replayTotalFrames() > 0) {
					renderReplayTimeline();
				}
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
					replayPaused = false;
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
				adjustFastforward(-1);
			}
			if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_RIGHT)) {
				adjustFastforward(1);
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

		// Execute game loops. Suspended while scrubbing: the drag's live seek
		// is the only thing moving the engine, otherwise playback would
		// advance past the handle every frame and force a fresh
		// reset+re-simulate on each tick.
		if((!pause && !replayPaused && !scrubbing) || (GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_FRAMESTEP) && enableframestep)) {
			if(gameManager != null) {
				// Track the settings-screen position for the timeline. Advances
				// only on a live tick (never during pause; the seek re-sim loops
				// call updateAll() directly and set these themselves). settingFrames
				// is a running max so the domain doesn't shrink on re-entry.
				// ponytail: the band grows from 0 during the very first settings
				// screen (~2% domain wobble over ~1s), then locks in.
				if(gameManager.replayMode) {
					GameEngine e0 = gameManager.engine[0];
					boolean setting = e0 != null && e0.stat == GameEngine.Status.SETTING;
					if(setting) {
						settingElapsed = prevTickSetting ? settingElapsed + 1 : 0;
						if(settingElapsed > settingFrames) settingFrames = settingElapsed;
					}
					prevTickSetting = setting;
				}

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

				// Post-game RESULT screen: wheel and PageUp/PageDown drive
				// the per-mode page list (FINAL, GRADE MANIA, MARATHON+, ...)
				// the same way arrow keys do. Done before updateAll() so the
				// engine sees the synthetic press in the same frame.
				for(int i = 0; i < Math.min(gameManager.getPlayers(), 2); i++) {
					GameEngine engine = gameManager.engine[i];
					if(engine != null && engine.stat == GameEngine.Status.RESULT) {
						injectResultMouseInput(engine);
					}
				}

				for(int i = 0; i <= fastforward; i++) gameManager.updateAll();
			}
		}

		// Clicking the top-right BACK button (closeBtn) or the mouse back
		// button while a replay is playing acts like Escape / BUTTON_GIVEUP —
		// return to the previous screen. Skipped when an engine is in SETTING
		// or RESULT or the game is paused, since those branches above already
		// consume the click for their own cancel handling.
		boolean replayMouseBack = false;
		if(shouldPollReplayBack(gameManager, pause)) {
			MouseInputSDL.mouseInput.update();
			boolean closeClicked = closeBtn.update(
					MouseInputSDL.mouseInput.getMouseX(),
					MouseInputSDL.mouseInput.getMouseY(),
					MouseInputSDL.mouseInput.isMouseClicked());
			replayMouseBack = closeClicked || MouseInputSDL.mouseInput.isMouseBackClicked();
			updateReplayTimeline();
		} else if(shouldShowReplayTimeline(gameManager, pause)) {
			// An engine is on RESULT: its mouse handler above already ran
			// mouseInput.update() this frame (a second call would eat the
			// click edge), so only poll the timeline here. Scrubbing back
			// from the result screen re-simulates into live playback.
			updateReplayTimeline();
		} else if((scrubbing || replayPaused) && shouldRenderReplayTimeline(gameManager, pause)) {
			// The timeline sits on the SETTING screen, reached by dragging into
			// the violet band. SETTING is normally unpolled, but poll it here
			// while a drag is in progress OR playback is paused — otherwise a
			// drag can't finish and a paused viewer is stranded on the settings
			// screen with a dead bar (the tick, which auto-advances SETTING, is
			// suspended in both cases). Safe precisely because the settings
			// menu's own mouse handler lives in that suspended tick block, so
			// nothing else updated the mouse this frame — do it here.
			MouseInputSDL.mouseInput.update();
			updateReplayTimeline();
		}

		if(gameManager != null) {
			// Retry button
			if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_RETRY) || GameKeySDL.gamekey[1].isPushKey(GameKeySDL.BUTTON_RETRY)) {
				ResourceHolderSDL.bgmStop();
				pause = false;
				replayPaused = false;
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
		if(!shouldShowReplayTimeline(gameManager, pause)) return false;
		for(int i = 0; i < gameManager.getPlayers(); i++) {
			GameEngine engine = gameManager.engine[i];
			if(engine != null && engine.stat == GameEngine.Status.RESULT) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether the replay timeline (scrub bar + transport buttons) should be
	 * shown and polled. Wider than {@link #shouldPollReplayBack}: RESULT is
	 * allowed so a finished replay can be scrubbed back without RETRYing —
	 * the result screen's own mouse handling (RETRY/END row at
	 * offsetY+340..356) doesn't reach the bottom timeline row. SETTING stays
	 * excluded; its click-to-confirm would fight the bar.
	 */
	static boolean shouldShowReplayTimeline(GameManager gameManager, boolean pause) {
		if(!shouldRenderReplayTimeline(gameManager, pause)) return false;
		for(int i = 0; i < gameManager.getPlayers(); i++) {
			GameEngine engine = gameManager.engine[i];
			if(engine != null && engine.stat == GameEngine.Status.SETTING) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether to DRAW the replay timeline. Wider than
	 * {@link #shouldShowReplayTimeline}: SETTING is allowed so the bar (with its
	 * distinct SETTING band) is visible during the pre-game settings screen.
	 * The poll paths still use {@link #shouldShowReplayTimeline}, so the bar
	 * shows but isn't interactive during SETTING — clicks there belong to the
	 * settings menu ({@link #injectSettingMouseInput}), not the scrubber.
	 */
	static boolean shouldRenderReplayTimeline(GameManager gameManager, boolean pause) {
		if(gameManager == null) return false;
		if(!gameManager.replayMode) return false;
		if(gameManager.replayRerecord) return false;
		if(pause) return false;
		return true;
	}

	/**
	 * Synthesize controller input from mouse + Escape on the pre-game
	 * SETTING screen. Hover updates the mode's cursor directly via
	 * {@link nullpomino.game.mode.GameMode#setMenuCursor};
	 * wheel / click / cancel are OR'd into ctrl.buttonPress[] (which
	 * inputStatusUpdate just populated from the keyboard) so keyboard and
	 * mouse can drive the same menu without one clobbering the other.
	 */
	private void injectSettingMouseInput(GameEngine engine) {
		MouseInputSDL.mouseInput.update();
		Controller ctrl = engine.ctrl;

		// Hover: slide the cursor to the item under the pointer. The
		// pixel->item math lives in settingHoverItem, which mirrors
		// drawMenuFont's layout and defers the row->item step to the mode
		// (layouts differ: two-row label/value vs. PracticeMode's single row
		// per item on a full-screen menuOnly page). We only steer a mode
		// that exposes a cursor (getMenuCursor != -1) and only when the item
		// actually changed, so we don't fight keyboard input every frame.
		if(MouseInputSDL.mouseInput.isMouseMoved()) {
			int currentCursor = gameManager.mode.getMenuCursor();
			if(currentCursor >= 0) {
				int offsetY = gameManager.receiver.getFieldDisplayPositionY(engine, 0);
				int my = MouseInputSDL.mouseInput.getMouseY();
				int item = settingHoverItem(gameManager.mode, my, offsetY,
						engine.owner.menuOnly, engine.displaysize == -1);
				if(item >= 0 && item != currentCursor) {
					gameManager.mode.setMenuCursor(item);
					ResourceHolderSDL.soundManager.play("cursor");
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

		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();
		boolean closeClicked = closeBtn.update(MouseInputSDL.mouseInput.getMouseX(), MouseInputSDL.mouseInput.getMouseY(), clicked);

		if(clicked && !closeClicked) {
			ctrl.buttonPress[Controller.BUTTON_A] = true;
		}

		if(closeClicked
				|| MouseInputSDL.mouseInput.isMouseBackClicked()
				|| MouseInputSDL.mouseInput.isMouseRightClicked()
				|| NullpoMinoSDL.isEscapePushedThisFrame()) {
			ctrl.buttonPress[Controller.BUTTON_B] = true;
		}
	}

	/**
	 * Pure mapping from a mouse Y (screen pixels) to the SETTING-screen menu
	 * item under the pointer, or -1 when the pointer isn't over a selectable
	 * row. Hoisted out of {@link #injectSettingMouseInput} so the geometry
	 * is unit-testable without an SDL window.
	 *
	 * <p>Mirrors {@link RendererSDL#drawMenuFont}'s vertical layout: when
	 * {@code menuOnly}, text is drawn at raw {@code row * 16} with no field
	 * offset (the full-screen menu {@link nullpomino.game.mode.PracticeMode}
	 * uses); otherwise the field offset plus the small-display split
	 * (+4 when {@code smallDisplay}, else +52) applies. Rows are 16 px tall
	 * either way. The row->item translation is delegated to
	 * {@link GameMode#getMenuItemForRow} so per-mode layouts stay with the
	 * mode.
	 */
	static int settingHoverItem(GameMode mode, int mouseY, int offsetY,
			boolean menuOnly, boolean smallDisplay) {
		int baseY = menuOnly ? 0 : offsetY + (smallDisplay ? 4 : 52);
		if(mouseY < baseY) return -1;
		int row = (mouseY - baseY) / 16;
		return mode.getMenuItemForRow(row);
	}

	/**
	 * Synthesize page-flip input on the post-game RESULT screen. Mouse
	 * wheel and Page Up / Page Down map to BUTTON_UP / BUTTON_DOWN, the
	 * same buttons the mode's onResult already polls for arrow-key
	 * pagination. Run after {@code inputStatusUpdate} so we OR onto the
	 * keyboard state instead of clobbering the player's UP/DOWN press.
	 */
	private void injectResultMouseInput(GameEngine engine) {
		boolean pageUp = NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_PAGEUP];
		boolean pageDown = NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_PAGEDOWN];
		applyResultPageInputs(engine.ctrl, (int) NullpoMinoSDL.mouseWheelDelta, pageUp, pageDown);
	}

	/**
	 * Pure mapping from wheel / Page Up / Page Down state to
	 * {@code ctrl.buttonPress[BUTTON_UP/DOWN]}. Hoisted out of
	 * {@link #injectResultMouseInput} so the keyboard / wheel routing is
	 * unit-testable without an SDL window. OR semantics: a held arrow key
	 * already populated by {@code inputStatusUpdate} stays pressed.
	 *
	 * <p>One wheel tick = one button press regardless of magnitude, matching
	 * the SETTING-screen wheel behavior in {@link #injectSettingMouseInput}.
	 * Result pages only number 2–3 deep, so multi-tick scrolls would skip
	 * across the whole list in confusing ways.
	 */
	static void applyResultPageInputs(Controller ctrl, int wheel, boolean pageUp, boolean pageDown) {
		if(wheel > 0 || pageUp) ctrl.buttonPress[Controller.BUTTON_UP] = true;
		if(wheel < 0 || pageDown) ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
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
			if(hovered == 0) { replayPaused = false; gameManager.reset(); }
			else engine.quitflag = true;
		}
	}

	/**
	 * Total recorded frames of the replay being watched, i.e. the highest
	 * value {@code engine[0].replayTimer} will reach. 0 when unavailable.
	 */
	private int replayTotalFrames() {
		GameEngine eng = gameManager.engine[0];
		if(eng == null || eng.replayData == null || eng.replayData.inputDataArray == null) return 0;
		return eng.replayData.inputDataArray.size();
	}

	/**
	 * Position the bottom row: buttons hug the left screen edge, the bar
	 * fills the rest of the row to near the right edge.
	 */
	private void layoutReplayTimeline() {
		playPauseBtn.x = 4;
		// Equal gap on both sides of the bar: button->bar == bar->right edge.
		barX = playPauseBtn.x + BTN_W + BAR_MARGIN;
		barW = NullpoMinoSDL.LOGICAL_WIDTH - BAR_MARGIN - barX;
	}

	/** Pixel width of {@code frames} within the current bar's extended domain. */
	private int pxOf(int frames, int domain) {
		return (barW > 0 && domain > 0) ? (int)((long)barW * frames / domain) : 0;
	}

	/** Applies one LEFT/RIGHT-key-equivalent step to the replay speed, clamped to [0, 98]. */
	private void adjustFastforward(int delta) {
		fastforward = Math.max(0, Math.min(98, fastforward + delta));
	}

	/**
	 * Draw the timeline: track, the two pre-game phase bands (violet SETTING,
	 * amber READY), the blue gameplay progress fill, the drag handle, the frame
	 * count, and the transport button.
	 *
	 * <p>The bar spans an extended domain {@code settingFrames + total}: the
	 * settings screen (violet) isn't in the recorded frame count (replayTimer
	 * stays 0 there), READY (amber) is in-domain at replayTimer {@code [0,
	 * goEnd)}, and gameplay (blue) fills the rest. Scrubbing maps pointer pixels
	 * to this domain and {@link #seekDomain} routes the settings prefix back to
	 * the settings screen, so dragging left re-displays it.
	 */
	private void renderReplayTimeline() {
		layoutReplayTimeline();
		GameEngine eng = gameManager.engine[0];
		int total  = replayTotalFrames();
		int setF   = settingFrames;
		int goEnd  = eng.goEnd;                 // READY->MOVE boundary, in replayTimer frames
		int domain = setF + total;

		// Handle/fill position in the extended domain.
		int cur = scrubbing ? scrubTarget : currentDomainPos();
		if(cur > domain) cur = domain;

		int setW    = pxOf(setF, domain);
		int readyW  = pxOf(setF + goEnd, domain) - setW;
		int gpStart = setF + goEnd;

		WidgetSDL.fillRect(barX, BAR_Y, barW, BAR_H, 0, 0, 0, 192);               // track
		WidgetSDL.fillRect(barX,        BAR_Y, setW,   BAR_H, 150, 100, 200, 255);// SETTING = violet
		WidgetSDL.fillRect(barX + setW, BAR_Y, readyW, BAR_H, 230, 180,  40, 255);// READY   = amber
		if(cur > gpStart) {                                                       // gameplay progress = blue
			int x0 = barX + pxOf(gpStart, domain), x1 = barX + pxOf(cur, domain);
			WidgetSDL.fillRect(x0, BAR_Y, x1 - x0, BAR_H, 0, 128, 255, 255);
		}
		WidgetSDL.drawRect(barX, BAR_Y, barW, BAR_H, 180, 180, 180, 255);         // border
		int handleX = barX + Math.max(0, Math.min(pxOf(cur, domain) - 2, barW - 4));
		WidgetSDL.fillRect(handleX, BAR_Y - 2, 4, BAR_H + 4, 255, 255, 255, 255); // handle

		// Playback position as a raw recorded frame count just above the bar's
		// right end. Right-aligned: new digits grow leftward, staying on screen.
		// Follows the drag position while scrubbing; reads 0 in the settings band.
		int textFrame = scrubbing ? Math.max(0, scrubTarget - setF) : eng.replayTimer;
		String frames = String.valueOf(textFrame);
		NormalFontSDL.printFont(barX + barW - frames.length() * 16, BAR_Y - 18, frames, NormalFontSDL.COLOR_WHITE);

		playPauseBtn.render();

		// Icon overlay. The atlas has only a right-pointing arrow ('b'); the
		// pause bars are plain rects.
		int gy = BTN_Y + (BTN_H - 16) / 2;
		int gxOff = (BTN_W - 16) / 2;
		if(replayPaused) {
			NormalFontSDL.printFont(playPauseBtn.x + gxOff, gy, "b", NormalFontSDL.COLOR_WHITE);
		} else {
			WidgetSDL.fillRect(playPauseBtn.x + BTN_W / 2 - 6, gy + 2, 4, 12, 255, 255, 255, 255);
			WidgetSDL.fillRect(playPauseBtn.x + BTN_W / 2 + 2, gy + 2, 4, 12, 255, 255, 255, 255);
		}
	}

	/**
	 * Poll the timeline bar and transport buttons. Assumes
	 * {@code MouseInputSDL.mouseInput.update()} was already called this frame
	 * (the surrounding shouldPollReplayBack block does it).
	 */
	private void updateReplayTimeline() {
		int total = replayTotalFrames();
		if(total <= 0) return;
		layoutReplayTimeline();

		int mx = MouseInputSDL.mouseInput.getMouseX();
		int my = MouseInputSDL.mouseInput.getMouseY();
		boolean clicked = MouseInputSDL.mouseInput.isMouseClicked();

		if(playPauseBtn.update(mx, my, clicked)) {
			replayPaused = !replayPaused;
		}

		// Wheel over the bar steps the paused playback one frame per tick
		// (up = forward). Dragging is too coarse for single frames on long
		// replays (one pixel covers total/barW frames), so this is the fine
		// control. Wheel over the buttons instead adjusts the replay speed,
		// mirroring the LEFT/RIGHT arrow keys' fastforward control (0..98).
		int wheel = (int) NullpoMinoSDL.mouseWheelDelta;
		boolean overBar = mx >= barX && mx < barX + barW && my >= BAR_HIT_TOP && my < BAR_HIT_BOTTOM;
		boolean overButtons = mx >= playPauseBtn.x && mx < playPauseBtn.x + BTN_W
				&& my >= BAR_HIT_TOP && my < BAR_HIT_BOTTOM;
		if(wheel != 0 && overBar) {
			replayPaused = true;
			seekDomain(currentDomainPos() + wheel);
		} else if(wheel != 0 && overButtons) {
			adjustFastforward(wheel);
		}

		// Drag-to-scrub: a press inside the hit zone grabs the handle and the
		// game live-seeks to the pointer every frame it moves, so the board
		// plays/rewinds under the drag. A plain click is a one-frame drag, so
		// click-to-jump falls out of the same path.
		if(clicked && overBar) {
			scrubbing = true;
		}
		if(scrubbing) {
			// getMouseX() reports -1 outside the logical viewport; keep the last
			// in-window position instead of snapping to frame 0. scrubTarget is a
			// position in the extended domain [0, settingFrames+total); seekDomain
			// routes the SETTING prefix to the settings screen and the rest to
			// replayTimer, so dragging left into the violet band re-displays the
			// pre-game settings.
			if(mx >= 0) scrubTarget = scrubFrameForX(mx, barX, barW, settingFrames + total);
			// ponytail: backward drags reset+re-simulate per changed target;
			// fine at ~40k-frame replays, add engine snapshots if it ever lags.
			if(scrubTarget != currentDomainPos()) {
				seekDomain(scrubTarget);
			}
			if(!MouseInputSDL.mouseInput.isMousePressed()) {
				scrubbing = false;
			}
		}
	}

	/**
	 * Pure px-&gt;frame mapping for the scrub bar, clamped to [0, total-1].
	 * Static for unit testing (same pattern as {@link #settingHoverItem}).
	 */
	static int scrubFrameForX(int mx, int barX, int barW, int total) {
		if(total <= 0 || barW <= 0) return 0;
		long frame = (long)(mx - barX) * total / barW;
		if(frame < 0) return 0;
		if(frame > total - 1) return total - 1;
		return (int)frame;
	}

	/**
	 * Current playback position in the extended timeline domain
	 * ({@code settingFrames + total}): the settings-screen elapsed frame while
	 * the engine sits in SETTING, otherwise {@code settingFrames + replayTimer}.
	 */
	private int currentDomainPos() {
		GameEngine eng = gameManager.engine[0];
		if(eng.stat == GameEngine.Status.SETTING) return settingElapsed;
		return settingFrames + eng.replayTimer;
	}

	/**
	 * Seek to a position in the extended domain. The leading {@code settingFrames}
	 * map to the pre-game settings screen (re-displayed via {@link #seekSetting});
	 * everything past it is a replayTimer frame handled by {@link #seekReplay}.
	 */
	private void seekDomain(int pos) {
		int domain = settingFrames + replayTotalFrames();
		if(pos < 0) pos = 0;
		if(pos > domain - 1) pos = domain - 1;
		if(pos < settingFrames) seekSetting(pos);
		else seekReplay(pos - settingFrames);
	}

	/**
	 * Seek replay playback to the given frame. The engine has no state
	 * snapshots, so a backward seek resets the manager (which re-reads the
	 * replay in engine.init()) and re-simulates forward; a forward seek just
	 * runs extra frames. SEs are muted during the catch-up; the BGM sync in
	 * update() restores the right track afterwards. Always simulates at least
	 * until gameActive so a seek can't strand the paused engine on the
	 * auto-advancing SETTING/READY screens.
	 */
	private void seekReplay(int target) {
		GameEngine eng = gameManager.engine[0];
		if(target < 0) target = 0;
		if(target < eng.replayTimer) gameManager.reset();
		ResourceHolderSDL.soundManager.mute = true;
		try {
			// ponytail: synchronous re-sim; ~36k frames (a 10min replay) is
			// headless engine ticks only. Make it async if it ever stutters.
			// +600 slack covers SETTING/READY frames that don't advance
			// replayTimer.
			for(int guard = target + 600;
				guard > 0 && (eng.replayTimer < target || !eng.gameActive)
					&& eng.stat != GameEngine.Status.RESULT && !gameManager.getQuitFlag();
				guard--) {
				gameManager.updateAll();
			}
		} finally {
			ResourceHolderSDL.soundManager.mute = false;
		}
	}

	/**
	 * Re-display the pre-game settings screen at the given elapsed frame. Unlike
	 * {@link #seekReplay} this deliberately leaves the engine in SETTING
	 * (gameActive false), so a leftward drag into the violet band shows the
	 * settings again. Resets and re-simulates the short, fixed settings screen;
	 * prevTickSetting is set so resumed playback continues the count from here
	 * instead of snapping back to 0.
	 */
	private void seekSetting(int elapsed) {
		GameEngine eng = gameManager.engine[0];
		if(elapsed < 0) elapsed = 0;
		gameManager.reset();                       // re-init -> SETTING, replayTimer 0, menuTime 0
		ResourceHolderSDL.soundManager.mute = true;
		int count = 0;
		try {
			for(int guard = elapsed + 8;
				guard > 0 && count < elapsed
					&& eng.stat == GameEngine.Status.SETTING && !gameManager.getQuitFlag();
				guard--) {
				gameManager.updateAll();
				count++;
			}
		} finally {
			ResourceHolderSDL.soundManager.mute = false;
		}
		settingElapsed = count;
		prevTickSetting = (eng.stat == GameEngine.Status.SETTING);
	}
}
