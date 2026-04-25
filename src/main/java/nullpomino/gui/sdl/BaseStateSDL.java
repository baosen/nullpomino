// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

/**
 * Base class for game states in the SDL3 frontend.
 */
public class BaseStateSDL {
	/**
	 * Called when entering this state
	 */
	public void enter() {}

	/**
	 * Called when leaving this state
	 */
	public void leave() {}

	/**
	 * Draw the game screen to the SDL3 renderer
	 */
	public void render() {}

	/**
	 * Update game state
	 */
	public void update() {}
}
