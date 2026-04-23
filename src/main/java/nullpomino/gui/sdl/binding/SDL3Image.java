package nullpomino.gui.sdl.binding;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * JNA interface to SDL3_image (libSDL3_image.so / SDL3_image.dll).
 */
public interface SDL3Image extends Library {
	SDL3Image INSTANCE = Native.load("SDL3_image", SDL3Image.class);

	/** Load an image file as an SDL_Surface*. Returns null on failure. */
	Pointer IMG_Load(String file);
}
