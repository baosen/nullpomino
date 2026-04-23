package nullpomino.gui.sdl.binding;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * JNA structure definitions for SDL3.
 */
public final class SDLStructs {
	private SDLStructs() {}

	/** SDL_FRect — floating-point rectangle used by the SDL3 renderer. */
	public static class SDL_FRect extends Structure {
		public float x, y, w, h;

		public SDL_FRect() {}

		public SDL_FRect(float x, float y, float w, float h) {
			this.x = x; this.y = y; this.w = w; this.h = h;
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("x", "y", "w", "h");
		}

		public static class ByReference extends SDL_FRect implements Structure.ByReference {}
		public static class ByValue extends SDL_FRect implements Structure.ByValue {
			public ByValue() {}
			public ByValue(float x, float y, float w, float h) { super(x, y, w, h); }
		}
	}

	/** SDL_Rect — integer rectangle. */
	public static class SDL_Rect extends Structure {
		public int x, y, w, h;

		public SDL_Rect() {}

		public SDL_Rect(int x, int y, int w, int h) {
			this.x = x; this.y = y; this.w = w; this.h = h;
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("x", "y", "w", "h");
		}

		public static class ByReference extends SDL_Rect implements Structure.ByReference {}
	}

	/** SDL_AudioSpec — audio format specification. */
	public static class SDL_AudioSpec extends Structure {
		/** SDL_AudioFormat (e.g. 0x8010 for SDL_AUDIO_S16) */
		public int format;
		/** Number of channels: 1 mono, 2 stereo */
		public int channels;
		/** Sample rate in frames per second (e.g. 44100) */
		public int freq;

		public SDL_AudioSpec() {}

		public SDL_AudioSpec(int format, int channels, int freq) {
			this.format = format;
			this.channels = channels;
			this.freq = freq;
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("format", "channels", "freq");
		}
	}

	/** SDL_Color — RGBA color. */
	public static class SDL_Color extends Structure {
		public byte r, g, b, a;

		public SDL_Color() {}

		public SDL_Color(int r, int g, int b, int a) {
			this.r = (byte)r; this.g = (byte)g; this.b = (byte)b; this.a = (byte)a;
		}

		public SDL_Color(int r, int g, int b) {
			this(r, g, b, 255);
		}

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("r", "g", "b", "a");
		}

		public static class ByValue extends SDL_Color implements Structure.ByValue {
			public ByValue() {}
			public ByValue(int r, int g, int b, int a) { super(r, g, b, a); }
			public ByValue(int r, int g, int b) { super(r, g, b); }
		}
	}

	/**
	 * SDL_Event — 128-byte union, accessed by reading fields at known offsets.
	 *
	 * Layout for the fields we care about:
	 * <pre>
	 * Offset  Size  Common field
	 *   0      4    type (Uint32 / SDL_EventType)
	 *   4      4    reserved
	 *   8      8    timestamp (Uint64)
	 *
	 * SDL_KeyboardEvent (type = SDL_EVENT_KEY_DOWN / SDL_EVENT_KEY_UP):
	 *  16      4    windowID
	 *  20      4    which (keyboard ID)
	 *  24      4    scancode (SDL_Scancode)
	 *  28      4    key (SDL_Keycode)
	 *  32      2    mod (SDL_Keymod)
	 *  34      2    raw
	 *  36      1    down (bool)
	 *  37      1    repeat (bool)
	 *
	 * SDL_TextInputEvent (type = SDL_EVENT_TEXT_INPUT):
	 *  16      4    windowID
	 *  24      8    text (const char *, UTF-8; pointer is owned by SDL)
	 *
	 * SDL_TextEditingEvent (type = SDL_EVENT_TEXT_EDITING):
	 *  16      4    windowID
	 *  24      8    text (const char *, UTF-8; IME preedit)
	 *  32      4    start  (cursor position within preedit text)
	 *  36      4    length (length of selected preedit range, 0 if none)
	 *
	 * SDL_MouseWheelEvent (type = SDL_EVENT_MOUSE_WHEEL):
	 *  16      4    windowID
	 *  20      4    which (mouse ID)
	 *  24      4    x (float, horizontal wheel delta)
	 *  28      4    y (float, vertical wheel delta; positive = up)
	 *  32      4    direction (SDL_MouseWheelDirection)
	 *  36      4    mouse_x (float)
	 *  40      4    mouse_y (float)
	 *
	 * SDL_WindowEvent (type = SDL_EVENT_WINDOW_*):
	 *  16      4    windowID
	 *  20      4    data1
	 *  24      4    data2
	 * </pre>
	 */
	public static class SDL_Event {
		private final Memory mem;

		public SDL_Event() {
			mem = new Memory(128);
			mem.clear();
		}

		public Pointer getPointer() { return mem; }

		public int getType() { return mem.getInt(0); }

		// Keyboard event fields
		public int getScancode() { return mem.getInt(24); }
		public int getKeycode() { return mem.getInt(28); }
		public int getKeymod() { return mem.getShort(32) & 0xFFFF; }
		public boolean isKeyDown() { return mem.getByte(36) != 0; }
		public boolean isKeyRepeat() { return mem.getByte(37) != 0; }

		// Text input / editing event fields
		/**
		 * Read the UTF-8 text pointed to by SDL_Text{Input,Editing}Event.text.
		 * Returns empty string if null. SDL owns the buffer — do not free.
		 */
		public String getTextInputText() {
			Pointer p = mem.getPointer(24);
			if(p == null) return "";
			return p.getString(0, "UTF-8");
		}
		public int getTextEditingStart()  { return mem.getInt(32); }
		public int getTextEditingLength() { return mem.getInt(36); }

		// Mouse wheel event fields
		public float getMouseWheelX() { return mem.getFloat(24); }
		public float getMouseWheelY() { return mem.getFloat(28); }

		// Window event fields
		public int getWindowData1() { return mem.getInt(20); }
		public int getWindowData2() { return mem.getInt(24); }
	}
}
