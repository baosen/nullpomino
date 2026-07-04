package nullpomino.gui.sdl.binding.teavm;

/**
 * The letterbox transform between the logical 640x480 render target and the
 * actual canvas surface, mirroring SDL_LOGICAL_PRESENTATION_LETTERBOX.
 *
 * A single shared computation keeps presentation (paint) and input mapping
 * (SDL_RenderCoordinatesFromWindow) from ever drifting apart.
 */
final class Letterbox {
	final double scale;
	final int offsetX;
	final int offsetY;
	final int scaledW;
	final int scaledH;

	Letterbox(int panelW, int panelH, int logicalW, int logicalH) {
		double s = Math.min((double) panelW / logicalW, (double) panelH / logicalH);
		if(s <= 0 || !Double.isFinite(s)) s = 1.0;
		scale = s;
		scaledW = (int) Math.round(logicalW * s);
		scaledH = (int) Math.round(logicalH * s);
		offsetX = (panelW - scaledW) / 2;
		offsetY = (panelH - scaledH) / 2;
	}

	float toLogicalX(float panelX) {
		return (float) ((panelX - offsetX) / scale);
	}

	float toLogicalY(float panelY) {
		return (float) ((panelY - offsetY) / scale);
	}
}
