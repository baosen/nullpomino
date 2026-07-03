package nullpomino.gui.sdl.binding.jna;

import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/**
 * Desktop implementation of the neutral {@link SDL3Image} interface.
 */
final class JnaSDL3Image implements SDL3Image {

	private final RawSDL3Image raw;

	JnaSDL3Image(RawSDL3Image raw) {
		this.raw = raw;
	}

	@Override public SdlSurface IMG_Load(String file) {
		return Ptr.wrap(raw.IMG_Load(file));
	}
}
