package mu.nu.nullpo.gui.sdl.binding;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA interface to SDL3_ttf (libSDL3_ttf.so / SDL3_ttf.dll).
 *
 * SDL3_ttf API changed from SDL2_ttf:
 * - TTF_RenderText_Blended takes (font, text, length, fg) where length=0 means NUL-terminated
 * - TTF_OpenFont still takes (file, ptsize)
 * - SDL_Color is passed by value (4 bytes: r,g,b,a)
 */
public interface SDL3TTF extends Library {
	SDL3TTF INSTANCE = Native.load("SDL3_ttf", SDL3TTF.class);

	boolean TTF_Init();
	void TTF_Quit();

	/**
	 * Open a font file at the given point size.
	 * @return TTF_Font* pointer, or null on failure
	 */
	Pointer TTF_OpenFont(String file, float ptsize);

	void TTF_CloseFont(Pointer font);

	/**
	 * Render text to a new SDL_Surface* with blended (anti-aliased) quality.
	 * @param font TTF_Font*
	 * @param text the text string
	 * @param length string length, or 0 for NUL-terminated
	 * @param fg foreground color as SDL_Color passed by value (packed as int: RGBA)
	 * @return SDL_Surface*, or null on failure
	 */
	Pointer TTF_RenderText_Blended(Pointer font, String text, int length, SDLStructs.SDL_Color.ByValue fg);

	boolean TTF_GetStringSize(Pointer font, String text, int length,
		com.sun.jna.ptr.IntByReference w, com.sun.jna.ptr.IntByReference h);
}
