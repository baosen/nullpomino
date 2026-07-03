package nullpomino.gui.sdl.binding.web;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.SdlJoystick;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.gui.sdl.binding.SdlHandles.SdlWindow;

/**
 * Pure-Java implementation of the neutral {@link SDL3} interface on top of
 * AWT/Swing/Java2D. Runs anywhere a headful JDK runs — including CheerpJ in
 * a browser, where native SDL is impossible.
 */
final class WebSDL3 implements SDL3 {

	private final long startNanos = System.nanoTime();

	private volatile String lastError = "";

	/** In-app clipboard fallback when the system clipboard is unavailable. */
	private volatile String clipboardFallback = "";

	private WebWindow window;

	@Override public byte SDL_Init(int flags) { return 1; }
	@Override public void SDL_Quit() {}
	@Override public String SDL_GetError() { return lastError; }

	@Override public SdlWindow SDL_CreateWindow(String title, int w, int h, long flags) {
		try {
			window = new WebWindow(title, w, h);
			return window;
		} catch(RuntimeException e) {
			lastError = String.valueOf(e);
			return null;
		}
	}

	@Override public void SDL_DestroyWindow(SdlWindow w) {
		if(w instanceof WebWindow) ((WebWindow) w).dispose();
	}

	@Override public byte SDL_SetWindowTitle(SdlWindow w, String title) {
		((WebWindow) w).setTitle(title);
		return 1;
	}

	@Override public byte SDL_SetWindowFullscreen(SdlWindow w, int fullscreen) {
		// No real fullscreen on the web canvas; the letterbox already scales.
		return 1;
	}

	@Override public SdlRenderer SDL_CreateRenderer(SdlWindow w, String name) {
		WebWindow ww = (WebWindow) w;
		return new WebRenderer(ww, ww.panel.getWidth(), ww.panel.getHeight());
	}

	@Override public void SDL_DestroyRenderer(SdlRenderer r) {
		((WebRenderer) r).dispose();
	}

	@Override public byte SDL_RenderClear(SdlRenderer r) {
		((WebRenderer) r).clear();
		return 1;
	}

	@Override public byte SDL_RenderPresent(SdlRenderer r) {
		((WebRenderer) r).present();
		return 1;
	}

	@Override public byte SDL_SetRenderDrawColor(SdlRenderer r, byte red, byte g, byte b, byte a) {
		((WebRenderer) r).setDrawColor(red & 0xFF, g & 0xFF, b & 0xFF, a & 0xFF);
		return 1;
	}

	@Override public byte SDL_SetRenderDrawBlendMode(SdlRenderer r, int blendMode) {
		((WebRenderer) r).setBlendMode(blendMode);
		return 1;
	}

	@Override public byte SDL_RenderFillRect(SdlRenderer r, SDLStructs.SDL_FRect rect) {
		((WebRenderer) r).fillRect(rect);
		return 1;
	}

	@Override public byte SDL_RenderRect(SdlRenderer r, SDLStructs.SDL_FRect rect) {
		((WebRenderer) r).drawRect(rect);
		return 1;
	}

	@Override public byte SDL_SetRenderLogicalPresentation(SdlRenderer r, int w, int h, int mode) {
		((WebRenderer) r).setLogicalSize(w, h);
		return 1;
	}

	@Override public byte SDL_RenderCoordinatesFromWindow(SdlRenderer r, float windowX, float windowY,
			FloatRef x, FloatRef y) {
		Letterbox box = ((WebRenderer) r).window.letterbox();
		x.value = box.toLogicalX(windowX);
		y.value = box.toLogicalY(windowY);
		return 1;
	}

	@Override public SdlTexture SDL_CreateTextureFromSurface(SdlRenderer r, SdlSurface surface) {
		if(surface == null) {
			lastError = "null surface";
			return null;
		}
		BufferedImage src = ((WebSurface) surface).img;
		BufferedImage argb;
		if(src.getType() == BufferedImage.TYPE_INT_ARGB) {
			argb = src;
		} else {
			argb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics2D g = argb.createGraphics();
			g.drawImage(src, 0, 0, null);
			g.dispose();
		}
		return new WebTexture(argb);
	}

	@Override public void SDL_DestroyTexture(SdlTexture texture) {}

	@Override public byte SDL_SetTextureAlphaMod(SdlTexture texture, byte alpha) {
		((WebTexture) texture).alphaMod = alpha & 0xFF;
		return 1;
	}

	@Override public byte SDL_SetTextureBlendMode(SdlTexture texture, int blendMode) {
		// Textures are always alpha-blended (the only mode this game sets).
		return 1;
	}

