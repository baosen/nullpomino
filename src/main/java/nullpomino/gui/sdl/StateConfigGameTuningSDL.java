// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import com.sun.jna.Pointer;

import nullpomino.game.component.Controller;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.play.GameManager;
import nullpomino.game.ai.DummyAI;
import nullpomino.game.mode.PreviewMode;
import nullpomino.game.wallkick.Wallkick;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;
import nullpomino.game.randomizer.Randomizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Game Tuning menu state
 */
public class StateConfigGameTuningSDL extends DummyMenuScrollStateSDL {
	/** UI Text identifier Strings */
	protected static final String[] UI_TEXT = {
		"GameTuning_RotateButtonDefaultRight",
		"GameTuning_Skin",
		"GameTuning_MinDAS",
		"GameTuning_MaxDAS",
		"GameTuning_DasDelay",
		"GameTuning_ReverseUpDown",
		"GameTuning_MoveDiagonal",
		"GameTuning_BlockOutlineType",
		"GameTuning_BlockShowOutlineOnly",
		"GameTuning_Preview",
	};

	/** Log */
	static Logger log = LoggerFactory.getLogger(StateConfigGameTuningSDL.class);

	/** Outline type names */
	protected static final String[] OUTLINE_TYPE_NAMES = {"AUTO", "NONE", "NORMAL", "CONNECT", "SAMECOLOR"};

	/** Cursor index for the [PREVIEW] menu entry */
	private static final int CURSOR_PREVIEW = 9;

	/** Player number */
	public int player;

	/** Preview flag */
	protected boolean isPreview;

	/** Game Manager for preview */
	protected GameManager gameManager;

	/** A button rotation -1=Auto 0=Always CCW 1=Always CW */
	protected int owRotateButtonDefaultRight;

	/** Block Skin -1=Auto 0 or above=Fixed */
	protected int owSkin;

	/** Min/Max DAS -1=Auto 0 or above=Fixed */
	protected int owMinDAS, owMaxDAS;

	/** DAS Delay -1=Auto 0 or above=Fixed */
	protected int owDasDelay;

	/** Reverse the roles of up/down keys in-game */
	protected boolean owReverseUpDown;

	/** Diagonal move (-1=Auto 0=Disable 1=Enable) */
	protected int owMoveDiagonal;

	/** Outline type (-1:Auto 0orAbove:Fixed) */
	protected int owBlockOutlineType;

	/** Show outline only flag (-1:Auto 0:Always Normal 1:Always Outline Only) */
	protected int owBlockShowOutlineOnly;

	/**
	 * Constructor
	 */
	public StateConfigGameTuningSDL() {
		pageHeight = 10;
		maxCursor = 9;
		player = 0;
		cursor = 0;
	}

	/**
	 * Load settings
	 * @param prop Property file to read from
	 */
	protected void loadConfig(CustomProperties prop) {
		owRotateButtonDefaultRight = prop.getProperty(player + ".tuning.owRotateButtonDefaultRight", -1);
		owSkin = prop.getProperty(player + ".tuning.owSkin", -1);
		owMinDAS = prop.getProperty(player + ".tuning.owMinDAS", -1);
		owMaxDAS = prop.getProperty(player + ".tuning.owMaxDAS", -1);
		owDasDelay = prop.getProperty(player + ".tuning.owDasDelay", -1);
		owReverseUpDown = prop.getProperty(player + ".tuning.owReverseUpDown", false);
		owMoveDiagonal = prop.getProperty(player + ".tuning.owMoveDiagonal", -1);
		owBlockOutlineType = prop.getProperty(player + ".tuning.owBlockOutlineType", -1);
		owBlockShowOutlineOnly = prop.getProperty(player + ".tuning.owBlockShowOutlineOnly", -1);
	}

	/**
	 * Save settings
	 * @param prop Property file to save to
	 */
	protected void saveConfig(CustomProperties prop) {
		prop.setProperty(player + ".tuning.owRotateButtonDefaultRight", owRotateButtonDefaultRight);
		prop.setProperty(player + ".tuning.owSkin", owSkin);
		prop.setProperty(player + ".tuning.owMinDAS", owMinDAS);
		prop.setProperty(player + ".tuning.owMaxDAS", owMaxDAS);
		prop.setProperty(player + ".tuning.owDasDelay", owDasDelay);
		prop.setProperty(player + ".tuning.owReverseUpDown", owReverseUpDown);
		prop.setProperty(player + ".tuning.owMoveDiagonal", owMoveDiagonal);
		prop.setProperty(player + ".tuning.owBlockOutlineType", owBlockOutlineType);
		prop.setProperty(player + ".tuning.owBlockShowOutlineOnly", owBlockShowOutlineOnly);
	}

