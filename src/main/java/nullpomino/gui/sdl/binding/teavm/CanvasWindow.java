package nullpomino.gui.sdl.binding.teavm;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

import nullpomino.gui.sdl.binding.SdlHandles.SdlWindow;

/**
 * The single game window: the on-screen {@code <canvas id="game">}, sized to
 * fill the viewport. The renderer letterboxes its logical buffer onto it; the
 * DOM event bridge feeds keyboard/mouse input.
 */
final class CanvasWindow implements SdlWindow {

	static final String CANVAS_ID = "game";

	final DomEventBridge bridge = new DomEventBridge();

	private final HTMLCanvasElement canvas;
	private final CanvasRenderingContext2D display;

	volatile CanvasRenderer renderer;

	CanvasWindow(String title) {
		HTMLDocument document = Window.current().getDocument();
		Window.current().getDocument().setTitle(title);

		HTMLCanvasElement el = (HTMLCanvasElement) document.getElementById(CANVAS_ID);
		if (el == null) {
			el = (HTMLCanvasElement) document.createElement("canvas");
			el.setId(CANVAS_ID);
			document.getBody().appendChild(el);
		}
		this.canvas = el;
		this.display = (CanvasRenderingContext2D) canvas.getContext("2d");
		resizeToViewport();

		bridge.install(canvas);
		Window.current().addEventListener("resize", e -> resizeToViewport());
	}

	private void resizeToViewport() {
		int w = Math.max(Window.current().getInnerWidth(), 1);
		int h = Math.max(Window.current().getInnerHeight(), 1);
		canvas.setWidth(w);
		canvas.setHeight(h);
		// Resizing a canvas resets its 2D context state (including
		// imageSmoothingEnabled) back to the default, so this must be
		// re-applied on every resize, not just once at creation.
		CanvasRenderer.disableSmoothing(display);
	}

	void setTitle(String title) {
		Window.current().getDocument().setTitle(title);
	}

	/** Enter/leave real browser fullscreen for the game canvas. */
	void setFullscreen(boolean on) {
		if (on) enterFullscreen(canvas);
		else exitFullscreen();
	}

	// Toggle real browser fullscreen based on the actual browser state, reading it
	// atomically so we always pick the right direction. Called SYNCHRONOUSLY from
	// the F11 keydown handler: the browser requires a live user activation to
	// *re-enter* fullscreen for a short time after an exit, and a request deferred
	// to the game loop (even ~1 frame later) no longer counts as one — that is why
	// the second time you went fullscreen the flag flipped but the canvas never
	// changed. Vendor-prefixed fallbacks; the rejected promise is swallowed to
	// avoid console noise if a request is denied anyway.
	@JSBody(params = {"el"}, script =
		"if (document.fullscreenElement || document.webkitFullscreenElement) {" +
		"  var ex = document.exitFullscreen || document.webkitExitFullscreen;" +
		"  if (ex) ex.call(document);" +
		"} else {" +
		"  var req = el.requestFullscreen || el.webkitRequestFullscreen;" +
		"  if (req) { var p = req.call(el); if (p && p.catch) p.catch(function(){}); }" +
		"}")
	static native void toggleFullscreen(HTMLCanvasElement el);

	// Directional enter/leave, driven through the SDL seam (SDL_SetWindowFullscreen)
	// by the config screen's fullscreen option — F11 uses toggleFullscreen() above.
	// Vendor-prefixed fallback + swallowed rejection. Idempotent via fullscreenElement.
	@JSBody(params = {"el"}, script =
		"if (document.fullscreenElement || document.webkitFullscreenElement) return;" +
		"var req = el.requestFullscreen || el.webkitRequestFullscreen;" +
		"if (req) { var p = req.call(el); if (p && p.catch) p.catch(function(){}); }")
	private static native void enterFullscreen(HTMLCanvasElement el);

	@JSBody(params = {}, script =
		"if (!(document.fullscreenElement || document.webkitFullscreenElement)) return;" +
		"var ex = document.exitFullscreen || document.webkitExitFullscreen;" +
		"if (ex) ex.call(document);")
	private static native void exitFullscreen();

	int panelWidth() {
		return canvas.getWidth();
	}

	int panelHeight() {
		return canvas.getHeight();
	}

	CanvasRenderingContext2D displayContext() {
		return display;
	}

	/** Current letterbox transform for input mapping. */
	Letterbox letterbox() {
		CanvasRenderer r = renderer;
		int lw = (r != null) ? r.logicalW : panelWidth();
		int lh = (r != null) ? r.logicalH : panelHeight();
		return new Letterbox(panelWidth(), panelHeight(), Math.max(lw, 1), Math.max(lh, 1));
	}

	void dispose() {
	}
}