	@Override public byte SDL_SetTextureScaleMode(SdlTexture texture, int scaleMode) {
		// NEAREST is the global rendering hint; nothing to do per texture.
		return 1;
	}

	@Override public byte SDL_GetTextureSize(SdlTexture texture, FloatRef w, FloatRef h) {
		BufferedImage img = ((WebTexture) texture).img;
		w.value = img.getWidth();
		h.value = img.getHeight();
		return 1;
	}

	@Override public byte SDL_RenderTexture(SdlRenderer r, SdlTexture texture,
			SDLStructs.SDL_FRect srcrect, SDLStructs.SDL_FRect dstrect) {
		((WebRenderer) r).renderTexture((WebTexture) texture, srcrect, dstrect);
		return 1;
	}

	@Override public void SDL_DestroySurface(SdlSurface surface) {}

	@Override public byte SDL_SaveBMP(SdlSurface surface, String file) {
		try {
			return ImageIO.write(((WebSurface) surface).img, "bmp", new File(file)) ? (byte) 1 : 0;
		} catch(Exception e) {
			lastError = String.valueOf(e);
			return 0;
		}
	}

	@Override public SdlSurface SDL_RenderReadPixels(SdlRenderer r, SDLStructs.SDL_Rect rect) {
		return new WebSurface(((WebRenderer) r).snapshot());
	}

	@Override public byte SDL_PollEvent(SDLStructs.SDL_Event event) {
		if(window == null) return 0;
		SDLStructs.SDL_Event queued = window.bridge.queue.poll();
		if(queued == null) return 0;

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
		((WebWindow) w).bridge.textInput = true;
		return 1;
	}

	@Override public byte SDL_StopTextInput(SdlWindow w) {
		((WebWindow) w).bridge.textInput = false;
		return 1;
	}

	@Override public byte SDL_SetTextInputArea(SdlWindow w, SDLStructs.SDL_Rect rect, int cursor) {
		// No IME composition window to place.
		return 1;
	}

	@Override public String SDL_GetClipboardText() {
		try {
			Object data = Toolkit.getDefaultToolkit().getSystemClipboard()
				.getData(DataFlavor.stringFlavor);
			if(data instanceof String) return (String) data;
		} catch(Exception | Error e) {
			// System clipboard unavailable (browser permissions, headless).
		}
		return clipboardFallback;
	}

	@Override public byte SDL_SetClipboardText(String text) {
		String value = (text == null) ? "" : text;
		clipboardFallback = value;
		try {
			Toolkit.getDefaultToolkit().getSystemClipboard()
				.setContents(new StringSelection(value), null);
		} catch(Exception | Error e) {
			// Keep the in-app fallback only.
		}
		return 1;
	}

	@Override public int SDL_GetMouseState(FloatRef x, FloatRef y) {
		if(window == null) return 0;
		x.value = window.bridge.mouseX;
		y.value = window.bridge.mouseY;
		return window.bridge.mouseButtonMask();
	}

	@Override public int[] SDL_GetJoysticks() {
		// No gamepad support in the browser backend.
		return new int[0];
	}

	@Override public SdlJoystick SDL_OpenJoystick(int instanceId) { return null; }
	@Override public void SDL_CloseJoystick(SdlJoystick joystick) {}
	@Override public short SDL_GetJoystickAxis(SdlJoystick joystick, int axis) { return 0; }
	@Override public byte SDL_GetJoystickButton(SdlJoystick joystick, int button) { return 0; }
	@Override public byte SDL_GetJoystickHat(SdlJoystick joystick, int hat) { return 0; }
	@Override public int SDL_GetNumJoystickButtons(SdlJoystick joystick) { return 0; }
	@Override public int SDL_GetNumJoystickHats(SdlJoystick joystick) { return 0; }

	@Override public byte SDL_ShowSimpleMessageBox(int flags, String title, String message, SdlWindow w) {
		int type = JOptionPane.INFORMATION_MESSAGE;
		if((flags & SDLConstants.SDL_MESSAGEBOX_ERROR) != 0) type = JOptionPane.ERROR_MESSAGE;
		else if((flags & SDLConstants.SDL_MESSAGEBOX_WARNING) != 0) type = JOptionPane.WARNING_MESSAGE;
		final int messageType = type;
		SwingUtilities.invokeLater(() ->
			JOptionPane.showMessageDialog(window != null ? window.frame : null, message, title, messageType));
		return 1;
	}

	@Override public long SDL_GetTicks() {
		return (System.nanoTime() - startNanos) / 1_000_000L;
	}

	void setError(String error) {
		lastError = error;
	}
}
