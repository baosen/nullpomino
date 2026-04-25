// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.subsystem.ai;

import nullpomino.game.component.Controller;
import nullpomino.game.play.GameEngine;

/**
 * AIInterface
 */
public interface AIPlayer {
	/**
	 * NameGet the
	 * @return AIOfName
	 */
	public String getName();

	/**
	 * Called at initialization
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	public void init(GameEngine engine, int playerID);

	/**
	 * End processing
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	public void shutdown(GameEngine engine, int playerID);

	/**
	 *  Set button input states
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 * @param ctrl Button inputState management class
	 */
	public void setControl(GameEngine engine, int playerID, Controller ctrl);

	/**
	 * Called at the start of each frame
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	public void onFirst(GameEngine engine, int playerID);

	/**
	 * Called after every frame
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	public void onLast(GameEngine engine, int playerID);

	/**
	 * Called to display internal state
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	public void renderState(GameEngine engine, int playerID);

	/**
	 * Happens when a new piece appeared
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	public void newPiece(GameEngine engine, int playerID);

	/**
	 * Called to display additional hint information
	 * @param engine The GameEngine that owns this AI
	 * @param playerID Player ID
	 */
	public void renderHint(GameEngine engine, int playerID);
}
