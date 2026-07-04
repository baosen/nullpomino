package nullpomino.gui.sdl.binding.teavm;

import org.teavm.jso.canvas.CanvasImageSource;

import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;

/** A drawable image source plus its current alpha modulation (0-255). */
final class TeaVMTexture implements SdlTexture {
	final CanvasImageSource image;
	final int width;
	final int height;
	int alphaMod = 255;

	TeaVMTexture(CanvasImageSource image, int width, int height) {
		this.image = image;
		this.width = width;
		this.height = height;
	}
}
