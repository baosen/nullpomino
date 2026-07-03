package nullpomino.gui.sdl.binding.jna;

import com.sun.jna.ptr.IntByReference;

import nullpomino.gui.sdl.binding.Ref.IntRef;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.SdlFont;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/**
 * Desktop implementation of the neutral {@link SDL3TTF} interface.
 */
final class JnaSDL3TTF implements SDL3TTF {

	private final RawSDL3TTF raw;

	JnaSDL3TTF(RawSDL3TTF raw) {
		this.raw = raw;
	}

	@Override public boolean TTF_Init() { return raw.TTF_Init(); }
	@Override public void TTF_Quit() { raw.TTF_Quit(); }

	@Override public SdlFont TTF_OpenFont(String file, float ptsize) {
		return Ptr.wrap(raw.TTF_OpenFont(file, ptsize));
	}

	@Override public void TTF_CloseFont(SdlFont font) { raw.TTF_CloseFont(Ptr.p(font)); }

	@Override public SdlSurface TTF_RenderText_Blended(SdlFont font, String text, int length,
			SDLStructs.SDL_Color.ByValue fg) {
		return Ptr.wrap(raw.TTF_RenderText_Blended(Ptr.p(font), text, length, JnaStructs.color(fg)));
	}

	@Override public boolean TTF_GetStringSize(SdlFont font, String text, int length, IntRef w, IntRef h) {
		IntByReference nw = new IntByReference();
		IntByReference nh = new IntByReference();
		boolean ok = raw.TTF_GetStringSize(Ptr.p(font), text, length, nw, nh);
		w.value = nw.getValue();
		h.value = nh.getValue();
		return ok;
	}
}
