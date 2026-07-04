package nullpomino.gui.sdl.binding.teavm;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SdlBackend;

/**
 * The browser (TeaVM) backend: Canvas2D rendering, image/font fetching, and
 * Web Audio sound. No AWT or native code. Inject with
 * {@code SdlBackend.set(new TeaVMBackend())} before any binding interface is
 * initialized.
 */
public final class TeaVMBackend implements SdlBackend.Backend {

	private final TeaVMSDL3 sdl3 = new TeaVMSDL3();
	private final TeaVMSDL3Image image = new TeaVMSDL3Image(sdl3);
	private final TeaVMSDL3TTF ttf = new TeaVMSDL3TTF(sdl3);

	private SDL3Mixer mixer;
	private boolean mixerProbed;

	@Override public SDL3 sdl3() { return sdl3; }
	@Override public SDL3Image image() { return image; }
	@Override public SDL3TTF ttf() { return ttf; }

	@Override public SDL3Mixer mixerOrNull() {
		if (!mixerProbed) {
			mixerProbed = true;
			mixer = TeaVMSDL3Mixer.createOrNull();
		}
		return mixer;
	}
}
