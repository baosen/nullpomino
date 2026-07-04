package nullpomino.gui.sdl.binding.teavm;

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
		resizeToViewport();
		this.display = (CanvasRenderingContext2D) canvas.getContext("2d");

		bridge.install(canvas);
		Window.current().addEventListener("resize", e -> resizeToViewport());
	}

	private void resizeToViewport() {
		int w = Math.max(Window.current().getInnerWidth(), 1);
		int h = Math.max(Window.current().getInnerHeight(), 1);
		canvas.setWidth(w);
		canvas.setHeight(h);
	}

	void setTitle(String title) {
		Window.current().getDocument().setTitle(title);
	}

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
