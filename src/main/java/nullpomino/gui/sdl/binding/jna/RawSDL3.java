package nullpomino.gui.sdl.binding.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;

/**
 * JNA interface to the core SDL3 library (libSDL3.so / SDL3.dll).
 *
 * SDL3 functions return C {@code bool} ({@code _Bool}, 1 byte).
 * JNA's {@code boolean} maps to C {@code int} (4 bytes), which
 * can cause a size mismatch through libffi.  We use {@code byte}
 * for all SDL3 bool returns and check {@code != 0} at call sites.
 */
interface RawSDL3 extends Library {

	// --- Initialization ---
	byte SDL_Init(int flags);
	void SDL_Quit();
	String SDL_GetError();

	// --- Window ---
	Pointer SDL_CreateWindow(String title, int w, int h, long flags);
	void SDL_DestroyWindow(Pointer window);
	byte SDL_SetWindowTitle(Pointer window, String title);
	byte SDL_SetWindowFullscreen(Pointer window, int fullscreen);

	// --- Renderer ---
	Pointer SDL_CreateRenderer(Pointer window, String name);
	void SDL_DestroyRenderer(Pointer renderer);
	byte SDL_RenderClear(Pointer renderer);
	byte SDL_RenderPresent(Pointer renderer);
	byte SDL_SetRenderDrawColor(Pointer renderer, byte r, byte g, byte b, byte a);
	byte SDL_SetRenderDrawBlendMode(Pointer renderer, int blendMode);
	byte SDL_RenderFillRect(Pointer renderer, JnaStructs.SDL_FRect rect);
	byte SDL_RenderRect(Pointer renderer, JnaStructs.SDL_FRect rect);
	byte SDL_SetRenderLogicalPresentation(Pointer renderer, int w, int h, int mode);
	byte SDL_RenderCoordinatesFromWindow(Pointer renderer, float window_x, float window_y,
		FloatByReference x, FloatByReference y);

	// --- Texture ---
	Pointer SDL_CreateTextureFromSurface(Pointer renderer, Pointer surface);
	void SDL_DestroyTexture(Pointer texture);
	byte SDL_SetTextureAlphaMod(Pointer texture, byte alpha);
	byte SDL_SetTextureBlendMode(Pointer texture, int blendMode);
	byte SDL_SetTextureScaleMode(Pointer texture, int scaleMode);
	byte SDL_GetTextureSize(Pointer texture, FloatByReference w, FloatByReference h);
	byte SDL_RenderTexture(Pointer renderer, Pointer texture,
		JnaStructs.SDL_FRect srcrect, JnaStructs.SDL_FRect dstrect);
	byte SDL_RenderTextureRotated(Pointer renderer, Pointer texture,
		JnaStructs.SDL_FRect srcrect, JnaStructs.SDL_FRect dstrect,
		double angle, Pointer center, int flip);

	// --- Surface ---
	void SDL_DestroySurface(Pointer surface);
	byte SDL_SaveBMP(Pointer surface, String file);
	Pointer SDL_RenderReadPixels(Pointer renderer, JnaStructs.SDL_Rect rect);

	// --- Events ---
	byte SDL_PollEvent(Pointer event);

	// --- Text input (IME-aware typing) ---
	byte SDL_StartTextInput(Pointer window);
	byte SDL_StopTextInput(Pointer window);
	byte SDL_SetTextInputArea(Pointer window, JnaStructs.SDL_Rect rect, int cursor);

	// --- Clipboard (text returned is malloc'd by SDL; must be SDL_free'd) ---
	Pointer SDL_GetClipboardText();
	byte SDL_SetClipboardText(String text);

	// --- Mouse ---
	int SDL_GetMouseState(FloatByReference x, FloatByReference y);

	// --- Gamepad ---
	Pointer SDL_GetJoysticks(int[] count);
	byte SDL_IsGamepad(int instance_id);
	Pointer SDL_OpenGamepad(int instance_id);
	void SDL_CloseGamepad(Pointer gamepad);
	byte SDL_GetGamepadButton(Pointer gamepad, int button);
	short SDL_GetGamepadAxis(Pointer gamepad, int axis);
	// const char* owned by SDL — JNA String return copies, must not be freed
	String SDL_GetGamepadNameForID(int instance_id);

	// --- Message Box ---
	byte SDL_ShowSimpleMessageBox(int flags, String title, String message, Pointer window);

	// --- Timer ---
	long SDL_GetTicks();

	// --- Memory ---
	void SDL_free(Pointer mem);
}
