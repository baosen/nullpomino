// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.Ref.IntRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;

/**
 * Normal display class string (SDL3 version)
 */
public class NormalFontSDL {
	/** Character constant color count */
	public static final int COLOR_WHITE = 0, COLOR_BLUE = 1, COLOR_RED = 2, COLOR_PINK = 3, COLOR_GREEN = 4, COLOR_YELLOW = 5, COLOR_CYAN = 6,
			COLOR_ORANGE = 7, COLOR_PURPLE = 8, COLOR_DARKBLUE = 9;
	/** Extra TTF-only color — the bitmap font atlas only has rows 0–9. */
	public static final int COLOR_LIGHTGRAY = 10;

	private static final int[][] FONT_COLORS = {
			{255, 255, 255},
			{0, 0, 255},
			{255, 0, 0},
			{255, 128, 128},
			{0, 255, 0},
			{255, 255, 0},
			{0, 255, 255},
			{255, 128, 0},
			{255, 0, 255},
			{0, 0, 128},
			{192, 192, 192},
	};

	/**
	 * Get font color as RGB values
	 * @param fontColor font color constant
	 * @return int array {r, g, b}
	 */
	public static int[] getFontColorRGB(int fontColor) {
		if((fontColor < 0) || (fontColor >= FONT_COLORS.length)) fontColor = COLOR_WHITE;
		return FONT_COLORS[fontColor].clone();
	}

	/**
	 * TTF font draw a string
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param fontStr String
	 * @param fontColor Letter color
	 */
	public static void printTTFFont(int fontX, int fontY, String fontStr, int fontColor) {
		if(ResourceHolderSDL.ttfFont == null) return;
		drawTTFText(fontX + 1, fontY + 1, fontStr, new SDLStructs.SDL_Color.ByValue(0, 0, 0));
		drawTTFText(fontX, fontY, fontStr, color(fontColor));
	}

	/**
	 * TTF font draw a string (white)
	 */
	public static void printTTFFont(int fontX, int fontY, String fontStr) {
		printTTFFont(fontX, fontY, fontStr, COLOR_WHITE);
	}

	/**
	 * Measure the rendered pixel width of a string in the loaded TTF font.
	 * Returns 0 if the font isn't loaded or SDL refuses to size the string —
	 * callers should treat that as "draw at the requested origin" rather than
	 * silently mis-aligning.
	 */
	public static int getTTFStringWidth(String fontStr) {
		if(ResourceHolderSDL.ttfFont == null || fontStr == null) return 0;
		IntRef w = new IntRef();
		IntRef h = new IntRef();
		if(!SDL3TTF.INSTANCE.TTF_GetStringSize(ResourceHolderSDL.ttfFont, fontStr, 0, w, h)) return 0;
		return w.getValue();
	}

	/**
	 * Rough average TTF glyph advance in px, for legacy char-count layout math
	 * (scroll windows, truncation, wrap width) that predates proportional-font
	 * rendering. Falls back to 16 (the bitmap font's cell size) if TTF is
	 * unavailable. Not for precise positioning — use {@link #getTTFStringWidth}
	 * for that.
	 */
	public static int getTTFCharWidthPx() {
		int w = getTTFStringWidth("MMMMMMMMMM");
		return (w > 0) ? Math.max(1, w / 10) : 16;
	}

	private static SDLStructs.SDL_Color.ByValue color(int fontColor) {
		int[] rgb = getFontColorRGB(fontColor);
		return new SDLStructs.SDL_Color.ByValue(rgb[0], rgb[1], rgb[2]);
	}

