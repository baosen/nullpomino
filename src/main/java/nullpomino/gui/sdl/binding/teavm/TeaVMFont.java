package nullpomino.gui.sdl.binding.teavm;

import nullpomino.gui.sdl.binding.SdlHandles.SdlFont;

/** A TTF font handle: a registered CSS font-family name plus its point size. */
final class TeaVMFont implements SdlFont {
	final String cssFont;
	final int pointSize;

	TeaVMFont(String family, int pointSize) {
		this.pointSize = pointSize;
		this.cssFont = pointSize + "px \"" + family + "\"";
	}
}
