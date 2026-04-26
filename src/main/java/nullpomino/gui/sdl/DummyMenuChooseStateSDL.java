// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

/**
 * Dummy class for menus where the player picks from a list of options
 */
public abstract class DummyMenuChooseStateSDL extends BaseStateSDL {
	/** Cursor position */
	protected int cursor = 0;

	/** Max cursor value */
	protected int maxCursor;

	/** Top choice's y-coordinate */
	protected int minChoiceY;

	/** Set to false to ignore mouse input */
	protected boolean mouseEnabled;

	public DummyMenuChooseStateSDL () {
		maxCursor = -1;
		minChoiceY = 3;
		mouseEnabled = true;
	}

	@Override
	public void update()
	{
		// Mouse
		boolean mouseConfirm = false;
		if (mouseEnabled)
			mouseConfirm = updateMouseInput();

		if (maxCursor >= 0) {

			// Cursor movement
			if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_UP)) {
				cursor--;
				if(cursor < 0) cursor = maxCursor;
				ResourceHolderSDL.soundManager.play("cursor");
			}
			if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_DOWN)) {
				cursor++;
				if(cursor > maxCursor) cursor = 0;
				ResourceHolderSDL.soundManager.play("cursor");
			}

			// Page Up / Page Down
			int pageEvent = PageNavigationSDL.checkPageEvent();
			if(pageEvent != 0) onPageEvent(pageEvent);

			int change = 0;
			if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_LEFT)) change = -1;
			if(GameKeySDL.gamekey[0].isMenuRepeatKey(GameKeySDL.BUTTON_RIGHT)) change = 1;

			if(change != 0)
				onChange(change);

			// Decision button
			if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_A) || mouseConfirm) {
				if (onDecide())
					return;
			}

		}
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_D)) {
			if (onPushButtonD()) return;
		}

		// Cancel button. Escape is checked directly so the back affordance
		// works even if the user has remapped BUTTON_B to another key.
		if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_B)
				|| NullpoMinoSDL.isEscapePushedThisFrame()
				|| MouseInputSDL.mouseInput.isMouseRightClicked()
				|| MouseInputSDL.mouseInput.isMouseBackClicked()) {
			if (onCancel()) return;
		}
	}

	protected boolean updateMouseInput()
	{
		MouseInputSDL.mouseInput.update();

		// Hover: when the pointer moves, slide the cursor to the row it's
		// over. Gated on isMouseMoved() so a stationary pointer can't fight
		// keyboard / wheel navigation — if the mouse stays put while the
		// user arrows around, the cursor stays wherever they put it.
		// Doesn't return true, so hover never confirms; confirmation stays
		// bound to click / BUTTON_A below.
		if (MouseInputSDL.mouseInput.isMouseMoved())
		{
			int y = MouseInputSDL.mouseInput.getMouseY() >> 4;
			int newCursor = y - minChoiceY;
			if (newCursor >= 0 && newCursor <= maxCursor && newCursor != cursor)
			{
				cursor = newCursor;
				ResourceHolderSDL.soundManager.play("cursor");
			}
		}

		if (MouseInputSDL.mouseInput.isMouseClicked())
		{
			int y = MouseInputSDL.mouseInput.getMouseY() >> 4;
			int newCursor = y - minChoiceY;
			if (newCursor >= 0 && newCursor <= maxCursor)
			{
				cursor = newCursor;
				return true;
			}
		}
		return false;
	}

	protected void renderChoices(int x, String[] choices)
	{
		renderChoices(x, minChoiceY, choices);
	}

	protected void renderChoices(int x, int y, String[] choices)
	{
		NormalFontSDL.printFontGrid(x-1, y+cursor, "b", NormalFontSDL.COLOR_RED);
		for (int i = 0; i < choices.length; i++)
			NormalFontSDL.printFontGrid(x, y+i, choices[i], (cursor == i));
	}

	/**
	 * Called when left or right is pressed.
	 */
	protected void onChange(int change) {
	}

	/**
	 * Called on a decide operation (left click on an entry or select button).
	 * @return True to skip all further update processing, false otherwise.
	 */
	protected boolean onDecide() {
		return false;
	}

	/**
	 * Called on a cancel operation (right click or cancel button).
	 * @return True to skip all further update processing, false otherwise.
	 */
	protected boolean onCancel() {
		return false;
	}

	/**
	 * Called when D button is pushed.
	 * Currently, this is the only one needed; methods for other buttons can be added if needed.
	 * @return True to skip all further update processing, false otherwise.
	 */
	protected boolean onPushButtonD() {
		return false;
	}

	/**
	 * Called on a page up (-1) or page down (1) event.
	 * Default behavior: jump to start (page up) or end (page down) of the menu.
	 * Override to customize (e.g. scroll by one page in scroll-bar menus).
	 * @param direction -1 for page up, 1 for page down
	 */
	protected void onPageEvent(int direction) {
		cursor = PageNavigationSDL.jumpToEnd(direction, cursor, 0, maxCursor);
	}
}
