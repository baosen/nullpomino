package nullpomino.gui.sdl.binding;

import nullpomino.gui.sdl.binding.Ref.IntRef;
import nullpomino.gui.sdl.binding.SdlHandles.SdlFont;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/**
 * Backend-neutral interface to SDL3_ttf.
 *
 * SDL3_ttf API notes (vs SDL2_ttf):
 * - TTF_RenderText_Blended takes (font, text, length, fg) where length=0 means NUL-terminated
 * - TTF_OpenFont still takes (file, ptsize)
 * - SDL_Color is passed by value
 */
public interface SDL3TTF {
	SDL3TTF INSTANCE = SdlBackend.get().ttf();

	boolean TTF_Init();
	void TTF_Quit();

	/**
	 * Open a font file at the given point size.
	 * @return font handle, or null on failure
	 */
	SdlFont TTF_OpenFont(String file, float ptsize);

	void TTF_CloseFont(SdlFont font);

	/**
	 * Render text to a new surface with blended (anti-aliased) quality.
	 * @param font font handle
	 * @param text the text string
	 * @param length string length, or 0 for NUL-terminated
	 * @param fg foreground color
	 * @return surface handle, or null on failure
	 */
	SdlSurface TTF_RenderText_Blended(SdlFont font, String text, int length, SDLStructs.SDL_Color.ByValue fg);

	boolean TTF_GetStringSize(SdlFont font, String text, int length, IntRef w, IntRef h);
}
