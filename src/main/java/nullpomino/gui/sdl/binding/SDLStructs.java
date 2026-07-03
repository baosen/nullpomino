package nullpomino.gui.sdl.binding;

/**
 * Backend-neutral SDL struct equivalents.
 *
 * Plain data classes shared by all binding backends. The desktop backend
 * converts these to/from real JNA structures at the call boundary; the web
 * backend consumes them directly.
 */
public final class SDLStructs {
	private SDLStructs() {}

	/** SDL_FRect — floating-point rectangle used by the SDL3 renderer. */
	public static class SDL_FRect {
		public float x, y, w, h;

		public SDL_FRect() {}

		public SDL_FRect(float x, float y, float w, float h) {
			this.x = x; this.y = y; this.w = w; this.h = h;
		}
	}

	/** SDL_Rect — integer rectangle. */
	public static class SDL_Rect {
		public int x, y, w, h;

		public SDL_Rect() {}

		public SDL_Rect(int x, int y, int w, int h) {
			this.x = x; this.y = y; this.w = w; this.h = h;
		}
	}

	/** SDL_AudioSpec — audio format specification. */
	public static class SDL_AudioSpec {
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
	}

	/** SDL_Color — RGBA color. */
	public static class SDL_Color {
		public byte r, g, b, a;

		public SDL_Color() {}

		public SDL_Color(int r, int g, int b, int a) {
			this.r = (byte)r; this.g = (byte)g; this.b = (byte)b; this.a = (byte)a;
		}

		public SDL_Color(int r, int g, int b) {
			this(r, g, b, 255);
		}

		/** Kept as a distinct type so pass-by-value call sites read the same as before. */
		public static class ByValue extends SDL_Color {
			public ByValue() {}
			public ByValue(int r, int g, int b, int a) { super(r, g, b, a); }
			public ByValue(int r, int g, int b) { super(r, g, b); }
		}
	}

	/**
	 * SDL_Event — backend-neutral event record filled by
	 * {@link SDL3#SDL_PollEvent}. Field meaning follows the SDL_Event union:
	 * only the fields relevant to {@link #getType} are populated.
	 */
	public static class SDL_Event {
		public int type;

		// Keyboard event fields
		public int scancode;
		public int keycode;
		public int keymod;
		public boolean keyDown;
		public boolean keyRepeat;

		// Text input / editing event fields
		public String text = "";
		public int editingStart;
		public int editingLength;

		// Mouse wheel event fields
		public float wheelX;
		public float wheelY;

		// Window event fields
		public int windowData1;
		public int windowData2;

		public int getType() { return type; }

		public int getScancode() { return scancode; }
		public int getKeycode() { return keycode; }
		public int getKeymod() { return keymod; }
		public boolean isKeyDown() { return keyDown; }
		public boolean isKeyRepeat() { return keyRepeat; }

		/** UTF-8 text of an SDL_Text{Input,Editing}Event; empty string if none. */
		public String getTextInputText() { return text == null ? "" : text; }
		public int getTextEditingStart()  { return editingStart; }
		public int getTextEditingLength() { return editingLength; }

		public float getMouseWheelX() { return wheelX; }
		public float getMouseWheelY() { return wheelY; }

		public int getWindowData1() { return windowData1; }
		public int getWindowData2() { return windowData2; }
	}
}
