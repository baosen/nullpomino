package nullpomino.gui.sdl.binding.jna;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.FloatByReference;

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
 * Desktop implementation of the neutral {@link SDL3} interface on top of the
 * raw JNA binding. Marshals opaque handles to native pointers and decodes the
 * 128-byte SDL_Event union into the neutral event POJO.
 *
 * SDL_Event union layout for the fields we decode:
 * <pre>
 * Offset  Size  Common field
 *   0      4    type (Uint32 / SDL_EventType)
 *   4      4    reserved
 *   8      8    timestamp (Uint64)
 *
 * SDL_KeyboardEvent (type = SDL_EVENT_KEY_DOWN / SDL_EVENT_KEY_UP):
 *  24      4    scancode, 28 key, 32 mod (Uint16), 36 down (bool), 37 repeat (bool)
 *
 * SDL_Text{Input,Editing}Event: 24 text (char*), 32 start, 36 length
 * SDL_MouseWheelEvent: 24 x (float), 28 y (float)
 * SDL_WindowEvent: 20 data1, 24 data2
 * </pre>
 */
final class JnaSDL3 implements SDL3 {

	private final RawSDL3 raw;

	/** Reusable 128-byte buffer for SDL_PollEvent. */
	private final Memory eventBuf = new Memory(128);

	JnaSDL3(RawSDL3 raw) {
		this.raw = raw;
		eventBuf.clear();
	}

	@Override public byte SDL_Init(int flags) { return raw.SDL_Init(flags); }
	@Override public void SDL_Quit() { raw.SDL_Quit(); }
	@Override public String SDL_GetError() { return raw.SDL_GetError(); }

	@Override public SdlWindow SDL_CreateWindow(String title, int w, int h, long flags) {
		return Ptr.wrap(raw.SDL_CreateWindow(title, w, h, flags));
	}
	@Override public void SDL_DestroyWindow(SdlWindow window) { raw.SDL_DestroyWindow(Ptr.p(window)); }
	@Override public byte SDL_SetWindowTitle(SdlWindow window, String title) {
		return raw.SDL_SetWindowTitle(Ptr.p(window), title);
	}
	@Override public byte SDL_SetWindowFullscreen(SdlWindow window, int fullscreen) {
		return raw.SDL_SetWindowFullscreen(Ptr.p(window), fullscreen);
	}

	@Override public SdlRenderer SDL_CreateRenderer(SdlWindow window, String name) {
		return Ptr.wrap(raw.SDL_CreateRenderer(Ptr.p(window), name));
	}
	@Override public void SDL_DestroyRenderer(SdlRenderer renderer) { raw.SDL_DestroyRenderer(Ptr.p(renderer)); }
	@Override public byte SDL_RenderClear(SdlRenderer renderer) { return raw.SDL_RenderClear(Ptr.p(renderer)); }
	@Override public byte SDL_RenderPresent(SdlRenderer renderer) { return raw.SDL_RenderPresent(Ptr.p(renderer)); }
	@Override public byte SDL_SetRenderDrawColor(SdlRenderer renderer, byte r, byte g, byte b, byte a) {
		return raw.SDL_SetRenderDrawColor(Ptr.p(renderer), r, g, b, a);
	}
	@Override public byte SDL_SetRenderDrawBlendMode(SdlRenderer renderer, int blendMode) {
		return raw.SDL_SetRenderDrawBlendMode(Ptr.p(renderer), blendMode);
	}
	@Override public byte SDL_RenderFillRect(SdlRenderer renderer, SDLStructs.SDL_FRect rect) {
		return raw.SDL_RenderFillRect(Ptr.p(renderer), JnaStructs.frect(rect));
	}
	@Override public byte SDL_RenderRect(SdlRenderer renderer, SDLStructs.SDL_FRect rect) {
		return raw.SDL_RenderRect(Ptr.p(renderer), JnaStructs.frect(rect));
	}
	@Override public byte SDL_SetRenderLogicalPresentation(SdlRenderer renderer, int w, int h, int mode) {
		return raw.SDL_SetRenderLogicalPresentation(Ptr.p(renderer), w, h, mode);
	}
	@Override public byte SDL_RenderCoordinatesFromWindow(SdlRenderer renderer, float windowX, float windowY,
			FloatRef x, FloatRef y) {
		FloatByReference nx = new FloatByReference();
		FloatByReference ny = new FloatByReference();
		byte ok = raw.SDL_RenderCoordinatesFromWindow(Ptr.p(renderer), windowX, windowY, nx, ny);
		x.value = nx.getValue();
		y.value = ny.getValue();
		return ok;
	}