	private static void drawTTFText(int x, int y, String text, SDLStructs.SDL_Color.ByValue color) {
		SdlSurface surface = SDL3TTF.INSTANCE.TTF_RenderText_Blended(ResourceHolderSDL.ttfFont, text, 0, color);
		if(surface == null) return;

		SdlTexture texture = SDL3.INSTANCE.SDL_CreateTextureFromSurface(NullpoMinoSDL.renderer, surface);
		SDL3.INSTANCE.SDL_DestroySurface(surface);
		if(texture == null) return;

		FloatRef width = new FloatRef();
		FloatRef height = new FloatRef();
		SDL3.INSTANCE.SDL_GetTextureSize(texture, width, height);
		SDL3.INSTANCE.SDL_SetTextureBlendMode(texture, SDLConstants.SDL_BLENDMODE_BLEND);
		SDLStructs.SDL_FRect dst = new SDLStructs.SDL_FRect(x, y, width.getValue(), height.getValue());
		SDL3.INSTANCE.SDL_RenderTexture(NullpoMinoSDL.renderer, texture, null, dst);
		SDL3.INSTANCE.SDL_DestroyTexture(texture);
	}

	/**
	 * Reusable rect scratch space for {@link #printFont}. HUD labels and
	 * values are redrawn character-by-character every frame; allocating a
	 * fresh source/destination rect per glyph is cheap on a desktop JVM but
	 * adds up to real pressure on the browser's JS GC.
	 * Consumed synchronously by SDL_RenderTexture before the next glyph
	 * reuses them, so sharing across calls on the single game thread is safe.
	 */
	private static final SDLStructs.SDL_FRect fontRectSrc = new SDLStructs.SDL_FRect();
	private static final SDLStructs.SDL_FRect fontRectDst = new SDLStructs.SDL_FRect();

	/**
	 * Draws the string using bitmap font
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param fontStr String
	 * @param fontColor Letter color
	 * @param scale Enlargement factor (2.0f, 1.0f, or 0.5f)
	 */
	public static void printFont(int fontX, int fontY, String fontStr, int fontColor, float scale) {
		SdlRenderer renderer = NullpoMinoSDL.renderer;
		int dx = fontX;
		int dy = fontY;

		for(int i = 0; i < fontStr.length(); i++) {
			int stringChar = fontStr.charAt(i);

			if(stringChar == 0x0A) {
				// New line (\n)
				if(scale == 2.0f) {
					dy = dy + 32; dx = fontX;
				} else if(scale == 1.0f) {
					dy = dy + 16; dx = fontX;
				} else {
					dy = dy + 8; dx = fontX;
				}
			} else {
				// Character output
				if(scale == 2.0f) {
					int sx = ((stringChar - 32) % 32) * 32;
					int sy = ((stringChar - 32) / 32) * 32 + fontColor * 96;
					setFontRects(sx, sy, dx, dy, 32);
					SDL3.INSTANCE.SDL_RenderTexture(renderer, ResourceHolderSDL.imgFontBig, fontRectSrc, fontRectDst);
					dx = dx + 32;
				} else if(scale == 1.0f) {
					int sx = ((stringChar - 32) % 32) * 16;
					int sy = ((stringChar - 32) / 32) * 16 + fontColor * 48;
					setFontRects(sx, sy, dx, dy, 16);
					SDL3.INSTANCE.SDL_RenderTexture(renderer, ResourceHolderSDL.imgFont, fontRectSrc, fontRectDst);
					dx = dx + 16;
				} else if(scale == 0.5f) {
					int sx = ((stringChar - 32) % 32) * 8;
					int sy = ((stringChar - 32) / 32) * 8 + fontColor * 24;
					setFontRects(sx, sy, dx, dy, 8);
					SDL3.INSTANCE.SDL_RenderTexture(renderer, ResourceHolderSDL.imgFontSmall, fontRectSrc, fontRectDst);
					dx = dx + 8;
				}
			}
		}
	}

	private static void setFontRects(int sx, int sy, int dx, int dy, int size) {
		fontRectSrc.x = sx; fontRectSrc.y = sy; fontRectSrc.w = size; fontRectSrc.h = size;
		fontRectDst.x = dx; fontRectDst.y = dy; fontRectDst.w = size; fontRectDst.h = size;
	}

	public static void printFont(int fontX, int fontY, String fontStr, int fontColor) {
		printFont(fontX, fontY, fontStr, fontColor, 1.0f);
	}

