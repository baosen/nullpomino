package nullpomino.gui.sdl.binding.teavm;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Navigator;
import org.teavm.jso.browser.Window;
import org.teavm.jso.gamepad.Gamepad;
import org.teavm.jso.gamepad.GamepadButton;

import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.SdlGamepad;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.gui.sdl.binding.SdlHandles.SdlWindow;

/**
 * TeaVM/Canvas2D implementation of the neutral {@link SDL3} interface. Mirrors
 * the desktop web backend method-for-method; screenshot is a no-op (null-guarded
 * in the frontend). Joystick/gamepad read the browser Gamepad API — see
 * {@code WebPad}. Fullscreen drives the browser Fullscreen
 * API via {@link CanvasWindow}. Clipboard uses an in-app fallback plus a
 * best-effort async write to the system clipboard.
 */
final class TeaVMSDL3 implements SDL3 {

	private final double startMillis = now();

	private String lastError = "";
	private String clipboardFallback = "";

	private CanvasWindow window;

	@Override public byte SDL_Init(int flags) { return 1; }
	@Override public void SDL_Quit() {}
	@Override public String SDL_GetError() { return lastError; }

	@Override public SdlWindow SDL_CreateWindow(String title, int w, int h, long flags) {
		window = new CanvasWindow(title);
		return window;
	}

	@Override public void SDL_DestroyWindow(SdlWindow w) {
		if (w instanceof CanvasWindow) ((CanvasWindow) w).dispose();
	}

	@Override public byte SDL_SetWindowTitle(SdlWindow w, String title) {
		((CanvasWindow) w).setTitle(title);
		return 1;
	}

	@Override public byte SDL_SetWindowFullscreen(SdlWindow w, int fullscreen) {
		if (w instanceof CanvasWindow) ((CanvasWindow) w).setFullscreen(fullscreen != 0);
		return 1;
	}

	@Override public SdlRenderer SDL_CreateRenderer(SdlWindow w, String name) {
		CanvasWindow cw = (CanvasWindow) w;
		return new CanvasRenderer(cw, cw.panelWidth(), cw.panelHeight());
	}

	@Override public void SDL_DestroyRenderer(SdlRenderer r) {
		((CanvasRenderer) r).dispose();
	}

	@Override public byte SDL_RenderClear(SdlRenderer r) {
		((CanvasRenderer) r).clear();
		return 1;
	}

	@Override public byte SDL_RenderPresent(SdlRenderer r) {
		((CanvasRenderer) r).present();
		return 1;
	}

	@Override public byte SDL_SetRenderDrawColor(SdlRenderer r, byte red, byte g, byte b, byte a) {
		((CanvasRenderer) r).setDrawColor(red & 0xFF, g & 0xFF, b & 0xFF, a & 0xFF);
		return 1;
	}

	@Override public byte SDL_SetRenderDrawBlendMode(SdlRenderer r, int blendMode) {
		((CanvasRenderer) r).setBlendMode(blendMode);
		return 1;
	}

	@Override public byte SDL_RenderFillRect(SdlRenderer r, SDLStructs.SDL_FRect rect) {
		((CanvasRenderer) r).fillRect(rect);
		return 1;
	}

	@Override public byte SDL_RenderRect(SdlRenderer r, SDLStructs.SDL_FRect rect) {
		((CanvasRenderer) r).drawRect(rect);
		return 1;
	}

	@Override public byte SDL_SetRenderLogicalPresentation(SdlRenderer r, int w, int h, int mode) {
		((CanvasRenderer) r).setLogicalSize(w, h);
		return 1;
	}

	@Override public byte SDL_RenderCoordinatesFromWindow(SdlRenderer r, float windowX, float windowY,
			FloatRef x, FloatRef y) {
		Letterbox box = ((CanvasRenderer) r).window.letterbox();
		x.value = box.toLogicalX(windowX);
		y.value = box.toLogicalY(windowY);
		return 1;
	}

