// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

import java.util.ArrayList;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.menu.AbstractMenuItem;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

/**
 * Dummy implementation of game mode. Used as a base of most game modes.
 */
public abstract class AbstractMode implements GameMode {

	/** Total score */
	protected static enum Statistic { SCORE, LINES, TIME,
			LEVEL, LEVEL_MANIA, PIECE,
			MAXCOMBO, SPL, SPM, SPS,
			LPM, LPS, PPM, PPS,
			MAXCHAIN, LEVEL_ADD_DISP};

	/** GameManager that owns this mode */
	protected GameManager owner;

	/** Drawing and event handling EventReceiver */
	protected EventReceiver receiver;

	/** Current state of menu for drawMenu */
	protected int statcMenu, menuColor, menuY;

	protected ArrayList<AbstractMenuItem<?>> menu;
	
	/** Name of mode in properties file */
	protected String propName;
	
	/** Position of cursor in menu */
	protected int menuCursor;
	
	/** Number of frames spent in menu */
	protected int menuTime;

	public AbstractMode() {
		statcMenu = 0;
		menuCursor = 0;
		menuTime = 0;
		menuColor = EventReceiver.COLOR_WHITE;
		menuY = 0;
		menu = new ArrayList<AbstractMenuItem<?>>();
		propName = "dummy";
	}

	protected void loadSetting(CustomProperties prop) {
		for (AbstractMenuItem<?> item : menu)
			item.load(-1, prop, propName);
	}

	protected void saveSetting(CustomProperties prop) {
		for (AbstractMenuItem<?> item : menu)
			item.save(-1, prop, propName);
	}

	protected final void addMenuItems(AbstractMenuItem<?>... items) {
		for(AbstractMenuItem<?> item : items) {
			menu.add(item);
		}
	}

	public String getName() {
		return "DUMMY";
	}

	public int getPlayers() {
		return 1;
	}

	public int getGameStyle() {
		return GameEngine.GAMESTYLE_TETROMINO;
	}

	public void modeInit(GameManager manager) {
	}

	public void playerInit(GameEngine engine, int playerID) {
		owner = engine.owner;
		receiver = engine.owner.receiver;
	}

	public void renderSetting(GameEngine engine, int playerID) {
		//TODO: Custom page breaks
		AbstractMenuItem<?> menuItem;
		int pageNum = menuCursor / 10;
		int pageStart = pageNum * 10;
		int endPage = Math.min(menu.size(), pageStart+10);
		for (int i = pageStart; i < endPage; i++)
		{
			menuItem = menu.get(i);
			receiver.drawMenuFont(engine, playerID, 0, i << 1, menuItem.displayName, menuItem.color);
			if (menuCursor == i && !engine.owner.replayMode)
				receiver.drawMenuFont(engine, playerID, 0, (i << 1) + 1, "b" + menuItem.getValueString(), true);
			else
				receiver.drawMenuFont(engine, playerID, 1, (i << 1) + 1, menuItem.getValueString());
		}
	}

	@Override
	public int getMenuCursor() {
		return menuCursor;
	}

	@Override
	public void setMenuCursor(int cursor) {
		menuCursor = cursor;
	}

	@Override
	public int getMenuItemCount() {
		return menu.isEmpty() ? -1 : menu.size();
	}

	/**
	 * Update menu cursor
	 * @param engine GameEngine
	 * @param maxCursor Max value of cursor position
	 * @return -1 if Left key is pressed, 1 if Right key is pressed, 0 otherwise
	 */
	protected int updateCursor(GameEngine engine, int maxCursor) {
		return updateCursor(engine, maxCursor, 0);
	}

