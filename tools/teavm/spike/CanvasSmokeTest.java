package nullpomino.teavmspike;

import org.teavm.jso.browser.Window;
import org.teavm.jso.canvas.CanvasRenderingContext2D;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

/**
 * TeaVM feasibility spike, Test B: Canvas2D + keyboard JS-interop plumbing,
 * fully independent of Test A's game-logic outcome. Draws one filled rect
 * and logs a keypress into a status div, using the exact org.teavm.jso
 * bindings (canvas.CanvasRenderingContext2D, dom.html.HTMLCanvasElement,
 * dom.events.KeyboardEvent) a real binding/teavm renderer/event-bridge
 * would use.
 */
public final class CanvasSmokeTest {
	private CanvasSmokeTest() {}

	public static void main(String[] args) {
		HTMLDocument document = Window.current().getDocument();

		HTMLCanvasElement canvas = (HTMLCanvasElement) document.getElementById("canvas");
		CanvasRenderingContext2D ctx = (CanvasRenderingContext2D) canvas.getContext("2d");
		ctx.setFillStyle("#3366ff");
		ctx.fillRect(10, 10, 50, 50);

		HTMLElement status = document.getElementById("status");
		status.setTextContent("canvas drawn; waiting for keypress...");

		document.getBody().listenKeyDown(evt -> {
			status.setTextContent("keydown: " + evt.getKey() + " (code=" + evt.getCode() + ")");
			ctx.setFillStyle("#33cc33");
			ctx.fillRect(70, 10, 50, 50);
		});
	}
}
