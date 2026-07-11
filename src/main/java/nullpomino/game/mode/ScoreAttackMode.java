// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

/**
 * SCORE ATTACK mode (Original from NullpoUE build 121909 by Zircean)
 */
public class ScoreAttackMode extends AbstractManiaMode {
	/** Current version */
	private static final int CURRENT_VERSION = 6;

	/** Scripted items are available from this replay version onward. */
	private static final int ITEM_VERSION = 1;

	/** The section slow-down moved from level 100/200 to 101/201 in this version. */
	private static final int SLOWDOWN_SHIFT_VERSION = 2;

	/** Corrected gravity, score rounding, and per-piece sonic-drop bonus. */
	private static final int ACCURACY_FIX_VERSION = 3;

	/** Items at an exact starting section boundary (level 100/200) are awarded from this version. */
	private static final int ITEM_START_LEVEL_FIX_VERSION = 4;

	/** Free Fall immediately removes lines completed by its collapse. */
	private static final int FREE_FALL_LINE_CLEAR_VERSION = 5;

	/** Corrected lock-speed scoring, item-clear scoring, and roll top-out handling. */
	private static final int SCORE_AND_ROLL_FIX_VERSION = 6;

	/** Gravity table (Gravity speed value) */
	private static final int[] tableGravityValue =
	{
		4, 5, 6, 8, 10, 12, 16, 32, 48, 64, 4, 5, 6, 8, 12, 32, 48, 80, 112, 128, 144, 16, 48, 80, 112, 144, 176, 192, 208, 224, 240, -1
	};

	/** Gravity table (Gravity change level) */
	private static final int[] tableGravityChangeLevel =
	{
		8, 19, 35, 40, 50, 60, 70, 80, 90, 101, 108, 119, 125, 131, 139, 149, 156, 164, 174, 180, 201, 212, 221, 232, 244, 256, 267, 277, 287, 295, 300, 10000
	};

	/** Ending time limit */
	private static final int ROLLTIMELIMIT = 1956;

	/** Secret grade names */
	private static final String[] tableSecretGradeName =
	{
		"S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8", "S9",	//  0 -  8
		"M1", "M2", "M3", "M4", "M5", "M6", "M7", "M8", "M9",	//  9 - 17
		"GM"													// 18
	};

	/** Number of sections */
	private static final int SECTION_MAX = 3;

	/** Default section time */
	private static final int DEFAULT_SECTION_TIME = 6000;

	/** No scripted item remains to be awarded. */
	private static final int ITEM_LEVEL_NONE = -1;

	/** GameManager object (Manages entire game status) */

	/** EventReceiver object (This receives many game events, can also be used for drawing the fonts.) */

	/** Current gravity index number (Increases when the level reaches to certain value that defined in tableGravityChangeLevel) */
	private int gravityindex;

	/** Next section level */
	private int nextseclv;

	/** Level up flag (Set to true when the level increases) */
	private boolean lvupflag;


	/** Used by combo scoring */
	private int comboValue;

	/** Amount of points you just get from line clears */
	private int lastscore;

	/** Frames spent controlling the piece that produced the pending clear. */
	private int activePieceFrames;

	/** Elapsed time from last line clear (lastscore is displayed to screen until this reaches to 120) */
	private int scgettime;

	/** Remaining ending time limit */
	private int rolltime;

	/** Whether the roll completion screen has already been entered. */
	private boolean rollCompletionAcknowledged;

	/** Secret Grade */
	private int secretGrade;

	/** Current BGM number */
	private int bgmlv;

	/** Selected start level */
	private int startlevel;

	/** Always show ghost */
	private boolean alwaysghost;

	/** Always 20G */
	private boolean always20g;

	/** Big Mode */
	private boolean big;

	/** Show section time */
	private boolean showsectiontime;

	/** Enable scripted items */
	private boolean enableitem;

	/** Next level that awards a scripted item */
	private int nextItemLevel;

	/** Item effect to apply on the first post-clear ARE frame */
	private int pendingItemEffect;

	/** Version of this mode */
	private int version;

	/** Score records */
	private int[] rankingScore;

	/**
	 * Returns the name of this mode
	 */
	@Override
	public String getName() {
		return "SCORE ATTACK";
	}

