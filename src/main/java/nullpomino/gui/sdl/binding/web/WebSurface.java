package nullpomino.gui.sdl.binding.web;

import java.awt.image.BufferedImage;

import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/** A surface is a plain in-memory image (pre-texture pixel data). */
final class WebSurface implements SdlSurface {
	final BufferedImage img;

	WebSurface(BufferedImage img) {
		this.img = img;
	}
}
