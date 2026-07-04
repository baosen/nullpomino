package nullpomino.gui.sdl.binding.teavm;

import org.teavm.jso.JSBody;

/**
 * Drives the browser loading overlay's progress bar (see {@code web/index.html}).
 *
 * <p>The overlay is painted by the HTML shell before {@code classes.js} even
 * downloads. The download phase owns the {@code [0, 0.3]} band (reported from JS
 * as bytes arrive); this class owns the {@code [0.3, 1.0]} band, advancing the
 * bar as the game's HTTP fetches complete via {@code frac = 0.3 + 0.7 *
 * completed / total}.
 *
 * <p>Every asset/config fetch goes through {@link JsAsync}'s {@code @Async}
 * bridges, which suspend the single green thread back to the browser event loop
 * until their callback fires — so the bar repaints between fetches with no
 * artificial yielding.
 *
 * <p>The denominator (set by the web entry point from the two manifests) is an
 * approximation: returning users skip already-seeded config, and options
 * ({@code showbg}/{@code showlineeffect}/{@code se}) change how many res assets
 * {@code ResourceHolderSDL.load()} actually fetches, so the bar can top out just
 * below 100% before {@link #complete()} snaps it. It stays monotonic, which is
 * what matters.
 */
public final class WebProgress {
	private WebProgress() {}

	private static int total;
	private static int completed;
	private static boolean done;

	/** Set the number of Java-side fetches expected; enables {@link #bump}. */
	public static void setTotal(int expected) {
		total = Math.max(expected, 0);
	}

	/** Count one completed fetch and advance the bar. No-op until total is set. */
	public static void bump(String url) {
		if (total <= 0 || done) return;
		if (completed < total) completed++;
		double frac = 0.3 + 0.7 * completed / total;
		if (frac > 0.99) frac = 0.99;
		setProgressJs(frac, labelFor(url));
	}

	/** Snap to 100% and fade the overlay out; idempotent. */
	public static void complete() {
		if (done) return;
		done = true;
		doneJs();
	}

	/** Show an error in the overlay (e.g. a stuck/failed load). */
	public static void showError(String message) {
		showErrorJs(message);
	}

	private static String labelFor(String url) {
		if (url == null) return "Loading…";
		if (url.endsWith(".png")) return "Loading graphics…";
		if (url.endsWith(".wav")) return "Loading sounds…";
		if (url.endsWith(".ttf")) return "Loading fonts…";
		return "Loading…";
	}

	@JSBody(params = {"frac", "label"}, script =
		"if (window.__nppProgress) window.__nppProgress(frac, label);")
	private static native void setProgressJs(double frac, String label);

	@JSBody(params = {}, script = "if (window.__nppDone) window.__nppDone();")
	private static native void doneJs();

	@JSBody(params = {"message"}, script =
		"if (window.__nppError) window.__nppError(message);")
	private static native void showErrorJs(String message);
}
