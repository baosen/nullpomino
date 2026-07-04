package nullpomino.gui.sdl.binding.teavm;

import org.teavm.jso.dom.html.HTMLImageElement;

import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/**
 * TeaVM implementation of {@link SDL3Image}: fetch the image over HTTP and
 * block (on the game's green thread) until it decodes. Asset paths are
 * page-relative URLs ({@code res/...}); a leading slash is dropped so
 * subdirectory hosting works.
 */
final class TeaVMSDL3Image implements SDL3Image {

	private final TeaVMSDL3 sdl;

	TeaVMSDL3Image(TeaVMSDL3 sdl) {
		this.sdl = sdl;
	}

	@Override public SdlSurface IMG_Load(String file) {
		if (file == null || file.isEmpty()) {
			return null;
		}
		HTMLImageElement img = JsAsync.fetchImage(Assets.toUrl(file));
		if (img == null) {
			sdl.setError("failed to load image: " + file);
			return null;
		}
		return new TeaVMSurface(img, img.getWidth(), img.getHeight());
	}
}
