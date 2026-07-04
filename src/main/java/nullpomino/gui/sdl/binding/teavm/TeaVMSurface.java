package nullpomino.gui.sdl.binding.teavm;

import org.teavm.jso.canvas.CanvasImageSource;

import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/**
 * A surface is decoded pixel data before it becomes a texture: an
 * {@code HTMLImageElement} (from IMG_Load) or an offscreen canvas (from TTF
 * rendering). Both are {@link CanvasImageSource}, so a texture can reuse them
 * directly.
 */
final class TeaVMSurface implements SdlSurface {
	final CanvasImageSource image;
	final int width;
	final int height;

	TeaVMSurface(CanvasImageSource image, int width, int height) {
		this.image = image;
		this.width = width;
		this.height = height;
	}
}
