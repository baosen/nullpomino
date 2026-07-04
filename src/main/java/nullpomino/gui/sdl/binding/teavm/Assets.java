package nullpomino.gui.sdl.binding.teavm;

/** Shared helpers for turning game asset paths into page-relative URLs. */
final class Assets {
	private Assets() {}

	/**
	 * Game asset paths look like {@code res/graphics/font.png}. A leading
	 * slash is dropped so the URL stays page-relative and works under
	 * subdirectory hosting (e.g. GitHub Pages project sites).
	 */
	static String toUrl(String path) {
		return path.startsWith("/") ? path.substring(1) : path;
	}
}
