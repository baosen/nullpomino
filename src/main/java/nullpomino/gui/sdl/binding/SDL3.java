package nullpomino.gui.sdl.binding;

import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SdlHandles.SdlGamepad;
import nullpomino.gui.sdl.binding.SdlHandles.SdlJoystick;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.gui.sdl.binding.SdlHandles.SdlWindow;

/**
 * Backend-neutral interface to the core SDL3 library.
 *
 * Method names and shapes mirror the SDL3 C API. Bool-returning SDL functions
 * keep the {@code byte} return type (check {@code != 0} at call sites) so the
 * desktop JNA backend can pass them through without conversion; see the
 * original size-mismatch note: SDL3 returns C {@code _Bool} (1 byte), which
 * JNA must not map to Java {@code boolean} (4 bytes through libffi).
 */
public interface SDL3 {
	SDL3 INSTANCE = SdlBackend.get().sdl3();

	// --- Initialization ---
	byte SDL_Init(int flags);
	void SDL_Quit();
	String SDL_GetError();

	// --- Window ---
	SdlWindow SDL_CreateWindow(String title, int w, int h, long flags);
	void SDL_DestroyWindow(SdlWindow window);
	byte SDL_SetWindowTitle(SdlWindow window, String title);
	byte SDL_SetWindowFullscreen(SdlWindow window, int fullscreen);

	// --- Renderer ---
	SdlRenderer SDL_CreateRenderer(SdlWindow window, String name);
	void SDL_DestroyRenderer(SdlRenderer renderer);
	byte SDL_RenderClear(SdlRenderer renderer);
	byte SDL_RenderPresent(SdlRenderer renderer);
	byte SDL_SetRenderDrawColor(SdlRenderer renderer, byte r, byte g, byte b, byte a);
	byte SDL_SetRenderDrawBlendMode(SdlRenderer renderer, int blendMode);
	byte SDL_RenderFillRect(SdlRenderer renderer, SDLStructs.SDL_FRect rect);
	byte SDL_RenderRect(SdlRenderer renderer, SDLStructs.SDL_FRect rect);
	byte SDL_SetRenderLogicalPresentation(SdlRenderer renderer, int w, int h, int mode);
	byte SDL_RenderCoordinatesFromWindow(SdlRenderer renderer, float window_x, float window_y,
		FloatRef x, FloatRef y);

	// --- Texture ---
	SdlTexture SDL_CreateTextureFromSurface(SdlRenderer renderer, SdlSurface surface);
	void SDL_DestroyTexture(SdlTexture texture);
	byte SDL_SetTextureAlphaMod(SdlTexture texture, byte alpha);
	byte SDL_SetTextureBlendMode(SdlTexture texture, int blendMode);
	byte SDL_SetTextureScaleMode(SdlTexture texture, int scaleMode);
	byte SDL_GetTextureSize(SdlTexture texture, FloatRef w, FloatRef h);
	byte SDL_RenderTexture(SdlRenderer renderer, SdlTexture texture,
		SDLStructs.SDL_FRect srcrect, SDLStructs.SDL_FRect dstrect);
	/** Like {@link #SDL_RenderTexture} but flips the texture. {@code flip} is an
	 *  SDL_FlipMode ({@link SDLConstants#SDL_FLIP_HORIZONTAL} etc.). Used to draw
	 *  a left-pointing arrow from the atlas's only (right-pointing) arrow glyph. */
	byte SDL_RenderTextureFlipped(SdlRenderer renderer, SdlTexture texture,
		SDLStructs.SDL_FRect srcrect, SDLStructs.SDL_FRect dstrect, int flip);

	// --- Surface ---
	void SDL_DestroySurface(SdlSurface surface);
	byte SDL_SaveBMP(SdlSurface surface, String file);
	SdlSurface SDL_RenderReadPixels(SdlRenderer renderer, SDLStructs.SDL_Rect rect);

	// --- Events ---
	/** Poll the next pending event into {@code event}. Returns nonzero if one was filled. */
	byte SDL_PollEvent(SDLStructs.SDL_Event event);

	// --- Text input (IME-aware typing) ---
	byte SDL_StartTextInput(SdlWindow window);
	byte SDL_StopTextInput(SdlWindow window);
	byte SDL_SetTextInputArea(SdlWindow window, SDLStructs.SDL_Rect rect, int cursor);

	// --- Clipboard ---
	/** @return clipboard text as a Java string, or null if empty/unavailable */
	String SDL_GetClipboardText();
	byte SDL_SetClipboardText(String text);

	// --- Mouse ---
	int SDL_GetMouseState(FloatRef x, FloatRef y);

	// --- Joystick ---
	/** @return instance IDs of all connected joysticks (empty array if none) */
	int[] SDL_GetJoysticks();
	SdlJoystick SDL_OpenJoystick(int instance_id);
	void SDL_CloseJoystick(SdlJoystick joystick);
	short SDL_GetJoystickAxis(SdlJoystick joystick, int axis);
	byte SDL_GetJoystickButton(SdlJoystick joystick, int button);
	byte SDL_GetJoystickHat(SdlJoystick joystick, int hat);
	int SDL_GetNumJoystickButtons(SdlJoystick joystick);
	int SDL_GetNumJoystickHats(SdlJoystick joystick);

	// --- Gamepad ---
	/** @return nonzero if the joystick instance id has a standard gamepad mapping */
	byte SDL_IsGamepad(int instance_id);
	SdlGamepad SDL_OpenGamepad(int instance_id);
	void SDL_CloseGamepad(SdlGamepad gamepad);
	/** @param button SDL_GamepadButton ordinal ({@code SDLConstants.SDL_GAMEPAD_BUTTON_*}) */
	byte SDL_GetGamepadButton(SdlGamepad gamepad, int button);
	/** @param axis SDL_GamepadAxis ordinal ({@code SDLConstants.SDL_GAMEPAD_AXIS_*}) */
	short SDL_GetGamepadAxis(SdlGamepad gamepad, int axis);

	// --- Message Box ---
	byte SDL_ShowSimpleMessageBox(int flags, String title, String message, SdlWindow window);

	// --- Timer ---
	long SDL_GetTicks();

	// Convenience methods using int for color components
	public static void setDrawColor(SdlRenderer renderer, int r, int g, int b, int a) {
		INSTANCE.SDL_SetRenderDrawColor(renderer, (byte)r, (byte)g, (byte)b, (byte)a);
	}

	public static void setTextureAlpha(SdlTexture texture, int alpha) {
		INSTANCE.SDL_SetTextureAlphaMod(texture, (byte)alpha);
	}
}
