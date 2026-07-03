package nullpomino.gui.sdl.binding.web;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import nullpomino.gui.sdl.binding.Ref.IntRef;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.SdlFont;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;

/**
 * Pure-Java implementation of {@link SDL3TTF} via AWT font rendering.
 */
final class WebSDL3TTF implements SDL3TTF {

	private final WebSDL3 sdl;

	/** Scratch image for measuring text without a target surface. */
	private final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

	WebSDL3TTF(WebSDL3 sdl) {
		this.sdl = sdl;
	}

	@Override public boolean TTF_Init() { return true; }
	@Override public void TTF_Quit() {}

	@Override public SdlFont TTF_OpenFont(String file, float ptsize) {
		try {
			Font font = Font.createFont(Font.TRUETYPE_FONT, new File(file)).deriveFont(ptsize);
			return new WebFont(font);
		} catch(Exception e) {
			sdl.setError(String.valueOf(e));
			return null;
		}
	}

	@Override public void TTF_CloseFont(SdlFont font) {}

	@Override public SdlSurface TTF_RenderText_Blended(SdlFont font, String text, int length,
			SDLStructs.SDL_Color.ByValue fg) {
		if(font == null || text == null || text.isEmpty()) return null;
		try {
			Font awtFont = ((WebFont) font).font;
			FontMetrics fm = metrics(awtFont);
			int w = Math.max(fm.stringWidth(text), 1);
			int h = Math.max(fm.getHeight(), 1);

			BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setFont(awtFont);
			g.setColor(new Color(fg.r & 0xFF, fg.g & 0xFF, fg.b & 0xFF, fg.a & 0xFF));
			g.drawString(text, 0, fm.getAscent());
			g.dispose();
			return new WebSurface(img);
		} catch(Exception e) {
			sdl.setError(String.valueOf(e));
			return null;
		}
	}

	@Override public boolean TTF_GetStringSize(SdlFont font, String text, int length, IntRef w, IntRef h) {
		if(font == null || text == null) return false;
		FontMetrics fm = metrics(((WebFont) font).font);
		w.value = fm.stringWidth(text);
		h.value = fm.getHeight();
		return true;
	}

	private FontMetrics metrics(Font font) {
		Graphics2D g = scratch.createGraphics();
		try {
			return g.getFontMetrics(font);
		} finally {
			g.dispose();
		}
	}
}
