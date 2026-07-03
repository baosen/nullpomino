package nullpomino.gui.sdl.binding.jna;

import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

import nullpomino.gui.sdl.binding.SDLStructs;

/**
 * JNA structure definitions for SDL3, plus converters from the
 * backend-neutral POJO structs.
 */
final class JnaStructs {
	private JnaStructs() {}

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
	}

	/** SDL_AudioSpec — audio format specification. */
	public static class SDL_AudioSpec extends Structure {
		public int format;
		public int channels;
		public int freq;

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

		@Override
		protected List<String> getFieldOrder() {
			return Arrays.asList("r", "g", "b", "a");
		}

		public static class ByValue extends SDL_Color implements Structure.ByValue {
			public ByValue() {}
			public ByValue(int r, int g, int b, int a) { super(r, g, b, a); }
		}
	}

	static SDL_FRect frect(SDLStructs.SDL_FRect r) {
		return r == null ? null : new SDL_FRect(r.x, r.y, r.w, r.h);
	}

	static SDL_Rect rect(SDLStructs.SDL_Rect r) {
		return r == null ? null : new SDL_Rect(r.x, r.y, r.w, r.h);
	}

	static SDL_Color.ByValue color(SDLStructs.SDL_Color c) {
		return new SDL_Color.ByValue(c.r & 0xFF, c.g & 0xFF, c.b & 0xFF, c.a & 0xFF);
	}
}