	@Override public SdlTexture SDL_CreateTextureFromSurface(SdlRenderer renderer, SdlSurface surface) {
		return Ptr.wrap(raw.SDL_CreateTextureFromSurface(Ptr.p(renderer), Ptr.p(surface)));
	}
	@Override public void SDL_DestroyTexture(SdlTexture texture) { raw.SDL_DestroyTexture(Ptr.p(texture)); }
	@Override public byte SDL_SetTextureAlphaMod(SdlTexture texture, byte alpha) {
		return raw.SDL_SetTextureAlphaMod(Ptr.p(texture), alpha);
	}
	@Override public byte SDL_SetTextureBlendMode(SdlTexture texture, int blendMode) {
		return raw.SDL_SetTextureBlendMode(Ptr.p(texture), blendMode);
	}
	@Override public byte SDL_SetTextureScaleMode(SdlTexture texture, int scaleMode) {
		return raw.SDL_SetTextureScaleMode(Ptr.p(texture), scaleMode);
	}
	@Override public byte SDL_GetTextureSize(SdlTexture texture, FloatRef w, FloatRef h) {
		FloatByReference nw = new FloatByReference();
		FloatByReference nh = new FloatByReference();
		byte ok = raw.SDL_GetTextureSize(Ptr.p(texture), nw, nh);
		w.value = nw.getValue();
		h.value = nh.getValue();
		return ok;
	}
	@Override public byte SDL_RenderTexture(SdlRenderer renderer, SdlTexture texture,
			SDLStructs.SDL_FRect srcrect, SDLStructs.SDL_FRect dstrect) {
		return raw.SDL_RenderTexture(Ptr.p(renderer), Ptr.p(texture),
			JnaStructs.frect(srcrect), JnaStructs.frect(dstrect));
	}

	@Override public void SDL_DestroySurface(SdlSurface surface) { raw.SDL_DestroySurface(Ptr.p(surface)); }
	@Override public byte SDL_SaveBMP(SdlSurface surface, String file) {
		return raw.SDL_SaveBMP(Ptr.p(surface), file);
	}
	@Override public SdlSurface SDL_RenderReadPixels(SdlRenderer renderer, SDLStructs.SDL_Rect rect) {
		return Ptr.wrap(raw.SDL_RenderReadPixels(Ptr.p(renderer), JnaStructs.rect(rect)));
	}

	@Override public byte SDL_PollEvent(SDLStructs.SDL_Event event) {
		byte got = raw.SDL_PollEvent(eventBuf);
		if(got != 0) decodeEvent(event);
		return got;
	}

	private void decodeEvent(SDLStructs.SDL_Event out) {
		int type = eventBuf.getInt(0);
		out.type = type;
		out.text = "";

		if(type == SDLConstants.SDL_EVENT_KEY_DOWN || type == SDLConstants.SDL_EVENT_KEY_UP) {
			out.scancode = eventBuf.getInt(24);
			out.keycode = eventBuf.getInt(28);
			out.keymod = eventBuf.getShort(32) & 0xFFFF;
			out.keyDown = eventBuf.getByte(36) != 0;
			out.keyRepeat = eventBuf.getByte(37) != 0;
		} else if(type == SDLConstants.SDL_EVENT_TEXT_INPUT || type == SDLConstants.SDL_EVENT_TEXT_EDITING) {
			// SDL owns the text buffer — copy it, do not free.
			Pointer p = eventBuf.getPointer(24);
			out.text = (p == null) ? "" : p.getString(0, "UTF-8");
			out.editingStart = eventBuf.getInt(32);
			out.editingLength = eventBuf.getInt(36);
		} else if(type == SDLConstants.SDL_EVENT_MOUSE_WHEEL) {
			out.wheelX = eventBuf.getFloat(24);
			out.wheelY = eventBuf.getFloat(28);
		} else {
			// Window events (and anything else carrying data1/data2)
			out.windowData1 = eventBuf.getInt(20);
			out.windowData2 = eventBuf.getInt(24);
		}
	}

