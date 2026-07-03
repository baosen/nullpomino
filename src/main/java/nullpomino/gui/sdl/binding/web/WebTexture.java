package nullpomino.gui.sdl.binding.web;

import java.awt.image.BufferedImage;

import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;

/** A texture is just an ARGB image plus its current alpha modulation. */
final class WebTexture implements SdlTexture {
	final BufferedImage img;
	int alphaMod = 255;

	WebTexture(BufferedImage img) {
		this.img = img;
	}
}
