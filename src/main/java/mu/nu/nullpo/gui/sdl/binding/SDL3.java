package mu.nu.nullpo.gui.sdl.binding;

import com.sun.jna.Library;
import com.sun.jna.Native;
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
public interface SDL3 extends Library {
	SDL3 INSTANCE = Native.load("SDL3", SDL3.class);

	// --- Initialization ---
	byte SDL_Init(int flags);
	void SDL_Quit();
	String SDL_GetError();

	// --- Window ---
	Pointer SDL_CreateWindow(String title, int w, int h, long flags);
	void SDL_DestroyWindow(Pointer window);
	byte SDL_SetWindowTitle(Pointer window, String title);
	byte SDL_SetWindowFullscreen(Pointer window, int fullscreen);
	byte SDL_SetWindowResizable(Pointer window, int resizable);

	// --- Renderer ---
	Pointer SDL_CreateRenderer(Pointer window, String name);
	void SDL_DestroyRenderer(Pointer renderer);
	byte SDL_RenderClear(Pointer renderer);
	byte SDL_RenderPresent(Pointer renderer);
	byte SDL_SetRenderDrawColor(Pointer renderer, byte r, byte g, byte b, byte a);
	byte SDL_SetRenderDrawBlendMode(Pointer renderer, int blendMode);
	byte SDL_RenderFillRect(Pointer renderer, SDLStructs.SDL_FRect rect);
	byte SDL_RenderRect(Pointer renderer, SDLStructs.SDL_FRect rect);
	byte SDL_SetRenderLogicalPresentation(Pointer renderer, int w, int h, int mode);
	byte SDL_RenderCoordinatesFromWindow(Pointer renderer, float window_x, float window_y,
		FloatByReference x, FloatByReference y);
	byte SDL_SetRenderVSync(Pointer renderer, int vsync);

	// --- Texture ---
	Pointer SDL_CreateTextureFromSurface(Pointer renderer, Pointer surface);
	void SDL_DestroyTexture(Pointer texture);
	byte SDL_SetTextureAlphaMod(Pointer texture, byte alpha);
	byte SDL_SetTextureColorMod(Pointer texture, byte r, byte g, byte b);
	byte SDL_SetTextureBlendMode(Pointer texture, int blendMode);
	byte SDL_SetTextureScaleMode(Pointer texture, int scaleMode);
	byte SDL_GetTextureSize(Pointer texture, FloatByReference w, FloatByReference h);
	byte SDL_RenderTexture(Pointer renderer, Pointer texture,
		SDLStructs.SDL_FRect srcrect, SDLStructs.SDL_FRect dstrect);

	// --- Surface ---
	void SDL_DestroySurface(Pointer surface);
	byte SDL_SaveBMP(Pointer surface, String file);
	Pointer SDL_RenderReadPixels(Pointer renderer, SDLStructs.SDL_Rect rect);

	// --- Events ---
	byte SDL_PollEvent(Pointer event);

	// --- Keyboard ---
	Pointer SDL_GetKeyboardState(int[] numkeys);

	// --- Mouse ---
	int SDL_GetMouseState(FloatByReference x, FloatByReference y);

	// --- Gamepad ---
	byte SDL_IsGamepad(int instance_id);
	Pointer SDL_OpenGamepad(int instance_id);
	void SDL_CloseGamepad(Pointer gamepad);
	byte SDL_GetGamepadButton(Pointer gamepad, int button);
	short SDL_GetGamepadAxis(Pointer gamepad, int axis);
	Pointer SDL_GetGamepads(int[] count);

	// --- Joystick (fallback) ---
	Pointer SDL_GetJoysticks(int[] count);
	Pointer SDL_OpenJoystick(int instance_id);
	void SDL_CloseJoystick(Pointer joystick);
	short SDL_GetJoystickAxis(Pointer joystick, int axis);
	byte SDL_GetJoystickButton(Pointer joystick, int button);
	byte SDL_GetJoystickHat(Pointer joystick, int hat);
	int SDL_GetNumJoystickButtons(Pointer joystick);
	int SDL_GetNumJoystickHats(Pointer joystick);

	// --- Timer ---
	long SDL_GetTicks();

	// --- Memory ---
	void SDL_free(Pointer mem);

	// Convenience methods using int for color components
	public static void setDrawColor(Pointer renderer, int r, int g, int b, int a) {
		INSTANCE.SDL_SetRenderDrawColor(renderer, (byte)r, (byte)g, (byte)b, (byte)a);
	}

	public static void setTextureAlpha(Pointer texture, int alpha) {
		INSTANCE.SDL_SetTextureAlphaMod(texture, (byte)alpha);
	}

	public static void setTextureColor(Pointer texture, int r, int g, int b) {
		INSTANCE.SDL_SetTextureColorMod(texture, (byte)r, (byte)g, (byte)b);
	}
}