	/**
	 * Persist the in-memory tuning values to propGlobal and flush the config
	 * file so every L/R nudge and mouse click sticks immediately — matches
	 * the General Options auto-save behaviour. The runtime doesn't need any
	 * of these values live; they're read again when the preview or a real
	 * game starts.
	 */
	protected void applyAndSave() {
		saveConfig(NullpoMinoSDL.propGlobal);
		NullpoMinoSDL.saveConfig();
	}

	/*
	 * Called when entering this state
	 */
	@Override
	public void enter() {
		isPreview = false;
		loadConfig(NullpoMinoSDL.propGlobal);
		rebuildList();
	}

	/*
	 * Called when leaving the state
	 */
	@Override
	public void leave() {
		stopPreviewGame();
	}

	/**
	 * Start the preview game
	 */
	protected void startPreviewGame() {
		NullpoMinoSDL.disableAutoInputUpdate = true;

		gameManager = new GameManager(new RendererSDL());
		gameManager.receiver.setGraphics(NullpoMinoSDL.renderer);

		gameManager.mode = new PreviewMode();
		gameManager.init();

		gameManager.backgroundStatus.bg = -2;	// Force no BG

		// Initialization for each player
		for(int i = 0; i < gameManager.getPlayers(); i++) {
			// Tuning
			gameManager.engine[i].owRotateButtonDefaultRight = owRotateButtonDefaultRight;
			gameManager.engine[i].owSkin = owSkin;
			gameManager.engine[i].owMinDAS = owMinDAS;
			gameManager.engine[i].owMaxDAS = owMaxDAS;
			gameManager.engine[i].owDasDelay = owDasDelay;
			gameManager.engine[i].owReverseUpDown = owReverseUpDown;
			gameManager.engine[i].owMoveDiagonal = owMoveDiagonal;
			gameManager.engine[i].owBlockOutlineType = owBlockOutlineType;
			gameManager.engine[i].owBlockShowOutlineOnly = owBlockShowOutlineOnly;

			// Rule
			RuleOptions ruleopt = null;
			String rulename = NullpoMinoSDL.propGlobal.getProperty(i + ".rule", "");
			if(gameManager.mode.getGameStyle() > 0) {
				rulename = NullpoMinoSDL.propGlobal.getProperty(i + ".rule." + gameManager.mode.getGameStyle(), "");
			}
			if((rulename != null) && (rulename.length() > 0)) {
				log.info("Load rule options from {}", rulename);
				ruleopt = GeneralUtil.loadRule(rulename);
			} else {
				log.info("Load rule options from setting file");
				ruleopt = new RuleOptions();
				ruleopt.readProperty(NullpoMinoSDL.propGlobal, i);
			}
			gameManager.engine[i].ruleopt = ruleopt;

			// Randomizer
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

			// Init
			gameManager.engine[i].init();
		}

		isPreview = true;
	}

	/**
	 * Stop the preview game
	 */
	protected void stopPreviewGame() {
		if(isPreview) {
			NullpoMinoSDL.disableAutoInputUpdate = false;
			isPreview = false;
			if(gameManager != null) {
				gameManager.shutdown();
				gameManager = null;
			}
		}
	}

	/**
	 * Rebuild {@link #list} with the current tuning values. Called each
	 * frame from {@link #render()} so labels like "MIN DAS:12" track live
	 * L/R edits without any per-case refresh code.
	 */
	protected void rebuildList() {
		list = new String[] {
			"A BUTTON ROTATE:" + rotateLabel(owRotateButtonDefaultRight),
			"BLOCK SKIN:" + (owSkin == -1 ? "AUTO" : String.valueOf(owSkin)),
			"MIN DAS:" + (owMinDAS == -1 ? "AUTO" : String.valueOf(owMinDAS)),
			"MAX DAS:" + (owMaxDAS == -1 ? "AUTO" : String.valueOf(owMaxDAS)),
			"DAS DELAY:" + (owDasDelay == -1 ? "AUTO" : String.valueOf(owDasDelay)),
			"REVERSE UP/DOWN:" + GeneralUtil.getOorX(owReverseUpDown),
			"DIAGONAL MOVE:" + triStateLabel(owMoveDiagonal),
			"OUTLINE TYPE:" + OUTLINE_TYPE_NAMES[owBlockOutlineType + 1],
			"SHOW OUTLINE ONLY:" + triStateLabel(owBlockShowOutlineOnly),
			"[PREVIEW]",
		};
	}

