package nullpomino.gui.sdl.binding.teavm;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;

import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;

/**
 * Canvas2D implementation of the SDL renderer: an offscreen back buffer the
 * game draws into, blitted (letterboxed) onto the on-screen canvas on present.
 * The whole game runs on one green thread, so no front/back double-buffering
 * or locking is needed.
 *
 * Block/background/font-glyph draws run into the hundreds per frame during
 * gameplay (one texture draw per bitmap-font character alone). The rgba/rgb
 * fill-style strings are therefore recomputed only in {@link #setDrawColor}
 * rather than on every fill, mirroring the desktop web backend's anti-GC
 * discipline — string churn at 60 FPS is real pressure on the JS GC too.
 */
final class CanvasRenderer implements SdlRenderer {

	final CanvasWindow window;

	int logicalW;
	int logicalH;

	private HTMLCanvasElement back;
	private CanvasRenderingContext2D ctx;

	private String rgbaStyle = "rgba(0,0,0,1)";
	private String rgbStyle = "rgb(0,0,0)";
	private int blendMode = SDLConstants.SDL_BLENDMODE_NONE;

	// The loading overlay is dismissed on the first present, i.e. once the title
	// screen's pixels are already on the canvas underneath it — no black flash.
	private static boolean loadingHidden;

	CanvasRenderer(CanvasWindow window, int w, int h) {
		this.window = window;
		allocate(Math.max(w, 1), Math.max(h, 1));
		window.renderer = this;
	}

	void setLogicalSize(int w, int h) {
		allocate(Math.max(w, 1), Math.max(h, 1));
	}

	private void allocate(int w, int h) {
		logicalW = w;
		logicalH = h;
		back = (HTMLCanvasElement) Window.current().getDocument().createElement("canvas");
		back.setWidth(w);
		back.setHeight(h);
		ctx = (CanvasRenderingContext2D) back.getContext("2d");
		disableSmoothing(ctx);
	}

	void setDrawColor(int r, int g, int b, int a) {
		rgbaStyle = "rgba(" + (r & 0xFF) + "," + (g & 0xFF) + "," + (b & 0xFF) + "," + ((a & 0xFF) / 255.0) + ")";
		rgbStyle = "rgb(" + (r & 0xFF) + "," + (g & 0xFF) + "," + (b & 0xFF) + ")";
	}

	void setBlendMode(int mode) {
		blendMode = mode;
	}

	void clear() {
		// The back buffer is opaque, so painting the opaque colour over the
		// whole surface is equivalent to SDL_BLENDMODE_NONE's overwrite.
		ctx.setFillStyle(rgbStyle);
		ctx.fillRect(0, 0, logicalW, logicalH);
	}

	private void applyFillStyle() {
		ctx.setFillStyle(blendMode == SDLConstants.SDL_BLENDMODE_BLEND ? rgbaStyle : rgbStyle);
	}

	void fillRect(SDLStructs.SDL_FRect rect) {
		applyFillStyle();
		if (rect == null) {
			ctx.fillRect(0, 0, logicalW, logicalH);
		} else {
			ctx.fillRect((int) rect.x, (int) rect.y, (int) rect.w, (int) rect.h);
		}
	}

	void drawRect(SDLStructs.SDL_FRect rect) {
		ctx.setStrokeStyle(blendMode == SDLConstants.SDL_BLENDMODE_BLEND ? rgbaStyle : rgbStyle);
		int x, y, w, h;
		if (rect == null) {
			x = 0; y = 0; w = logicalW; h = logicalH;
		} else {
			x = (int) rect.x; y = (int) rect.y; w = (int) rect.w; h = (int) rect.h;
		}
		// +0.5 keeps the 1px outline crisp; w-1/h-1 matches SDL's inclusive rect.
		ctx.setLineWidth(1);
		ctx.strokeRect(x + 0.5, y + 0.5, w - 1, h - 1);
	}

	void renderTexture(TeaVMTexture texture, SDLStructs.SDL_FRect src, SDLStructs.SDL_FRect dst) {
		if (texture == null) return;

		double sx, sy, sw, sh;
		if (src == null) {
			sx = 0; sy = 0; sw = texture.width; sh = texture.height;
		} else {
			sx = src.x; sy = src.y; sw = src.w; sh = src.h;
		}

		double dx, dy, dw, dh;
		if (dst == null) {
			dx = 0; dy = 0; dw = logicalW; dh = logicalH;
		} else {
			dx = dst.x; dy = dst.y; dw = dst.w; dh = dst.h;
		}

		int alpha = texture.alphaMod & 0xFF;
		if (alpha != 255) ctx.setGlobalAlpha(alpha / 255.0);
		ctx.drawImage(texture.image, sx, sy, sw, sh, dx, dy, dw, dh);
		if (alpha != 255) ctx.setGlobalAlpha(1.0);
	}

	/** Same as {@link #renderTexture} but mirrored on the X axis (SDL_FLIP_HORIZONTAL). */
	void renderTextureFlippedH(TeaVMTexture texture, SDLStructs.SDL_FRect src, SDLStructs.SDL_FRect dst) {
		if (texture == null) return;

		double sx, sy, sw, sh;
		if (src == null) {
			sx = 0; sy = 0; sw = texture.width; sh = texture.height;
		} else {
			sx = src.x; sy = src.y; sw = src.w; sh = src.h;
		}

		double dx, dy, dw, dh;
		if (dst == null) {
			dx = 0; dy = 0; dw = logicalW; dh = logicalH;
		} else {
			dx = dst.x; dy = dst.y; dw = dst.w; dh = dst.h;
		}

		int alpha = texture.alphaMod & 0xFF;
		if (alpha != 255) ctx.setGlobalAlpha(alpha / 255.0);
		ctx.save();
		ctx.translate(dx + dw, dy);
		ctx.scale(-1, 1);
		ctx.drawImage(texture.image, sx, sy, sw, sh, 0, 0, dw, dh);
		ctx.restore();
		if (alpha != 255) ctx.setGlobalAlpha(1.0);
	}

	void present() {
		CanvasRenderingContext2D display = window.displayContext();
		int pw = window.panelWidth();
		int ph = window.panelHeight();
		Letterbox box = new Letterbox(pw, ph, logicalW, logicalH);
		display.setFillStyle("rgb(0,0,0)");
		display.fillRect(0, 0, pw, ph);
		display.drawImage(back, box.offsetX, box.offsetY, box.scaledW, box.scaledH);

		if (!loadingHidden) {
			loadingHidden = true;
			WebProgress.complete();
		}
	}

	void dispose() {
		if (window.renderer == this) window.renderer = null;
	}

	@JSBody(params = {"c"}, script = "c.imageSmoothingEnabled = false;")
	static native void disableSmoothing(CanvasRenderingContext2D c);
}