	public static void printFont(int fontX, int fontY, String fontStr) {
		printFont(fontX, fontY, fontStr, COLOR_WHITE);
	}

	/**
	 * Draw a single 16px bitmap glyph mirrored horizontally. Bypasses
	 * {@link #safeString} (so the raw glyph char survives) and blits via the
	 * flip-capable renderer path. The atlas has only a right-pointing arrow
	 * ('b'); mirroring it yields the left-pointing arrow we lack.
	 */
	public static void printFontFlippedH(int fontX, int fontY, char glyph, int fontColor) {
		int sx = ((glyph - 32) % 32) * 16;
		int sy = ((glyph - 32) / 32) * 16 + fontColor * 48;
		setFontRects(sx, sy, fontX, fontY, 16);
		SDL3.INSTANCE.SDL_RenderTextureFlipped(NullpoMinoSDL.renderer, ResourceHolderSDL.imgFont,
			fontRectSrc, fontRectDst, SDLConstants.SDL_FLIP_HORIZONTAL);
	}

	public static void printFont(int fontX, int fontY, String fontStr, boolean flag, int fontColorFalse, int fontColorTrue) {
		if(!flag)
			printFont(fontX, fontY, fontStr, fontColorFalse);
		else
			printFont(fontX, fontY, fontStr, fontColorTrue);
	}

	public static void printFont(int fontX, int fontY, String fontStr, boolean flag) {
		printFont(fontX, fontY, fontStr, flag, COLOR_WHITE, COLOR_RED);
	}

	public static void printFont(int fontX, int fontY, String fontStr, boolean flag, int fontColorFalse, int fontColorTrue, float scale) {
		if(!flag)
			printFont(fontX, fontY, fontStr, fontColorFalse, scale);
		else
			printFont(fontX, fontY, fontStr, fontColorTrue, scale);
	}

	public static void printFont(int fontX, int fontY, String fontStr, boolean flag, float scale) {
		printFont(fontX, fontY, fontStr, flag, COLOR_WHITE, COLOR_RED, scale);
	}

	public static void printFontGrid(int fontX, int fontY, String fontStr, int fontColor) {
		printFont(fontX * 16, fontY * 16, fontStr, fontColor);
	}

	public static void printFontGrid(int fontX, int fontY, String fontStr) {
		printFont(fontX * 16, fontY * 16, fontStr, COLOR_WHITE);
	}

	public static void printFontGrid(int fontX, int fontY, String fontStr, boolean flag, int fontColorFalse, int fontColorTrue) {
		printFont(fontX * 16, fontY * 16, fontStr, flag, fontColorFalse, fontColorTrue);
	}

	public static void printFontGrid(int fontX, int fontY, String fontStr, boolean flag) {
		printFont(fontX * 16, fontY * 16, fontStr, flag, COLOR_WHITE, COLOR_RED);
	}

	/**
	 * Normalize a string for bitmap-font rendering.  The bitmap atlas only contains
	 * ASCII chars 32–95 (space, symbols, digits, uppercase A–Z) plus a handful of
	 * custom glyphs sprinkled through the lowercase slots (e.g. 'b' is reused as a
	 * cursor arrow).  Feeding untouched lowercase or non-ASCII text to
	 * {@link #printFont} therefore fetches garbage glyphs.
	 *
	 * This helper uppercases Latin lowercase and substitutes anything outside
	 * {@code 32..126} with '?'.  Newlines pass through unchanged so the existing
	 * \n-wrapping in printFont still works.
	 *
	 * @param s raw input (may be null)
	 * @return normalized string safe for {@link #printFont}
	 */
	public static String safeString(String s) {
		if(s == null) return "";
		StringBuilder sb = new StringBuilder(s.length());
		for(int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if(c == '\n') { sb.append(c); continue; }
			if(c >= 'a' && c <= 'z') { sb.append((char)(c - 'a' + 'A')); continue; }
			if(c < 32 || c > 126) { sb.append('?'); continue; }
			sb.append(c);
		}
		return sb.toString();
	}
}
