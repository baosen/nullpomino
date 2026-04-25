// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.subsystem.mode;

import nullpomino.game.component.Block;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

/**
 * Game mode interface
 */
public interface GameMode {
	/**
	 * Get mode name.
	 * @return Mode name
	 */
	public String getName();

	/**
	 * Get (max) number of players.
	 * @return Number of players
	 */
	public int getPlayers();

	/**
	 * Get game style.
	 * @return Game style of this mode (0:Tetromino, 1:Avalanche, 2:Physician, 3:SPF)
	 */
	public int getGameStyle();

	/**
	 * Initialization of game mode. Executed before the game screen appears.
	 * @param manager GameManager that owns this mode
	 */
	public void modeInit(GameManager manager);

	/**
	 * Initialization for each player.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void playerInit(GameEngine engine, int playerID);

	/**
	 * Executed after Ready->Go, before the first piece appears.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void startGame(GameEngine engine, int playerID);

	/**
	 * Executed at the start of each frame.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onFirst(GameEngine engine, int playerID);

	/**
	 * Executed at the end of each frame. You can update your own timers here.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void onLast(GameEngine engine, int playerID);

	/**
	 * Settings screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you don't want to start the game yet. false if settings are done.
	 */
	public boolean onSetting(GameEngine engine, int playerID);

	/**
	 * Ready->Go screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onReady(GameEngine engine, int playerID);

	/**
	 * Piece movement screen. This is where the player can move/rotate/drop current piece.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onMove(GameEngine engine, int playerID);

	/**
	 * "Lock flash" screen. Certain rules may skip this screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onLockFlash(GameEngine engine, int playerID);

	/**
	 * During line clear.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onLineClear(GameEngine engine, int playerID);

	/**
	 * During ARE.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onARE(GameEngine engine, int playerID);

	/**
	 * During ending-start sequence.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onEndingStart(GameEngine engine, int playerID);

	/**
	 * "Custom" screen. Any game mode can use this screen freely.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return This is ignored.
	 */
	public boolean onCustom(GameEngine engine, int playerID);

	/**
	 * "Excellent!" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onExcellent(GameEngine engine, int playerID);

	/**
	 * "Game Over" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onGameOver(GameEngine engine, int playerID);

	/**
	 * End-of-game results screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onResult(GameEngine engine, int playerID);

	/**
	 * Field editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean onFieldEdit(GameEngine engine, int playerID);

	/**
	 * Executed at the start of each frame.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderFirst(GameEngine engine, int playerID);

	/**
	 * Executed at the end of each frame. You can render HUD here.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderLast(GameEngine engine, int playerID);

	/**
	 * Render settings screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderSetting(GameEngine engine, int playerID);

	/**
	 * Render Ready->Go screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderReady(GameEngine engine, int playerID);

	/**
	 * Render piece movement screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderMove(GameEngine engine, int playerID);

	/**
	 * Render "Lock flash" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderLockFlash(GameEngine engine, int playerID);

	/**
	 * Render line clear screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderLineClear(GameEngine engine, int playerID);

	/**
	 * Render ARE screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderARE(GameEngine engine, int playerID);

	/**
	 * Render "ending start sequence" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderEndingStart(GameEngine engine, int playerID);

	/**
	 * Render "Custom" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderCustom(GameEngine engine, int playerID);

	/**
	 * Render "Excellent!" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderExcellent(GameEngine engine, int playerID);

	/**
	 * Render "Game Over" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderGameOver(GameEngine engine, int playerID);

	/**
	 * Render results screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderResult(GameEngine engine, int playerID);

	/**
	 * Render field editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderFieldEdit(GameEngine engine, int playerID);

	/**
	 * Render player input.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void renderInput(GameEngine engine, int playerID);

	/**
	 * Executed when a block gets destroyed in line-clear screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk Block
	 */
	public void blockBreak(GameEngine engine, int playerID, int x, int y, Block blk);

	/**
	 * Calculate score. Executed before pieceLocked. Please note this event will be called even if no lines are cleared!
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param lines Number of lines. Can be zero.
	 */
	public void calcScore(GameEngine engine, int playerID, int lines);

	/**
	 * After soft drop is used
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param fall Number of rows
	 */
	public void afterSoftDropFall(GameEngine engine, int playerID, int fall);

	/**
	 * After hard drop is used
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param fall Number of rows
	 */
	public void afterHardDropFall(GameEngine engine, int playerID, int fall);

	/**
	 * Executed after the player exits field-editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void fieldEditExit(GameEngine engine, int playerID);

	/**
	 * When the current piece locked (Executed befotre calcScore)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param lines Number of lines. Can be zero.
	 */
	public void pieceLocked(GameEngine engine, int playerID, int lines);

	/**
	 * When line clear ends
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public boolean lineClearEnd(GameEngine engine, int playerID);

	/**
	 * Called when saving replay
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param prop CustomProperties of replay file (You can write additional settings here)
	 */
	public void saveReplay(GameEngine engine, int playerID, CustomProperties prop);

	/**
	 * Called when a replay file is loaded
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param prop CustomProperties of replay file (You can read additional settings here)
	 */
	public void loadReplay(GameEngine engine, int playerID, CustomProperties prop);

	/**
	 * Is netplay-only mode?
	 * @return true if this is netplay-only mode.
	 */
	public boolean isNetplayMode();

	/**
	 * Is VS mode?
	 * @return true if this is multiplayer mode.
	 */
	public boolean isVSMode();

	/**
	 * Initialization for netplay.
	 * @param obj Any object (Currently NetLobbyFrame)
	 */
	public void netplayInit(Object obj);

	/**
	 * When the mode unloads during netplay (Called when mode change happens)
	 * @param obj Any object (Currently NetLobbyFrame)
	 */
	public void netplayUnload(Object obj);

	/**
	 * Called when retry key is pressed during netplay
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public void netplayOnRetryKey(GameEngine engine, int playerID);
}
