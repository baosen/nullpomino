package nullpomino.gui.sdl.binding.web;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/**
 * Pure-Java implementation of {@link SDL3Image} via ImageIO.
 */
final class WebSDL3Image implements SDL3Image {

	private final WebSDL3 sdl;

	WebSDL3Image(WebSDL3 sdl) {
		this.sdl = sdl;
	}

	@Override public SdlSurface IMG_Load(String file) {
		try {
			BufferedImage img = ImageIO.read(new File(file));
			if(img == null) {
				sdl.setError("unsupported image format: " + file);
				return null;
			}
			return new WebSurface(img);
		} catch(Exception e) {
			sdl.setError(String.valueOf(e));
			return null;
		}
	}
}
