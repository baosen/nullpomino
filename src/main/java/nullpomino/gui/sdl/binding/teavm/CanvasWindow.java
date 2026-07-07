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

	// requestFullscreen() needs a live user activation; it runs ~1 frame after the
	// F11 keydown (well within the browser's transient-activation window). Vendor
	// prefixed fallback + swallow the rejected promise to avoid console noise if the
	// request is denied. Idempotent via fullscreenElement.
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