	/**
	 * This function will be called when the game enters the main game screen.
	 */
	@Override
	public void playerInit(GameEngine engine, int playerID) {
		owner = engine.owner;
		receiver = engine.owner.receiver;
		menuTime = 0;
		menuCursor = 0;

		gravityindex = 0;
		nextseclv = 0;
		lvupflag = true;
		comboValue = 0;
		harddropBonus = 0;
		lastscore = 0;
		activePieceFrames = 0;
		scgettime = 0;
		rolltime = 0;
		rollCompletionAcknowledged = false;
		bgmlv = 0;
		sectiontime = new int[SECTION_MAX];
		sectionIsNewRecord = new boolean[SECTION_MAX];
		sectionAnyNewRecord = false;
		sectionscomp = 0;
		sectionavgtime = 0;
		isShowBestSectionTime = false;
		startlevel = 0;
		alwaysghost = false;
		always20g = false;
		big = false;
		showsectiontime = true;
		enableitem = true;
		nextItemLevel = ITEM_LEVEL_NONE;
		pendingItemEffect = Block.BLOCK_ITEM_NONE;

		rankingRank = -1;
		rankingScore = new int[RANKING_MAX];
		rankingLevel = new int[RANKING_MAX];
		rankingTime = new int[RANKING_MAX];
		bestSectionTime = new int[SECTION_MAX];

		engine.tspinEnable = false;
		engine.b2bEnable = false;
		engine.comboType = GameEngine.COMBO_TYPE_DOUBLE;
		engine.bighalf = false;
		engine.bigmove = false;
		engine.speed.are = 25;
		engine.speed.areLine = 25;
		engine.speed.lineDelay = 41;
		engine.speed.lockDelay = 30;
		engine.speed.das = 15;

		if(owner.replayMode == false) {
			loadSetting(owner.modeConfig);
			loadRanking(owner.modeConfig, engine.ruleopt.strRuleName);
			version = CURRENT_VERSION;
		} else {
			loadSetting(owner.replayProp);
			version = owner.replayProp.getProperty("scoreattack.version", 0);
			if(version < ITEM_VERSION) enableitem = false;
		}
		engine.staffrollNoDeath = version < SCORE_AND_ROLL_FIX_VERSION;
		engine.rainbowAnimate = isItemEnabled();

		owner.backgroundStatus.bg = startlevel;
	}

	/**
	 * Load the settings
	 */
	protected void loadSetting(CustomProperties prop) {
		startlevel = prop.getProperty("scoreattack.startlevel", 0);
		alwaysghost = prop.getProperty("scoreattack.alwaysghost", false);
		always20g = prop.getProperty("scoreattack.always20g", false);
		showsectiontime = prop.getProperty("scoreattack.showsectiontime", false);
		big = prop.getProperty("scoreattack.big", false);
		enableitem = prop.getProperty("scoreattack.enableitem", true);
		version = prop.getProperty("scoreattack.version", 0);
	}

	/**
	 * Save the settings
	 */
	protected void saveSetting(CustomProperties prop) {
		prop.setProperty("scoreattack.startlevel", startlevel);
		prop.setProperty("scoreattack.alwaysghost", alwaysghost);
		prop.setProperty("scoreattack.always20g", always20g);
		prop.setProperty("scoreattack.showsectiontime", showsectiontime);
		prop.setProperty("scoreattack.big", big);
		prop.setProperty("scoreattack.enableitem", enableitem);
		prop.setProperty("scoreattack.version", version);
	}

	/**
	 * Set the gravity speed
	 * @param engine GameEngine object
	 */
	private void setSpeed(GameEngine engine) {
		if(always20g == true) {
			engine.speed.gravity = -1;
		} else {
			while(engine.statistics.level >= gravityChangeLevel(gravityindex)) gravityindex++;
			engine.speed.gravity = tableGravityValue[gravityindex];
		}
	}

	/**
	 * Section slow-down (gravity reset) lands at level 101/201 as of
	 * {@link #SLOWDOWN_SHIFT_VERSION}; replays recorded earlier keep the original
	 * 100/200 timing so their recorded inputs stay in sync on playback.
	 */
	private int gravityChangeLevel(int index) {
		int level = tableGravityChangeLevel[index];
		if(version < SLOWDOWN_SHIFT_VERSION && (level == 101 || level == 201)) return level - 1;
		if(version < ACCURACY_FIX_VERSION && level == 156) return 146;
		return level;
	}

	/**
	 * Calculates average section time
	 */
	private void setAverageSectionTime() {
		if(sectionscomp > 0) {
			int temp = 0;
			for(int i = startlevel; i < startlevel + sectionscomp; i++) temp += sectiontime[i];
			sectionavgtime = temp / sectionscomp;
		} else {
			sectionavgtime = 0;
		}
	}