	@Override public SdlTexture SDL_CreateTextureFromSurface(SdlRenderer r, SdlSurface surface) {
		if (surface == null) {
			lastError = "null surface";
			return null;
		}
		TeaVMSurface s = (TeaVMSurface) surface;
		return new TeaVMTexture(s.image, s.width, s.height);
	}

	@Override public void SDL_DestroyTexture(SdlTexture texture) {}

	@Override public byte SDL_SetTextureAlphaMod(SdlTexture texture, byte alpha) {
		((TeaVMTexture) texture).alphaMod = alpha & 0xFF;
		return 1;
	}

	@Override public byte SDL_SetTextureBlendMode(SdlTexture texture, int blendMode) { return 1; }
	@Override public byte SDL_SetTextureScaleMode(SdlTexture texture, int scaleMode) { return 1; }

	@Override public byte SDL_GetTextureSize(SdlTexture texture, FloatRef w, FloatRef h) {
		TeaVMTexture t = (TeaVMTexture) texture;
		w.value = t.width;
		h.value = t.height;
		return 1;
	}

	@Override public byte SDL_RenderTexture(SdlRenderer r, SdlTexture texture,
			SDLStructs.SDL_FRect srcrect, SDLStructs.SDL_FRect dstrect) {
		((CanvasRenderer) r).renderTexture((TeaVMTexture) texture, srcrect, dstrect);
		return 1;
	}

	@Override public byte SDL_RenderTextureFlipped(SdlRenderer r, SdlTexture texture,
			SDLStructs.SDL_FRect srcrect, SDLStructs.SDL_FRect dstrect, int flip) {
		// Only SDL_FLIP_HORIZONTAL is used (left-arrow glyph mirror).
		((CanvasRenderer) r).renderTextureFlippedH((TeaVMTexture) texture, srcrect, dstrect);
		return 1;
	}

	@Override public void SDL_DestroySurface(SdlSurface surface) {}

	@Override public byte SDL_SaveBMP(SdlSurface surface, String file) {
		// Screenshots are unsupported in the browser.
		return 0;
	}

	@Override public SdlSurface SDL_RenderReadPixels(SdlRenderer r, SDLStructs.SDL_Rect rect) {
		// Screenshots are unsupported in the browser (null is frontend-guarded).
		return null;
	}

	@Override public byte SDL_PollEvent(SDLStructs.SDL_Event event) {
		if (window == null) return 0;
		SDLStructs.SDL_Event queued = window.bridge.queue.poll();
		if (queued == null) return 0;

		event.type = queued.type;
		event.scancode = queued.scancode;
		event.keycode = queued.keycode;
		event.keymod = queued.keymod;
		event.keyDown = queued.keyDown;
		event.keyRepeat = queued.keyRepeat;
		event.text = queued.text;
		event.editingStart = queued.editingStart;
		event.editingLength = queued.editingLength;
		event.wheelX = queued.wheelX;
		event.wheelY = queued.wheelY;
		event.windowData1 = queued.windowData1;
		event.windowData2 = queued.windowData2;
		return 1;
	}

	@Override public byte SDL_StartTextInput(SdlWindow w) {
		((CanvasWindow) w).bridge.textInput = true;
		return 1;
	}

	@Override public byte SDL_StopTextInput(SdlWindow w) {
		((CanvasWindow) w).bridge.textInput = false;
		return 1;
	}

	@Override public byte SDL_SetTextInputArea(SdlWindow w, SDLStructs.SDL_Rect rect, int cursor) {
		return 1;
	}

	@Override public String SDL_GetClipboardText() {
		return clipboardFallback;
	}

	@Override public byte SDL_SetClipboardText(String text) {
		clipboardFallback = (text == null) ? "" : text;
		try {
			clipboardWrite(clipboardFallback);
		} catch (Exception | Error e) {
			// Best effort; keep the in-app fallback.
		}
		return 1;
	}

	@Override public int SDL_GetMouseState(FloatRef x, FloatRef y) {
		if (window == null) return 0;
		x.value = window.bridge.mouseX;
		y.value = window.bridge.mouseY;
		return window.bridge.mouseButtonMask();
	}

