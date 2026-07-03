package nullpomino.gui.sdl.binding.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;

/**
 * JNA interface to SDL3_ttf (libSDL3_ttf.so / SDL3_ttf.dll).
 */
interface RawSDL3TTF extends Library {

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
	 * @return SDL_Surface*, or null on failure
	 */
	Pointer TTF_RenderText_Blended(Pointer font, String text, int length, JnaStructs.SDL_Color.ByValue fg);

	boolean TTF_GetStringSize(Pointer font, String text, int length,
		IntByReference w, IntByReference h);
}