	/**
	 * Update menu cursor
	 * @param engine GameEngine
	 * @param maxCursor Max value of cursor position
	 * @param playerID Player ID (unused)
	 * @return -1 if Left key is pressed, 1 if Right key is pressed, 0 otherwise
	 */
	protected int updateCursor (GameEngine engine, int maxCursor, int playerID) {
		// Up
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_UP)) {
			menuCursor--;
			if(menuCursor < 0) menuCursor = maxCursor;
			engine.playSE("cursor");
		}
		// Down
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_DOWN)) {
			menuCursor++;
			if(menuCursor > maxCursor) menuCursor = 0;
			engine.playSE("cursor");
		}

		// Configuration changes
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_LEFT)) return -1;
		if(engine.ctrl.isMenuRepeatKey(Controller.BUTTON_RIGHT)) return 1;
		return 0;
	}

	protected void updateMenu(GameEngine engine) {
		// Configuration changes
		int change = updateCursor(engine, menu.size()-1);

		if(change != 0) {
			engine.playSE("change");
			int fast = 0;
			if (engine.ctrl.isPush(Controller.BUTTON_E)) fast++;
			if (engine.ctrl.isPush(Controller.BUTTON_F)) fast += 2;
			menu.get(menuCursor).change(change, fast);
		}
	}

	protected void initMenu (int y, int color, int statc) {
		menuY = y;
		menuColor = color;
		statcMenu = statc;
	}

	protected void initMenu (int color, int statc) {
		menuY = 0;
		statcMenu = statc;
		menuColor = color;
	}

	protected void drawMenu (GameEngine engine, int playerID, EventReceiver receiver, String... str) {
		for (int i = 0; i < str.length; i++)
		{
			if ((i&1) == 0)
				receiver.drawMenuFont(engine, playerID, 0, menuY, str[i], menuColor);
			else if (menuCursor == statcMenu && !engine.owner.replayMode)
			{
				receiver.drawMenuFont(engine, playerID, 0, menuY, "b" + str[i], true);
				statcMenu++;
			}
			else {
				receiver.drawMenuFont(engine, playerID, 1, menuY, str[i]);
				statcMenu++;
			}
			menuY++;
		}
	}

	protected void drawMenu (GameEngine engine, int playerID, EventReceiver receiver,
			int y, int color, int statc, String... str) {
		menuY = y;
		menuColor = color;
		statcMenu = statc;
		drawMenu(engine, playerID, receiver, str);
	}

	protected void drawMenuCompact (GameEngine engine, int playerID, EventReceiver receiver, String... str) {
		for (int i = 0; i < str.length-1; i+= 2)
		{
			receiver.drawMenuFont(engine, playerID, 1, menuY, str[i] + ":", menuColor);
			if (menuCursor == statcMenu && !engine.owner.replayMode)
			{
				receiver.drawMenuFont(engine, playerID, 0, menuY, "b", true);
				receiver.drawMenuFont(engine, playerID, str[i].length()+2, menuY, str[i+1], true);
			}
			else
				receiver.drawMenuFont(engine, playerID, str[i].length()+2, menuY, str[i+1]);
			statcMenu++;
			menuY++;
		}
	}

	protected void drawMenuCompact (GameEngine engine, int playerID, EventReceiver receiver,
			int y, int color, int statc, String... str) {
		menuY = y;
		menuColor = color;
		statcMenu = statc;
		drawMenuCompact(engine, playerID, receiver, str);
	}

	protected void drawResult (GameEngine engine, int playerID, EventReceiver receiver, int y, int color, String... str) {
		drawResultScale(engine, playerID, receiver, y, color, 1.0f, str);
	}
	protected void drawResultScale (GameEngine engine, int playerID, EventReceiver receiver, int y, int color, float scale, String... str) {
		for (int i = 0; i < str.length; i++)
			receiver.drawMenuFont(engine, playerID, 0, y+i, str[i], ((i&1) == 0) ? color : EventReceiver.COLOR_WHITE, scale);
	}
	protected void drawResultRank (GameEngine engine, int playerID, EventReceiver receiver, int y, int color, int rank) {
		drawResultRankScale(engine, playerID, receiver, y, color, 1.0f, rank);
	}
	protected void drawResultRankScale (GameEngine engine, int playerID, EventReceiver receiver, int y, int color, float scale, int rank) {
		if(rank != -1) {
			receiver.drawMenuFont(engine, playerID, 0, y, "RANK", color, scale);
			receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", rank + 1), scale);
		}
	}
	protected void drawResultNetRank (GameEngine engine, int playerID, EventReceiver receiver, int y, int color, int rank) {
		drawResultNetRankScale(engine, playerID, receiver, y, color, 1.0f, rank);
	}
	protected void drawResultNetRankScale (GameEngine engine, int playerID, EventReceiver receiver, int y, int color, float scale, int rank) {
		if(rank != -1) {
			receiver.drawMenuFont(engine, playerID, 0, y, "NET-RANK", color, scale);
			receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", rank + 1), scale);
		}
	}
	protected void drawResultNetRankDaily(GameEngine engine, int playerID, EventReceiver receiver, int y, int color, int rank) {
		drawResultNetRankDailyScale(engine, playerID, receiver, y, color, 1.0f, rank);
	}
	protected void drawResultNetRankDailyScale(GameEngine engine, int playerID, EventReceiver receiver, int y, int color, float scale, int rank) {
		if(rank != -1) {
			receiver.drawMenuFont(engine, playerID, 0, y, "DAILY-RANK", color, scale);
			receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", rank + 1), scale);
		}
	}
	protected void drawResultStats (GameEngine engine, int playerID, EventReceiver receiver, int y, int color, Statistic ... stats) {
		drawResultStatsScale(engine, playerID, receiver, y, color, 1.0f, stats);
	}
	protected void drawResultStatsScale (GameEngine engine, int playerID, EventReceiver receiver, int y, int color, float scale, Statistic ... stats) {
		for (int i = 0; i < stats.length; i++)
		{
			switch(stats[i]) {
				case SCORE:
					receiver.drawMenuFont(engine, playerID, 0, y, "SCORE", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", engine.statistics.score), scale);
					break;
				case LINES:
					receiver.drawMenuFont(engine, playerID, 0, y, "LINES", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", engine.statistics.lines), scale);
					break;
				case TIME:
					receiver.drawMenuFont(engine, playerID, 0, y, "TIME", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10s", GeneralUtil.getTime(engine.statistics.time)), scale);
					break;
				case LEVEL:
					receiver.drawMenuFont(engine, playerID, 0, y, "LEVEL", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", engine.statistics.level + 1), scale);
					break;
				case LEVEL_MANIA:
					receiver.drawMenuFont(engine, playerID, 0, y, "LEVEL", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", engine.statistics.level), scale);
					break;
				case PIECE:
					receiver.drawMenuFont(engine, playerID, 0, y, "PIECE", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", engine.statistics.totalPieceLocked), scale);
					break;
				case MAXCOMBO:
					receiver.drawMenuFont(engine, playerID, 0, y, "MAX COMBO", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", engine.statistics.maxCombo - 1), scale);
					break;
				case SPL:
					receiver.drawMenuFont(engine, playerID, 0, y, "SCORE/LINE", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10s", GeneralUtil.formatG(engine.statistics.spl, 6)), scale);
					break;
				case SPM:
					receiver.drawMenuFont(engine, playerID, 0, y, "SCORE/MIN", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10s", GeneralUtil.formatG(engine.statistics.spm, 6)), scale);
					break;
				case SPS:
					receiver.drawMenuFont(engine, playerID, 0, y, "SCORE/SEC", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10s", GeneralUtil.formatG(engine.statistics.sps, 6)), scale);
					break;
				case LPM:
					receiver.drawMenuFont(engine, playerID, 0, y, "LINE/MIN", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10s", GeneralUtil.formatG(engine.statistics.lpm, 6)), scale);
					break;
				case LPS:
					receiver.drawMenuFont(engine, playerID, 0, y, "LINE/SEC", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10s", GeneralUtil.formatG(engine.statistics.lps, 6)), scale);
					break;
				case PPM:
					receiver.drawMenuFont(engine, playerID, 0, y, "PIECE/MIN", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10s", GeneralUtil.formatG(engine.statistics.ppm, 6)), scale);
					break;
				case PPS:
					receiver.drawMenuFont(engine, playerID, 0, y, "PIECE/SEC", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10s", GeneralUtil.formatG(engine.statistics.pps, 6)), scale);
					break;
				case MAXCHAIN:
					receiver.drawMenuFont(engine, playerID, 0, y, "MAX CHAIN", color, scale);
					receiver.drawMenuFont(engine, playerID, 0, y+1, String.format("%10d", engine.statistics.maxChain), scale);
					break;
				case LEVEL_ADD_DISP:
					receiver.drawMenuFont(engine, playerID, 0, y, "LEVEL", color, scale);
					receiver.drawMenuFont(engine, playerID,0,y+1,String.format("%10d",engine.statistics.level+engine.statistics.levelDispAdd),scale);
					break;
			}
			y += 2;
		}
	}

	/** Key names for the MOVE/TURN/HOLD/DROP controls-help block. */
	private record ControlsHelpKeys(String left, String right, String cw, String ccw, String hard, String soft, String hold) {}

	private ControlsHelpKeys getControlsHelpKeys(GameEngine engine) {
		String left  = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_LEFT);
		String right = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_RIGHT);
		String cw    = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_A);
		String ccw   = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_B);
		String hard  = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_UP);
		String soft  = receiver.getKeyNameByButtonID(engine, Controller.BUTTON_DOWN);
		String hold  = (engine.ruleopt != null && engine.ruleopt.holdEnable)
				? receiver.getKeyNameByButtonID(engine, Controller.BUTTON_D) : "OFF";
		return new ControlsHelpKeys(left, right, cw, ccw, hard, soft, hold);
	}

	/**
	 * Draws a condensed 5-row in-game control reference in the score column,
	 * starting at grid row y (row y is left blank to space it from the stat
	 * above). Key names come from the current player's active in-game keymap.
	 * HOLD shows "OFF" when the rule disables hold.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param y Top grid row (score column, 16px cells)
	 */
	protected void drawControlsHelp(GameEngine engine, int playerID, int y) {
		ControlsHelpKeys k = getControlsHelpKeys(engine);
		receiver.drawScoreFont(engine, playerID, 0, y + 1, "MOVE:" + k.left() + "/" + k.right(), EventReceiver.COLOR_BLUE);
		receiver.drawScoreFont(engine, playerID, 0, y + 2, "TURN:" + k.cw() + "/" + k.ccw(),     EventReceiver.COLOR_BLUE);
		receiver.drawScoreFont(engine, playerID, 0, y + 3, "HOLD:" + k.hold(),                   EventReceiver.COLOR_BLUE);
		receiver.drawScoreFont(engine, playerID, 0, y + 4, "DROP:" + k.hard() + "/" + k.soft(),  EventReceiver.COLOR_BLUE);
	}

	/**
	 * Half-scale (8px-row) variant of {@link #drawControlsHelp} for modes with
	 * very little room left in the score column: a blank spacer row then
	 * MOVE/TURN/HOLD/DROP, each row half the height of the normal score font.
	 * y8 is in 8px grid units (double a normal 16px row index), not the same
	 * units as drawControlsHelp's y.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param y8 Top grid row (score column, 8px cells)
	 */
	protected void drawControlsHelpSmall(GameEngine engine, int playerID, int y8) {
		ControlsHelpKeys k = getControlsHelpKeys(engine);
		receiver.drawScoreFont(engine, playerID, 0, y8 + 1, "MOVE:" + k.left() + "/" + k.right(), EventReceiver.COLOR_BLUE, 0.5f);
		receiver.drawScoreFont(engine, playerID, 0, y8 + 2, "TURN:" + k.cw() + "/" + k.ccw(),     EventReceiver.COLOR_BLUE, 0.5f);
		receiver.drawScoreFont(engine, playerID, 0, y8 + 3, "HOLD:" + k.hold(),                   EventReceiver.COLOR_BLUE, 0.5f);
		receiver.drawScoreFont(engine, playerID, 0, y8 + 4, "DROP:" + k.hard() + "/" + k.soft(),  EventReceiver.COLOR_BLUE, 0.5f);
	}

	/**
	 * Same content as {@link #drawControlsHelpSmall} but with no blank spacer
	 * row, for modes with only exactly 4 half-scale rows free.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param y8 Top grid row (score column, 8px cells)
	 */
	protected void drawControlsHelpSmallTight(GameEngine engine, int playerID, int y8) {
		ControlsHelpKeys k = getControlsHelpKeys(engine);
		receiver.drawScoreFont(engine, playerID, 0, y8,     "MOVE:" + k.left() + "/" + k.right(), EventReceiver.COLOR_BLUE, 0.5f);
		receiver.drawScoreFont(engine, playerID, 0, y8 + 1, "TURN:" + k.cw() + "/" + k.ccw(),     EventReceiver.COLOR_BLUE, 0.5f);
		receiver.drawScoreFont(engine, playerID, 0, y8 + 2, "HOLD:" + k.hold(),                   EventReceiver.COLOR_BLUE, 0.5f);
		receiver.drawScoreFont(engine, playerID, 0, y8 + 3, "DROP:" + k.hard() + "/" + k.soft(),  EventReceiver.COLOR_BLUE, 0.5f);
	}

	/**
	 * Default method to render controller input display
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderInput(GameEngine engine, int playerID) {
		EventReceiver receiver = engine.owner.receiver;
		int y = 24;
		if (isVSMode() && !isNetplayMode()) {
			int color = EventReceiver.COLOR_BLUE;
			if (playerID == 0) {
				color = EventReceiver.COLOR_RED;
				y--;
			}
			receiver.drawScoreFont(engine, 0, -9, y, (playerID+1) + "P INPUT:", color);
		} else {
			receiver.drawScoreFont(engine, 0, -6, y, "INPUT:", EventReceiver.COLOR_BLUE);
		}
		Controller ctrl = engine.ctrl;
		if (ctrl.isPress(Controller.BUTTON_LEFT)) receiver.drawScoreFont(engine, 0, 0, y, "<");
		if (ctrl.isPress(Controller.BUTTON_DOWN)) receiver.drawScoreFont(engine, 0, 1, y, "n");
		if (ctrl.isPress(Controller.BUTTON_UP)) receiver.drawScoreFont(engine, 0, 2, y, "k");
		if (ctrl.isPress(Controller.BUTTON_RIGHT)) receiver.drawScoreFont(engine, 0, 3, y, ">");
		if (ctrl.isPress(Controller.BUTTON_A)) receiver.drawScoreFont(engine, 0, 4, y, "A");
		if (ctrl.isPress(Controller.BUTTON_B)) receiver.drawScoreFont(engine, 0, 5, y, "B");
		if (ctrl.isPress(Controller.BUTTON_C)) receiver.drawScoreFont(engine, 0, 6, y, "C");
		if (ctrl.isPress(Controller.BUTTON_D)) receiver.drawScoreFont(engine, 0, 7, y, "D");
		if (ctrl.isPress(Controller.BUTTON_E)) receiver.drawScoreFont(engine, 0, 8, y, "E");
		if (ctrl.isPress(Controller.BUTTON_F)) receiver.drawScoreFont(engine, 0, 9, y, "F");
	}
}