	/**
	 * Controller handle = pad slot in navigator.getGamepads(). The live Gamepad
	 * object is re-fetched on every call because Chrome returns immutable
	 * per-frame snapshots; a disconnected pad simply reads as all-zero until the
	 * gamepaddisconnected event makes the core re-enumerate.
	 */
	private static final class WebPad implements SdlGamepad {
		final int index;
		WebPad(int index) { this.index = index; }
	}

	/** SDL_GamepadButton ordinal → W3C standard-mapping button index. They are
	 *  not identical: SDL puts BACK/GUIDE/START at 4-6, W3C puts shoulders at 4-5. */
	private static final int[] W3C_BUTTON = {0, 1, 2, 3, 8, 16, 9, 10, 11, 4, 5, 12, 13, 14, 15};
	// ponytail: triggers unmapped on purpose — SDL exposes them only as axes,
	// so there is no consistent button ordinal for them on either backend.

	private static Gamepad pad(int index) {
		Gamepad[] pads = Navigator.getGamepads();
		if (pads == null || index < 0 || index >= pads.length) return null;
		return pads[index];
	}

	private static Gamepad pad(Object handle) {
		return handle instanceof WebPad ? pad(((WebPad) handle).index) : null;
	}

	@Override public int[] SDL_GetJoysticks() {
		// Returns ALL pad slots, including non-"standard"-mapping ones — the core
		// filters by SDL_IsGamepad, so non-standard web pads are simply invisible.
		Gamepad[] pads = Navigator.getGamepads();
		if (pads == null) return new int[0];
		// Instance ids are the array slots; getGamepads() keeps null holes.
		int count = 0;
		for (Gamepad p : pads) if (p != null) count++;
		int[] ids = new int[count];
		int n = 0;
		for (int i = 0; i < pads.length; i++) {
			if (pads[i] != null) ids[n++] = i;
		}
		return ids;
	}

	@Override public byte SDL_IsGamepad(int instanceId) {
		Gamepad p = pad(instanceId);
		return (byte) (p != null && "standard".equals(p.getMapping()) ? 1 : 0);
	}
	@Override public SdlGamepad SDL_OpenGamepad(int instanceId) {
		return pad(instanceId) != null ? new WebPad(instanceId) : null;
	}
	@Override public void SDL_CloseGamepad(SdlGamepad gamepad) {}
	@Override public byte SDL_GetGamepadButton(SdlGamepad gamepad, int button) {
		Gamepad p = pad(gamepad);
		if (p == null || button < 0 || button >= W3C_BUTTON.length) return 0;
		GamepadButton[] buttons = p.getButtons();
		int w3c = W3C_BUTTON[button];
		return (byte) (w3c < buttons.length && buttons[w3c].isPressed() ? 1 : 0);
	}
	@Override public short SDL_GetGamepadAxis(SdlGamepad gamepad, int axis) {
		// W3C axes 0/1 are the left stick, same order and sign as SDL LEFTX/LEFTY.
		Gamepad p = pad(gamepad);
		if (p == null) return 0;
		double[] axes = p.getAxes();
		return axis >= 0 && axis < axes.length ? (short) (axes[axis] * 32767) : 0;
	}
	@Override public String SDL_GetGamepadNameForID(int instanceId) {
		Gamepad p = pad(instanceId);
		return p == null ? null : p.getId();
	}

	@Override public byte SDL_ShowSimpleMessageBox(int flags, String title, String message, SdlWindow w) {
		Window.alert(title + "\n\n" + message);
		return 1;
	}

	@Override public long SDL_GetTicks() {
		return (long) (now() - startMillis);
	}

	void setError(String error) {
		lastError = error;
	}

	@JSBody(params = {}, script = "return performance.now();")
	private static native double now();

	@JSBody(params = {"text"}, script =
		"if (navigator.clipboard) { navigator.clipboard.writeText(text); }")
	private static native void clipboardWrite(String text);
}
