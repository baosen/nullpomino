package mu.nu.nullpo.gui.sdl;

import sdljava.event.SDLKey;

/**
 * Shared utility for Page Up / Page Down edge detection in SDL menus.
 * Call {@link #checkPageEvent()} once per frame from the active menu state.
 * Returns -1 for a page-up event, 1 for a page-down event, or 0 for no event.
 */
public class PageNavigationSDL {
	/** Previous frame key state for edge detection */
	private static boolean prevPageUpPressed, prevPageDownPressed;

	/**
	 * Poll Page Up / Page Down keys with edge detection.
	 * Must be called exactly once per frame (only one menu state is active at a time).
	 * @return -1 on page-up press, 1 on page-down press, 0 otherwise
	 */
	public static int checkPageEvent() {
		boolean up = NullpoMinoSDL.keyPressedState[SDLKey.SDLK_PAGEUP];
		boolean down = NullpoMinoSDL.keyPressedState[SDLKey.SDLK_PAGEDOWN];
		int result = 0;
		if(up && !prevPageUpPressed) result = -1;
		else if(down && !prevPageDownPressed) result = 1;
		prevPageUpPressed = up;
		prevPageDownPressed = down;
		return result;
	}

	/**
	 * Jump cursor to min or max based on a page event, playing the cursor sound on change.
	 * @param pageEvent result from {@link #checkPageEvent()}
	 * @param current current cursor position
	 * @param min minimum cursor position (page-up target)
	 * @param max maximum cursor position (page-down target)
	 * @return new cursor position (unchanged if no page event)
	 */
	public static int jumpToEnd(int pageEvent, int current, int min, int max) {
		if(pageEvent == -1 && current != min) {
			ResourceHolderSDL.soundManager.play("cursor");
			return min;
		}
		if(pageEvent == 1 && current != max) {
			ResourceHolderSDL.soundManager.play("cursor");
			return max;
		}
		return current;
	}
}
