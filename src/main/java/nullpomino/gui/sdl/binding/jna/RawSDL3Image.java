package nullpomino.gui.sdl.binding.jna;

import com.sun.jna.Library;
import com.sun.jna.Pointer;

/**
 * JNA interface to SDL3_image (libSDL3_image.so / SDL3_image.dll).
 */
interface RawSDL3Image extends Library {

	/** Load an image file as an SDL_Surface*. Returns null on failure. */
	Pointer IMG_Load(String file);
}
