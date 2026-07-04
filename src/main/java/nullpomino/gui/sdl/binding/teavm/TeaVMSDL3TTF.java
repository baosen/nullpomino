package nullpomino.gui.sdl.binding.teavm;

import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import nullpomino.gui.sdl.binding.Ref.IntRef;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.SdlFont;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/**
 * TeaVM implementation of {@link SDL3TTF} via a registered {@code @font-face}
 * and Canvas2D text rendering. The font file is fetched and registered
 * (blocking on the green thread); text is rendered onto an offscreen canvas
 * that becomes a surface.
 */
final class TeaVMSDL3TTF implements SDL3TTF {

	private final TeaVMSDL3 sdl;

	/** Scratch canvas/context for measuring text without a target surface. */
	private final CanvasRenderingContext2D scratch;

	private int nextFamily;

	TeaVMSDL3TTF(TeaVMSDL3 sdl) {
		this.sdl = sdl;
		HTMLCanvasElement c = (HTMLCanvasElement) Window.current().getDocument().createElement("canvas");
		c.setWidth(1);
		c.setHeight(1);
		this.scratch = (CanvasRenderingContext2D) c.getContext("2d");
	}

	@Override public boolean TTF_Init() { return true; }
	@Override public void TTF_Quit() {}

	@Override public SdlFont TTF_OpenFont(String file, float ptsize) {
		if (file == null || file.isEmpty()) return null;
		String family = "nullpottf" + (nextFamily++);
		if (!JsAsync.loadFont(family, Assets.toUrl(file))) {
			sdl.setError("failed to load font: " + file);
			return null;
		}
		return new TeaVMFont(family, Math.max(1, Math.round(ptsize)));
	}

	@Override public void TTF_CloseFont(SdlFont font) {}

	@Override public SdlSurface TTF_RenderText_Blended(SdlFont font, String text, int length,
			SDLStructs.SDL_Color.ByValue fg) {
		if (font == null || text == null || text.isEmpty()) return null;
		TeaVMFont f = (TeaVMFont) font;

		scratch.setFont(f.cssFont);
		int w = Math.max((int) Math.ceil(scratch.measureText(text).getWidth()), 1);
		int ascent = f.pointSize;
		int h = Math.max((int) Math.ceil(f.pointSize * 1.3), 1);

		HTMLCanvasElement canvas = (HTMLCanvasElement) Window.current().getDocument().createElement("canvas");
		canvas.setWidth(w);
		canvas.setHeight(h);
		CanvasRenderingContext2D ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
		ctx.setFont(f.cssFont);
		ctx.setFillStyle("rgba(" + (fg.r & 0xFF) + "," + (fg.g & 0xFF) + "," + (fg.b & 0xFF) + ","
				+ ((fg.a & 0xFF) / 255.0) + ")");
		ctx.fillText(text, 0, ascent);
		return new TeaVMSurface(canvas, w, h);
	}

	@Override public boolean TTF_GetStringSize(SdlFont font, String text, int length, IntRef w, IntRef h) {
		if (font == null || text == null) return false;
		TeaVMFont f = (TeaVMFont) font;
		scratch.setFont(f.cssFont);
		w.value = (int) Math.ceil(scratch.measureText(text).getWidth());
		h.value = (int) Math.ceil(f.pointSize * 1.3);
		return true;
	}
}
