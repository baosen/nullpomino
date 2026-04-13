package mu.nu.nullpo.gui.sdl.binding;

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
		public boolean isKeyDown() { return mem.getByte(36) != 0; }
		public boolean isKeyRepeat() { return mem.getByte(37) != 0; }

		// Window event fields
		public int getWindowData1() { return mem.getInt(20); }
		public int getWindowData2() { return mem.getInt(24); }

		// Mouse event fields (SDL_MouseMotionEvent & SDL_MouseButtonEvent)
		// Layout: ...windowID(16) which(20) state/button(24) x(28) y(32)
		public float getMouseX() { return mem.getFloat(28); }
		public float getMouseY() { return mem.getFloat(32); }

		// Mouse button event: button(24, Uint8), down(25, bool)
		public int getMouseButton() { return mem.getByte(24) & 0xFF; }
		public boolean isMousePressed() { return mem.getByte(25) != 0; }

		// Text input event fields (SDL_TextInputEvent)
		// SDL3 64-bit: text is const char* at offset 24 (after 4 bytes padding for pointer alignment)
		public String getTextInput() {
			Pointer p = mem.getPointer(24);
			return (p != null) ? p.getString(0) : null;
		}
	}
}
