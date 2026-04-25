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
	public default void startGame(GameEngine engine, int playerID) {
	}

	/**
	 * Executed at the start of each frame.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void onFirst(GameEngine engine, int playerID) {
	}

	/**
	 * Executed at the end of each frame. You can update your own timers here.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void onLast(GameEngine engine, int playerID) {
	}

	/**
	 * Settings screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you don't want to start the game yet. false if settings are done.
	 */
	public default boolean onSetting(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * Ready->Go screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onReady(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * Piece movement screen. This is where the player can move/rotate/drop current piece.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onMove(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * "Lock flash" screen. Certain rules may skip this screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onLockFlash(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * During line clear.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onLineClear(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * During ARE.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onARE(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * During ending-start sequence.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onEndingStart(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * "Custom" screen. Any game mode can use this screen freely.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return This is ignored.
	 */
	public default boolean onCustom(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * "Excellent!" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onExcellent(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * "Game Over" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onGameOver(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * End-of-game results screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onResult(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * Field editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean onFieldEdit(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * Executed at the start of each frame.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderFirst(GameEngine engine, int playerID) {
	}

	/**
	 * Executed at the end of each frame. You can render HUD here.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderLast(GameEngine engine, int playerID) {
	}

	/**
	 * Render settings screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderSetting(GameEngine engine, int playerID) {
	}

	/**
	 * Render Ready->Go screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderReady(GameEngine engine, int playerID) {
	}

	/**
	 * Render piece movement screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderMove(GameEngine engine, int playerID) {
	}

	/**
	 * Render "Lock flash" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderLockFlash(GameEngine engine, int playerID) {
	}

	/**
	 * Render line clear screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderLineClear(GameEngine engine, int playerID) {
	}

	/**
	 * Render ARE screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderARE(GameEngine engine, int playerID) {
	}

	/**
	 * Render "ending start sequence" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderEndingStart(GameEngine engine, int playerID) {
	}

	/**
	 * Render "Custom" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderCustom(GameEngine engine, int playerID) {
	}

	/**
	 * Render "Excellent!" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderExcellent(GameEngine engine, int playerID) {
	}

	/**
	 * Render "Game Over" screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderGameOver(GameEngine engine, int playerID) {
	}

	/**
	 * Render results screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderResult(GameEngine engine, int playerID) {
	}

	/**
	 * Render field editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void renderFieldEdit(GameEngine engine, int playerID) {
	}

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
	public default void blockBreak(GameEngine engine, int playerID, int x, int y, Block blk) {
	}

	/**
	 * Calculate score. Executed before pieceLocked. Please note this event will be called even if no lines are cleared!
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param lines Number of lines. Can be zero.
	 */
	public default void calcScore(GameEngine engine, int playerID, int lines) {
	}

	/**
	 * After soft drop is used
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param fall Number of rows
	 */
	public default void afterSoftDropFall(GameEngine engine, int playerID, int fall) {
	}

	/**
	 * After hard drop is used
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param fall Number of rows
	 */
	public default void afterHardDropFall(GameEngine engine, int playerID, int fall) {
	}

	/**
	 * Executed after the player exits field-editor screen.
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void fieldEditExit(GameEngine engine, int playerID) {
	}

	/**
	 * When the current piece locked (Executed befotre calcScore)
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param lines Number of lines. Can be zero.
	 */
	public default void pieceLocked(GameEngine engine, int playerID, int lines) {
	}

	/**
	 * When line clear ends
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @return true if you override everything of this screen (skips default behavior)
	 */
	public default boolean lineClearEnd(GameEngine engine, int playerID) {
		return false;
	}

	/**
	 * Called when saving replay
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param prop CustomProperties of replay file (You can write additional settings here)
	 */
	public default void saveReplay(GameEngine engine, int playerID, CustomProperties prop) {
	}

	/**
	 * Called when a replay file is loaded
	 * @param engine GameEngine
	 * @param playerID Player ID
	 * @param prop CustomProperties of replay file (You can read additional settings here)
	 */
	public default void loadReplay(GameEngine engine, int playerID, CustomProperties prop) {
	}

	/**
	 * Is netplay-only mode?
	 * @return true if this is netplay-only mode.
	 */
	public default boolean isNetplayMode() {
		return false;
	}

	/**
	 * Is VS mode?
	 * @return true if this is multiplayer mode.
	 */
	public default boolean isVSMode() {
		return false;
	}

	/**
	 * Initialization for netplay.
	 * @param obj Any object (Currently NetLobbyFrame)
	 */
	public default void netplayInit(Object obj) {
	}

	/**
	 * When the mode unloads during netplay (Called when mode change happens)
	 * @param obj Any object (Currently NetLobbyFrame)
	 */
	public default void netplayUnload(Object obj) {
	}

	/**
	 * Called when retry key is pressed during netplay
	 * @param engine GameEngine
	 * @param playerID Player ID
	 */
	public default void netplayOnRetryKey(GameEngine engine, int playerID) {
	}
}