	private static String rotateLabel(int v) {
		if(v == 0) return "LEFT";
		if(v == 1) return "RIGHT";
		return "AUTO";
	}

	/** -1 → AUTO, 0 → off glyph 'e' (X), 1 → on glyph 'c' (O). */
	private static String triStateLabel(int v) {
		if(v == 0) return "e";
		if(v == 1) return "c";
		return "AUTO";
	}

	/*
	 * Draw the game screen
	 */
	@Override
	public void render() {
		if(isPreview) {
			SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, ResourceHolderSDL.imgMenu, null, null);
			try {
				String strButtonF = gameManager.receiver.getKeyNameByButtonID(gameManager.engine[0], Controller.BUTTON_F);
				int fontY = (gameManager.receiver.getNextDisplayType() == 2) ? 1 : 27;
				NormalFontSDL.printFontGrid(1, fontY, "PUSH F BUTTON (" + strButtonF.toUpperCase() + " KEY) TO EXIT", NormalFontSDL.COLOR_YELLOW);

				gameManager.renderAll();
			} catch (Exception e) {
				log.error("Render fail", e);
			}
			return;
		}

		rebuildList();
		super.render();
	}

	@Override
	protected void drawRow(int row, int y) {
		// Rows embed font-sprite characters (the 'c'/'e' O-X glyphs used by
		// getOorX and triStateLabel), so the label must render verbatim —
		// the base's default row renderer would toUpperCase them and break
		// the sprite lookup.
		NormalFontSDL.printFontGrid(2, 3 + y, list[row], (cursor == row));
		if(cursor == row) NormalFontSDL.printFontGrid(1, 3 + y, "b", NormalFontSDL.COLOR_RED);
	}

	@Override
	protected void onRenderSuccess() {
		NormalFontSDL.printFontGrid(1, 1, "GAME TUNING (" + (player + 1) + "P)", NormalFontSDL.COLOR_ORANGE);

		// Preview sprite for the currently-selected block skin, positioned to
		// the right of the BLOCK SKIN row (grid y=4, pixel y=64).
		if((owSkin >= 0) && (owSkin < ResourceHolderSDL.imgNormalBlockList.size())) {
			Pointer imgBlock = ResourceHolderSDL.imgNormalBlockList.get(owSkin);

			if(ResourceHolderSDL.blockStickyFlagList.get(owSkin) == true) {
				for(int j = 0; j < 9; j++) {
					SDLStructs.SDL_FRect rectSkinSrc = new SDLStructs.SDL_FRect(0, j * 16, 16, 16);
					SDLStructs.SDL_FRect rectSkinDst = new SDLStructs.SDL_FRect(256 + (j * 16), 64, 16, 16);
					SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, imgBlock, rectSkinSrc, rectSkinDst);
				}
			} else {
				SDLStructs.SDL_FRect rectSkinSrc = new SDLStructs.SDL_FRect(0, 0, 144, 16);
				SDLStructs.SDL_FRect rectSkinDst = new SDLStructs.SDL_FRect(256, 64, 144, 16);
				SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, imgBlock, rectSkinSrc, rectSkinDst);
			}
		}

		if(cursor >= 0 && cursor < UI_TEXT.length) {
			NormalFontSDL.printTTFFont(16, 432, NullpoMinoSDL.getUIText(UI_TEXT[cursor]));
		}
	}

	/*
	 * Update game state
	 */
	@Override
	public void update() {
		if(isPreview) {
			// Keep Page Up/Down edge detection in sync while we skip the menu
			// branch — PageNavigationSDL expects exactly one poll per frame.
			PageNavigationSDL.checkPageEvent();

			try {
				int joynum = NullpoMinoSDL.joyUseNumber[0];

				boolean ingame = (gameManager != null) && (gameManager.engine.length > 0) &&
								 (gameManager.engine[0] != null) && (gameManager.engine[0].isInGame);

				if((NullpoMinoSDL.joystickMax > 0) && (joynum >= 0) && (joynum < NullpoMinoSDL.joystickMax)) {
					GameKeySDL.gamekey[0].update(
							NullpoMinoSDL.keyPressedState,
							NullpoMinoSDL.joyPressedState[joynum],
							NullpoMinoSDL.joyAxisX[joynum],
							NullpoMinoSDL.joyAxisY[joynum],
							NullpoMinoSDL.joyHatState[joynum],
							ingame);
				} else {
					GameKeySDL.gamekey[0].update(NullpoMinoSDL.keyPressedState, ingame);
				}

				GameKeySDL.gamekey[0].inputStatusUpdate(gameManager.engine[0].ctrl);
				gameManager.updateAll();

				if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_RETRY)) {
					gameManager.reset();
					gameManager.backgroundStatus.bg = -1;
				}

				if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_F) || GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_GIVEUP) ||
				   gameManager.getQuitFlag())
				{
					stopPreviewGame();
				}
			} catch (Exception e) {
				log.error("Update fail", e);
			}
			return;
		}

		super.update();
	}

	@Override
	public boolean updateMouseInput() {
		// Inherit scroll-bar drag / wheel / page clicks, but reinterpret the
		// row-click return value: clicks on [PREVIEW] launch the preview
		// immediately (same as BUTTON_A on that row), clicks elsewhere route
		// through onChange(+1) so the value bumps one step the way RIGHT
		// does. Always return false so BUTTON_A keeps sole ownership of the
		// save-and-exit path.
		if(super.updateMouseInput()) {
			if(cursor == CURSOR_PREVIEW) {
				ResourceHolderSDL.soundManager.play("decide");
				startPreviewGame();
			} else {
				onChange(1);
			}
		}
		return false;
	}

	@Override
	protected void onChange(int change) {
		ResourceHolderSDL.soundManager.play("change");

		switch(cursor) {
		case 0:
			owRotateButtonDefaultRight += change;
			if(owRotateButtonDefaultRight < -1) owRotateButtonDefaultRight = 1;
			if(owRotateButtonDefaultRight > 1) owRotateButtonDefaultRight = -1;
			break;
		case 1:
			owSkin += change;
			if(owSkin < -1) owSkin = ResourceHolderSDL.imgNormalBlockList.size() - 1;
			if(owSkin > ResourceHolderSDL.imgNormalBlockList.size() - 1) owSkin = -1;
			break;
		case 2:
			owMinDAS += change;
			if(owMinDAS < -1) owMinDAS = 99;
			if(owMinDAS > 99) owMinDAS = -1;
			break;
		case 3:
			owMaxDAS += change;
			if(owMaxDAS < -1) owMaxDAS = 99;
			if(owMaxDAS > 99) owMaxDAS = -1;
			break;
		case 4:
			owDasDelay += change;
			if(owDasDelay < -1) owDasDelay = 99;
			if(owDasDelay > 99) owDasDelay = -1;
			break;
		case 5:
			owReverseUpDown ^= true;
			break;
		case 6:
			owMoveDiagonal += change;
			if(owMoveDiagonal < -1) owMoveDiagonal = 1;
			if(owMoveDiagonal > 1) owMoveDiagonal = -1;
			break;
		case 7:
			owBlockOutlineType += change;
			if(owBlockOutlineType < -1) owBlockOutlineType = 3;
			if(owBlockOutlineType > 3) owBlockOutlineType = -1;
			break;
		case 8:
			owBlockShowOutlineOnly += change;
			if(owBlockShowOutlineOnly < -1) owBlockShowOutlineOnly = 1;
			if(owBlockShowOutlineOnly > 1) owBlockShowOutlineOnly = -1;
			break;
		}

		applyAndSave();
	}

	@Override
	protected boolean onDecide() {
		ResourceHolderSDL.soundManager.play("decide");

		if(cursor == CURSOR_PREVIEW) {
			startPreviewGame();
			return true;
		}

		NullpoMinoSDL.goBack();
		return true;
	}

	@Override
	protected boolean onCancel() {
		NullpoMinoSDL.goBack();
		return true;
	}

	@Override
	protected boolean onPushButtonD() {
		ResourceHolderSDL.soundManager.play("decide");
		startPreviewGame();
		return true;
	}
}