	/**
	 * Main routine for game setup screen
	 */
	@Override
	public boolean onSetting(GameEngine engine, int playerID) {
		if(engine.owner.replayMode == false) {
			// Configuration changes
			int change = updateCursor(engine, 5);
			if(change != 0) {
				receiver.playSE("change");

				switch(menuCursor) {
				case 0:
					startlevel += change;
					if(startlevel < 0) startlevel = 2;
					if(startlevel > 2) startlevel = 0;
					owner.backgroundStatus.bg = startlevel;
					break;
				case 1:
					alwaysghost = !alwaysghost;
					break;
				case 2:
					always20g = !always20g;
					break;
				case 3:
					showsectiontime = !showsectiontime;
					break;
				case 4:
					big = !big;
					break;
				case 5:
					enableitem = !enableitem;
					engine.rainbowAnimate = isItemEnabled();
					loadRanking(owner.modeConfig, engine.ruleopt.strRuleName);
					rankingRank = -1;
					break;
				}
			}

			// Check for F button, when pressed this will flip Leaderboard/Best Section Time Records
			if(engine.ctrl.isPush(Controller.BUTTON_F) && (menuTime >= 5)) {
				engine.playSE("change");
				isShowBestSectionTime = !isShowBestSectionTime;
			}

			// Check for A button, when pressed this will begin the game
			if(engine.ctrl.isPush(Controller.BUTTON_A) && (menuTime >= 5)) {
				receiver.playSE("decide");
				saveSetting(owner.modeConfig);
				receiver.saveModeConfig(owner.modeConfig);
				isShowBestSectionTime = false;
				sectionscomp = 0;
				return false;
			}

			// Check for B button, when pressed this will shutdown the game engine.
			if(engine.ctrl.isPush(Controller.BUTTON_B)) {
				engine.quitflag = true;
			}

			menuTime++;
		} else {
			menuTime++;
			menuCursor = -1;

			if(menuTime >= 60) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Renders game setup screen
	 */
	@Override
	public void renderSetting(GameEngine engine, int playerID) {
		drawMenu(engine, playerID, receiver, 0, EventReceiver.COLOR_BLUE, 0,
				"LEVEL", String.valueOf(startlevel * 100),
				"FULL GHOST", GeneralUtil.getONorOFF(alwaysghost),
				"20G MODE", GeneralUtil.getONorOFF(always20g),
				"SHOW STIME", GeneralUtil.getONorOFF(showsectiontime),
				"BIG",  GeneralUtil.getONorOFF(big),
				"ITEM", GeneralUtil.getONorOFF(enableitem));
	}

	/**
	 * This function will be called before the game actually begins (after Ready&Go screen disappears)
	 */
	@Override
	public void startGame(GameEngine engine, int playerID) {
		engine.statistics.level = startlevel * 100;

		nextseclv = engine.statistics.level + 100;
		if(engine.statistics.level < 0) nextseclv = 100;
		if(engine.statistics.level >= 900) nextseclv = 999;
		nextItemLevel = getNextItemLevel(engine.statistics.level);
		pendingItemEffect = Block.BLOCK_ITEM_NONE;
		// Starting exactly on a section boundary skips the levelUp that would fire
		// at that level during normal play, so schedule its item here instead.
		scheduleScriptedItem(engine);

		owner.backgroundStatus.bg = engine.statistics.level / 100;

		if(engine.statistics.level < 500) bgmlv = 0;
		else bgmlv = 1;

		engine.big = big;

		setSpeed(engine);
		owner.bgmStatus.bgm = bgmlv;
	}

	/**
	 * Renders HUD (leaderboard or game statistics)
	 */
	@Override
	public void renderLast(GameEngine engine, int playerID) {
		receiver.drawScoreFont(engine, playerID, 0, 0, "SCORE ATTACK", EventReceiver.COLOR_DARKBLUE);

		if( (engine.stat == GameEngine.Status.SETTING) || ((engine.stat == GameEngine.Status.RESULT) && (owner.replayMode == false)) ) {
			if((owner.replayMode == false) && (startlevel == 0) && (big == false) && (always20g == false) && (engine.ai == null)) {
				if(!isShowBestSectionTime) {
					// Score Leaderboard
					receiver.drawScoreFont(engine, playerID, 3, 2,
						enableitem ? "SCORE+ITEM TIME" : "SCORE  TIME", EventReceiver.COLOR_BLUE);

					for(int i = 0; i < RANKING_MAX; i++) {
						receiver.drawScoreFont(engine, playerID, 0, 3 + i, String.format("%2d", i + 1), EventReceiver.COLOR_YELLOW);
						receiver.drawScoreFont(engine, playerID, 3, 3 + i, String.valueOf(rankingScore[i]), (i == rankingRank));
						receiver.drawScoreFont(engine, playerID, 10, 3 + i, GeneralUtil.getTime(rankingTime[i]), (i == rankingRank));
					}

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW SECTION TIME", EventReceiver.COLOR_GREEN);
				} else {
					// Best Section Time Records
					receiver.drawScoreFont(engine, playerID, 0, 2, "SECTION TIME", EventReceiver.COLOR_BLUE);

					int totalTime = 0;
					for(int i = 0; i < SECTION_MAX; i++) {
						int temp = i * 100;
						int temp2 = ((i + 1) * 100) - 1;

						String strSectionTime;
						strSectionTime = String.format("%3d-%3d %s", temp, temp2, GeneralUtil.getTime(bestSectionTime[i]));

						receiver.drawScoreFont(engine, playerID, 0, 3 + i, strSectionTime, sectionIsNewRecord[i]);

						totalTime += bestSectionTime[i];
					}

					receiver.drawScoreFont(engine, playerID, 0, 14, "TOTAL", EventReceiver.COLOR_BLUE);
					receiver.drawScoreFont(engine, playerID, 0, 15, GeneralUtil.getTime(totalTime));
					receiver.drawScoreFont(engine, playerID, 9, 14, "AVERAGE", EventReceiver.COLOR_BLUE);
					receiver.drawScoreFont(engine, playerID, 9, 15, GeneralUtil.getTime(totalTime / SECTION_MAX));

					receiver.drawScoreFont(engine, playerID, 0, 17, "F:VIEW RANKING", EventReceiver.COLOR_GREEN);
				}
			}
		} else {
			String strScore;
			receiver.drawScoreFont(engine, playerID, 0, 5, "SCORE", EventReceiver.COLOR_BLUE);
			if((lastscore == 0) || (scgettime <= 0)) {
				strScore = String.valueOf(engine.statistics.score);
			} else {
				strScore = String.valueOf(engine.statistics.score) + "(+" + String.valueOf(lastscore) + ")";
			}
			receiver.drawScoreFont(engine, playerID, 0, 6, strScore);

			receiver.drawScoreFont(engine, playerID, 0, 9, "LEVEL", EventReceiver.COLOR_BLUE);
			int tempLevel = engine.statistics.level;
			if(tempLevel < 0) tempLevel = 0;
			String strLevel = String.format("%3d", tempLevel);
			receiver.drawScoreFont(engine, playerID, 0, 10, strLevel);

			int speed = engine.speed.gravity / 128;
			if(engine.speed.gravity < 0) speed = 40;
			receiver.drawSpeedMeter(engine, playerID, 0, 11, speed);

			receiver.drawScoreFont(engine, playerID, 0, 12, "300");

			receiver.drawScoreFont(engine, playerID, 0, 14, "TIME", EventReceiver.COLOR_BLUE);
			receiver.drawScoreFont(engine, playerID, 0, 15, GeneralUtil.getTime(engine.statistics.time));

			drawControlsHelp(engine, playerID, 19);

			if((engine.gameActive) && (engine.ending == 2)) {
				int time = ROLLTIMELIMIT - rolltime;
				if(time < 0) time = 0;
				receiver.drawScoreFont(engine, playerID, 0, 17, "ROLL TIME", EventReceiver.COLOR_BLUE);
				receiver.drawScoreFont(engine, playerID, 0, 18, GeneralUtil.getTime(time), ((time > 0) && (time < 10 * 60)));
			}

			// Section time
			if((showsectiontime == true) && (sectiontime != null)) {
				int x = (receiver.getNextDisplayType() == 2) ? 8 : 12;
				int x2 = (receiver.getNextDisplayType() == 2) ? 9 : 12;

				receiver.drawScoreFont(engine, playerID, x, 2, "SECTION TIME", EventReceiver.COLOR_BLUE);

				for(int i = 0; i < sectiontime.length; i++) {
					if(sectiontime[i] > 0) {
						int temp = i * 100;
						if(temp > 300) temp = 300;

						int section = engine.statistics.level / 100;
						String strSeparator = " ";
						if((i == section) && (engine.ending == 0)) strSeparator = "b";

						String strSectionTime;
						strSectionTime = String.format("%3d%s%s", temp, strSeparator, GeneralUtil.getTime(sectiontime[i]));

						receiver.drawScoreFont(engine, playerID, x, 3 + i, strSectionTime, sectionIsNewRecord[i]);
					}
				}

				if(sectionavgtime > 0) {
					receiver.drawScoreFont(engine, playerID, x2, 14, "AVERAGE", EventReceiver.COLOR_BLUE);
					receiver.drawScoreFont(engine, playerID, x2, 15, GeneralUtil.getTime(sectionavgtime));
				}
			}
		}
	}

	/**
	 * This function will be called when the piece is active
	 */
	@Override
	public boolean onMove(GameEngine engine, int playerID) {
		if((engine.ending == 0) && (engine.statc[0] == 0) && (engine.holdDisable == false) && (!lvupflag)) {
			if(engine.statistics.level < 299) engine.statistics.level++;
			levelUp(engine);
		}
		if((engine.ending == 0) && (engine.statc[0] > 0)) {
			lvupflag = false;
		}

		return false;
	}

	/**
	 * This function will be called during ARE
	 */
	@Override
	public boolean onARE(GameEngine engine, int playerID) {
		if((engine.statc[0] == 0) && (pendingItemEffect != Block.BLOCK_ITEM_NONE)) {
			applyPendingItemEffect(engine);
		}

		if((engine.ending == 0) && (engine.statc[0] >= engine.statc[1] - 1) && (!lvupflag)) {
			if (engine.statistics.level < 299) engine.statistics.level++;
			levelUp(engine);
			lvupflag = true;
		}

		return false;
	}

	/**
	 * Levelup
	 */
	private void levelUp(GameEngine engine) {
		engine.meterValue = ((engine.statistics.level % 100) * receiver.getMeterMax(engine)) / 99;
		engine.meterColor = GameEngine.METER_COLOR_GREEN;
		if(engine.statistics.level % 100 >= 50) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
		if(engine.statistics.level % 100 >= 80) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
		if(engine.statistics.level >= nextseclv - 1) engine.meterColor = GameEngine.METER_COLOR_RED;

		if(engine.statistics.level >= nextseclv) {
			nextseclv += 100;
			receiver.playSE("levelup");

			sectionscomp++;
			setAverageSectionTime();
			stNewRecordCheck(sectionscomp - 1);
		}

		scheduleScriptedItem(engine);

		setSpeed(engine);

		if((engine.statistics.level >= 100) && (!alwaysghost)) engine.ghost = false;

		if((bgmlv == 0) && (engine.statistics.level >= 290) && (engine.ending == 0))
			owner.bgmStatus.fadesw = true;
	}

	/**
	 * Detect an item in the lines about to clear. The engine has already
	 * calculated {@link GameEngine#lineClearing}, but line flags are not set
	 * until immediately after this callback, so inspect complete rows directly.
	 */
	@Override
	public boolean onLineClear(GameEngine engine, int playerID) {
		if(!isItemEnabled() || (engine.statc[0] != 0) || (engine.lineClearing <= 0)) return false;

		int item = getClearedItem(engine);
		if(item != Block.BLOCK_ITEM_NONE) {
			clearItemTags(engine);
			pendingItemEffect = item;
		}
		return false;
	}

	/** Preserve the active-state timer before the engine resets its status counters. */
	@Override
	public void pieceLocked(GameEngine engine, int playerID, int lines) {
		activePieceFrames = engine.statc[0];
	}

	/**
	 * When a rule has no line ARE, there is no post-clear ARE callback in
	 * which to perform an item. Apply it at the equivalent transition point.
	 */
	@Override
	public boolean lineClearEnd(GameEngine engine, int playerID) {
		if((pendingItemEffect != Block.BLOCK_ITEM_NONE) && (engine.getARELine() <= 0) && (engine.ending == 0)) {
			applyPendingItemEffect(engine);
		}
		return false;
	}

	private boolean isItemEnabled() {
		return enableitem && (version >= ITEM_VERSION);
	}

	private int getNextItemLevel(int level) {
		// Starting exactly on a section boundary should still award that section's
		// item. Older replays used strict < and skipped it, so keep them in sync.
		int slack = (version >= ITEM_START_LEVEL_FIX_VERSION) ? 1 : 0;
		if(level < 100 + slack) return 100;
		if(level < 200 + slack) return 200;
		return ITEM_LEVEL_NONE;
	}

	private void scheduleScriptedItem(GameEngine engine) {
		if(!isItemEnabled() || (nextItemLevel == ITEM_LEVEL_NONE) || (engine.statistics.level < nextItemLevel)) return;

		// The engine's current queue position is consumed as the next active
		// piece when ARE ends. Tag the following slot so the item remains
		// visible in the Next display while level 100 or 200 is active.
		Piece nextPiece = engine.getNextObject(engine.nextPieceCount + 1);
		if(nextPiece == null) return;

		int item = (nextItemLevel == 100) ? Block.BLOCK_ITEM_FREE_FALL : Block.BLOCK_ITEM_DEL_EVEN;
		setPieceItem(nextPiece, item);
		nextItemLevel = (item == Block.BLOCK_ITEM_FREE_FALL) ? 200 : ITEM_LEVEL_NONE;
	}

	private void setPieceItem(Piece piece, int item) {
		for(Block block : piece.block) block.item = item;
	}

	private int getClearedItem(GameEngine engine) {
		int selectedItem = Block.BLOCK_ITEM_NONE;
		int selectedY = Integer.MAX_VALUE;
		int selectedX = -1;

		for(int y = -engine.field.getHiddenHeight(); y < engine.field.getHeightWithoutHurryupFloor(); y++) {
			if(!isCompleteLine(engine, y)) continue;

			for(int x = 0; x < engine.field.getWidth(); x++) {
				Block block = engine.field.getBlock(x, y);
				if((block == null) || (block.item == Block.BLOCK_ITEM_NONE)) continue;

				if((y < selectedY) || ((y == selectedY) && (x > selectedX))) {
					selectedItem = block.item;
					selectedY = y;
					selectedX = x;
				}
			}
		}

		return selectedItem;
	}

	private boolean isCompleteLine(GameEngine engine, int y) {
		for(int x = 0; x < engine.field.getWidth(); x++) {
			Block block = engine.field.getBlock(x, y);
			if((block == null) || block.isEmpty() || block.getAttribute(Block.BLOCK_ATTRIBUTE_WALL)) return false;
		}
		return true;
	}

	private void clearItemTags(GameEngine engine) {
		for(int y = -engine.field.getHiddenHeight(); y < engine.field.getHeight(); y++) {
			for(int x = 0; x < engine.field.getWidth(); x++) {
				Block block = engine.field.getBlock(x, y);
				if(block != null) block.item = Block.BLOCK_ITEM_NONE;
			}
		}

		if(engine.nextPieceArrayObject != null) {
			for(Piece piece : engine.nextPieceArrayObject) clearPieceItem(piece);
		}
		clearPieceItem(engine.nowPieceObject);
		clearPieceItem(engine.holdPieceObject);
	}

	private void clearPieceItem(Piece piece) {
		if(piece == null) return;
		for(Block block : piece.block) block.item = Block.BLOCK_ITEM_NONE;
	}

	private void applyPendingItemEffect(GameEngine engine) {
		int item = pendingItemEffect;
		pendingItemEffect = Block.BLOCK_ITEM_NONE;

		if(item == Block.BLOCK_ITEM_FREE_FALL) {
			engine.field.freeFall();
			if(version >= FREE_FALL_LINE_CLEAR_VERSION) clearFreeFallLines(engine);
		} else if(item == Block.BLOCK_ITEM_DEL_EVEN) {
			deleteEvenRows(engine);
		}
	}

	/**
	 * Remove rows completed by Free Fall without treating them as a placement,
	 * so they do not enter the normal score/level/statistics pipeline.
	 */
	private void clearFreeFallLines(GameEngine engine) {
		if(engine.field.checkLine() <= 0) return;
		engine.field.clearLine();
		engine.field.downFloatingBlocks();
	}

	private void deleteEvenRows(GameEngine engine) {
		for(int y = 0; y < engine.field.getHeightWithoutHurryupFloor(); y += 2) {
			for(int x = 0; x < engine.field.getWidth(); x++) {
				engine.field.setBlock(x, y, new Block());
			}
			engine.field.setLineFlag(y, true);
		}
		engine.field.downFloatingBlocks();
	}

	/**
	 * Calculates line-clear score
	 * (This function will be called even if no lines are cleared)
	 */
	@Override
	public void calcScore(GameEngine engine, int playerID, int lines) {
		if(engine.ending != 0) return;

		if(lines == 0) {
			comboValue = 1;
		} else {
			comboValue = comboValue + (2 * lines) - 2;
			if(comboValue < 1) comboValue = 1;
		}

		if(lines >= 1) {
			int levelb = engine.statistics.level;
			engine.statistics.level += lines;
			levelUp(engine);

			if(engine.statistics.level >= 300) {
				if(engine.timerActive) {
					engine.statistics.score += 1253*Math.ceil(Math.max(18000-engine.statistics.time,0)/60D);
				}

				bgmlv++;
				owner.bgmStatus.fadesw = false;
				owner.bgmStatus.bgm = bgmlv;

				engine.statistics.level = 300;
				engine.timerActive = false;
				engine.ending = 2;
			} else if(engine.statistics.level >= nextseclv) {
				//nextseclv += 100;
			}

			if(owner.backgroundStatus.bg < (nextseclv-100) / 100) {
				owner.backgroundStatus.fadesw = true;
				owner.backgroundStatus.fadecount = 0;
				owner.backgroundStatus.fadebg = (nextseclv-100) / 100;
			}

			int manuallock = 0;
			if(engine.manualLock == true) manuallock = 1;

			int bravo = 1;
			if(engine.field.isEmpty()) bravo = 4;

			int lockTime = version >= SCORE_AND_ROLL_FIX_VERSION ? activePieceFrames : engine.statc[0];
			int speedBonus = engine.getLockDelay() - lockTime;
			if(speedBonus < 0) speedBonus = 0;

			int levelQuarter = (levelb + lines) / 4;
			int levelHalf = engine.statistics.level / 2;
			if(version >= ACCURACY_FIX_VERSION) {
				levelQuarter = (levelb + lines + 3) / 4;
				levelHalf = (engine.statistics.level + 1) / 2;
			}

			boolean itemClear = isItemEnabled() && (pendingItemEffect != Block.BLOCK_ITEM_NONE);
			if(version >= SCORE_AND_ROLL_FIX_VERSION && itemClear) {
				lastscore = 0;
			} else {
				lastscore = 6*((levelQuarter + engine.softdropFall + manuallock + harddropBonus) * lines * comboValue * bravo +
							levelHalf + (speedBonus * 7));
				engine.statistics.score += lastscore;
			}
			scgettime = 120;
		}

		if(version >= ACCURACY_FIX_VERSION) harddropBonus = 0;
	}


	/**
	 * This function will be called when the game timer updates
	 */
	@Override
	public void onLast(GameEngine engine, int playerID) {
		// Decrease scgettime
		if(scgettime > 0) scgettime--;

		// Increase section timer
		if((engine.timerActive) && (engine.ending == 0)) {
			int section = engine.statistics.level / 100;

			if((section >= 0) && (section < sectiontime.length)) {
				sectiontime[section]++;
			}
		}

		// Increase ending timer
		if((engine.gameActive) && (engine.ending == 2)) {
			if(engine.ctrl.isPress(Controller.BUTTON_F))
				rolltime += 5;
			else
				rolltime += 1;

			int remainRollTime = ROLLTIMELIMIT - rolltime;
			engine.meterValue = (remainRollTime * receiver.getMeterMax(engine)) / ROLLTIMELIMIT;
			engine.meterColor = GameEngine.METER_COLOR_GREEN;
			if(remainRollTime <= 30*60) engine.meterColor = GameEngine.METER_COLOR_YELLOW;
			if(remainRollTime <= 20*60) engine.meterColor = GameEngine.METER_COLOR_ORANGE;
			if(remainRollTime <= 10*60) engine.meterColor = GameEngine.METER_COLOR_RED;

			if(rolltime >= ROLLTIMELIMIT) {
				showRollCompletion(engine);
			}
		}
	}

	private void showRollCompletion(GameEngine engine) {
		rollCompletionAcknowledged = true;
		engine.gameEnded();
		engine.resetStatc();
		engine.stat = GameEngine.Status.EXCELLENT;
	}

	/**
	 * This function will be called when the player tops out
	 */
	@Override
	public boolean onGameOver(GameEngine engine, int playerID) {
		if(engine.statc[0] == 0) {
			secretGrade = engine.field.getSecretGrade();
		}
		if((version >= SCORE_AND_ROLL_FIX_VERSION) && (engine.ending == 2) && !rollCompletionAcknowledged) {
			showRollCompletion(engine);
			return true;
		}
		return false;
	}

	/**
	 * Renders game result screen
	 */
	@Override
	public void renderResult(GameEngine engine, int playerID) {
		receiver.drawMenuFont(engine, playerID,  0, 0, "kn PAGE" + (engine.statc[1] + 1) + "/3", EventReceiver.COLOR_RED);

		if(engine.statc[1] == 0) {
			drawResultStats(engine, playerID, receiver, 2, EventReceiver.COLOR_BLUE,
					Statistic.SCORE, Statistic.LINES, Statistic.LEVEL, Statistic.TIME);
			drawResultRank(engine, playerID, receiver, 13, EventReceiver.COLOR_BLUE, rankingRank);
			if(secretGrade > 4) {
				drawResult(engine, playerID, receiver, 15, EventReceiver.COLOR_BLUE,
						"S. GRADE", String.format("%10s", tableSecretGradeName[secretGrade-1]));
			}
		} else if(engine.statc[1] == 1) {
			receiver.drawMenuFont(engine, playerID,  0, 2, "SECTION", EventReceiver.COLOR_BLUE);

			for(int i = 0; i < sectiontime.length; i++) {
				if(sectiontime[i] > 0) {
					receiver.drawMenuFont(engine, playerID, 2, 3 + i, GeneralUtil.getTime(sectiontime[i]), sectionIsNewRecord[i]);
				}
			}

			if(sectionavgtime > 0) {
				receiver.drawMenuFont(engine, playerID, 0, 14, "AVERAGE", EventReceiver.COLOR_BLUE);
				receiver.drawMenuFont(engine, playerID, 2, 15, GeneralUtil.getTime(sectionavgtime));
			}
		} else if(engine.statc[1] == 2) {
			drawResultStats(engine, playerID, receiver, 2, EventReceiver.COLOR_BLUE,
					Statistic.LPM, Statistic.SPM, Statistic.PIECE, Statistic.PPS);
		}
	}

	/**
	 * This function will be called when the replay data is going to be saved
	 */
	@Override
	public void saveReplay(GameEngine engine, int playerID, CustomProperties prop) {
		saveSetting(prop);

		if((owner.replayMode == false) && (startlevel == 0) && (always20g == false) && (big == false) && (engine.ai == null)) {
			updateRanking(engine.statistics.score, engine.statistics.level, engine.statistics.time);
			if(sectionAnyNewRecord) updateBestSectionTime();

			if((rankingRank != -1) || (sectionAnyNewRecord)) {
				saveRanking(owner.modeConfig, engine.ruleopt.strRuleName);
				receiver.saveModeConfig(owner.modeConfig);
			}
		}
	}

	/**
	 * Load the ranking
	 */
	private void loadRanking(CustomProperties prop, String ruleName) {
		String itemPrefix = enableitem ? "item." : "";
		for(int i = 0; i < RANKING_MAX; i++) {
			rankingScore[i] = prop.getProperty("scoreattack.ranking." + itemPrefix + ruleName + ".score." + i, 0);
			rankingLevel[i] = prop.getProperty("scoreattack.ranking." + itemPrefix + ruleName + ".level." + i, 0);
			rankingTime[i] = prop.getProperty("scoreattack.ranking." + itemPrefix + ruleName + ".time." + i, 0);
		}
		for(int i = 0; i < SECTION_MAX; i++) {
			bestSectionTime[i] = prop.getProperty("scoreattack.bestSectionTime." + itemPrefix + ruleName + "." + i, DEFAULT_SECTION_TIME);
		}
	}

	/**
	 * Save the ranking
	 */
	private void saveRanking(CustomProperties prop, String ruleName) {
		String itemPrefix = enableitem ? "item." : "";
		for(int i = 0; i < RANKING_MAX; i++) {
			prop.setProperty("scoreattack.ranking." + itemPrefix + ruleName + ".score." + i, rankingScore[i]);
			prop.setProperty("scoreattack.ranking." + itemPrefix + ruleName + ".level." + i, rankingLevel[i]);
			prop.setProperty("scoreattack.ranking." + itemPrefix + ruleName + ".time." + i, rankingTime[i]);
		}
		for(int i = 0; i < SECTION_MAX; i++) {
			prop.setProperty("scoreattack.bestSectionTime." + itemPrefix + ruleName + "." + i, bestSectionTime[i]);
		}
	}

	/**
	 * Update the ranking
	 */
	private void updateRanking(int sc, int lv, int time) {
		rankingRank = checkRanking(sc, lv, time);
		RankingHelper.insertAt(rankingRank, RANKING_MAX,
			(to, from) -> {
				rankingScore[to] = rankingScore[from];
				rankingLevel[to] = rankingLevel[from];
				rankingTime[to] = rankingTime[from];
			},
			rank -> {
				rankingScore[rank] = sc;
				rankingLevel[rank] = lv;
				rankingTime[rank] = time;
			});
	}

	/**
	 * This function will check the ranking and returns which place you are. (-1: Out of rank)
	 */
	private int checkRanking(int sc, int lv, int time) {
		return RankingHelper.findRank(RANKING_MAX, i ->
			(sc > rankingScore[i])
				|| ((sc == rankingScore[i]) && (lv > rankingLevel[i]))
				|| ((sc == rankingScore[i]) && (lv == rankingLevel[i]) && (time < rankingTime[i])));
	}
}
