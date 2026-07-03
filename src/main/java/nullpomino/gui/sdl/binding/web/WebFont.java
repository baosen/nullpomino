package nullpomino.gui.sdl.binding.web;

import java.awt.Font;

import nullpomino.gui.sdl.binding.SdlHandles.SdlFont;

/** A TTF font handle backed by an AWT font. */
final class WebFont implements SdlFont {
	final Font font;

	WebFont(Font font) {
		this.font = font;
	}
}