	@Override public byte SDL_StartTextInput(SdlWindow window) { return raw.SDL_StartTextInput(Ptr.p(window)); }
	@Override public byte SDL_StopTextInput(SdlWindow window) { return raw.SDL_StopTextInput(Ptr.p(window)); }
	@Override public byte SDL_SetTextInputArea(SdlWindow window, SDLStructs.SDL_Rect rect, int cursor) {
		return raw.SDL_SetTextInputArea(Ptr.p(window), JnaStructs.rect(rect), cursor);
	}

	@Override public String SDL_GetClipboardText() {
		// SDL returns a malloc'd buffer that we must SDL_free.
		Pointer p = raw.SDL_GetClipboardText();
		if(p == null) return null;
		try {
			return p.getString(0, "UTF-8");
		} finally {
			raw.SDL_free(p);
		}
	}
	@Override public byte SDL_SetClipboardText(String text) { return raw.SDL_SetClipboardText(text); }

	@Override public int SDL_GetMouseState(FloatRef x, FloatRef y) {
		FloatByReference nx = new FloatByReference();
		FloatByReference ny = new FloatByReference();
		int buttons = raw.SDL_GetMouseState(nx, ny);
		x.value = nx.getValue();
		y.value = ny.getValue();
		return buttons;
	}

	@Override public int[] SDL_GetJoysticks() {
		// SDL returns a malloc'd 0-terminated id array that we must SDL_free.
		int[] countBuf = new int[1];
		Pointer list = raw.SDL_GetJoysticks(countBuf);
		if(list == null) return new int[0];
		try {
			int count = countBuf[0];
			int[] ids = new int[Math.max(count, 0)];
			for(int i = 0; i < ids.length; i++) {
				ids[i] = list.getInt(i * 4L);
			}
			return ids;
		} finally {
			raw.SDL_free(list);
		}
	}
	@Override public SdlJoystick SDL_OpenJoystick(int instanceId) {
		return Ptr.wrap(raw.SDL_OpenJoystick(instanceId));
	}
	@Override public void SDL_CloseJoystick(SdlJoystick joystick) { raw.SDL_CloseJoystick(Ptr.p(joystick)); }
	@Override public short SDL_GetJoystickAxis(SdlJoystick joystick, int axis) {
		return raw.SDL_GetJoystickAxis(Ptr.p(joystick), axis);
	}
	@Override public byte SDL_GetJoystickButton(SdlJoystick joystick, int button) {
		return raw.SDL_GetJoystickButton(Ptr.p(joystick), button);
	}
	@Override public byte SDL_GetJoystickHat(SdlJoystick joystick, int hat) {
		return raw.SDL_GetJoystickHat(Ptr.p(joystick), hat);
	}
	@Override public int SDL_GetNumJoystickButtons(SdlJoystick joystick) {
		return raw.SDL_GetNumJoystickButtons(Ptr.p(joystick));
	}
	@Override public int SDL_GetNumJoystickHats(SdlJoystick joystick) {
		return raw.SDL_GetNumJoystickHats(Ptr.p(joystick));
	}

	@Override public byte SDL_ShowSimpleMessageBox(int flags, String title, String message, SdlWindow window) {
		return raw.SDL_ShowSimpleMessageBox(flags, title, message, Ptr.p(window));
	}

	@Override public long SDL_GetTicks() { return raw.SDL_GetTicks(); }
}
