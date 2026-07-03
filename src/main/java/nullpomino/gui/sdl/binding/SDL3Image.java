package nullpomino.gui.sdl.binding;

import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/**
 * Backend-neutral interface to SDL3_image.
 */
public interface SDL3Image {
	SDL3Image INSTANCE = SdlBackend.get().image();

	/** Load an image file as a surface. Returns null on failure. */
	SdlSurface IMG_Load(String file);
}
