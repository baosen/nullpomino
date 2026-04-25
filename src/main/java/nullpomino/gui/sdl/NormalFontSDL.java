// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import com.sun.jna.Pointer;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;

/**
 * Normal display class string (SDL3 version)
 */
public class NormalFontSDL {
	/** Character constant color count */
	public static final int COLOR_WHITE = 0, COLOR_BLUE = 1, COLOR_RED = 2, COLOR_PINK = 3, COLOR_GREEN = 4, COLOR_YELLOW = 5, COLOR_CYAN = 6,
			COLOR_ORANGE = 7, COLOR_PURPLE = 8, COLOR_DARKBLUE = 9;

	/**
	 * Get font color as RGB values
	 * @param fontColor font color constant
	 * @return int array {r, g, b}
	 */
	public static int[] getFontColorRGB(int fontColor) {
		switch(fontColor) {
		case COLOR_BLUE:     return new int[]{  0,  0,255};
		case COLOR_RED:      return new int[]{255,  0,  0};
		case COLOR_PINK:     return new int[]{255,128,128};
		case COLOR_GREEN:    return new int[]{  0,255,  0};
		case COLOR_YELLOW:   return new int[]{255,255,  0};
		case COLOR_CYAN:     return new int[]{  0,255,255};
		case COLOR_ORANGE:   return new int[]{255,128,  0};
		case COLOR_PURPLE:   return new int[]{255,  0,255};
		case COLOR_DARKBLUE: return new int[]{  0,  0,128};
		}
		return new int[]{255,255,255};
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
		Pointer renderer = NullpoMinoSDL.renderer;

		// Draw shadow
		SDLStructs.SDL_Color.ByValue shadowColor = new SDLStructs.SDL_Color.ByValue(0, 0, 0);
		Pointer shadowSurface = SDL3TTF.INSTANCE.TTF_RenderText_Blended(
			ResourceHolderSDL.ttfFont, fontStr, 0, shadowColor);
		if(shadowSurface != null) {
			Pointer shadowTex = SDL3.INSTANCE.SDL_CreateTextureFromSurface(renderer, shadowSurface);
			SDL3.INSTANCE.SDL_DestroySurface(shadowSurface);
			if(shadowTex != null) {
				com.sun.jna.ptr.FloatByReference tw = new com.sun.jna.ptr.FloatByReference();
				com.sun.jna.ptr.FloatByReference th = new com.sun.jna.ptr.FloatByReference();
				SDL3.INSTANCE.SDL_GetTextureSize(shadowTex, tw, th);
				SDL3.INSTANCE.SDL_SetTextureBlendMode(shadowTex, SDLConstants.SDL_BLENDMODE_BLEND);
				SDLStructs.SDL_FRect dst = new SDLStructs.SDL_FRect(fontX + 1, fontY + 1, tw.getValue(), th.getValue());
				SDL3.INSTANCE.SDL_RenderTexture(renderer, shadowTex, null, dst);
				SDL3.INSTANCE.SDL_DestroyTexture(shadowTex);
			}
		}

		// Draw text
		int[] rgb = getFontColorRGB(fontColor);
		SDLStructs.SDL_Color.ByValue fgColor = new SDLStructs.SDL_Color.ByValue(rgb[0], rgb[1], rgb[2]);
		Pointer textSurface = SDL3TTF.INSTANCE.TTF_RenderText_Blended(
			ResourceHolderSDL.ttfFont, fontStr, 0, fgColor);
		if(textSurface != null) {
			Pointer textTex = SDL3.INSTANCE.SDL_CreateTextureFromSurface(renderer, textSurface);
			SDL3.INSTANCE.SDL_DestroySurface(textSurface);
			if(textTex != null) {
				com.sun.jna.ptr.FloatByReference tw = new com.sun.jna.ptr.FloatByReference();
				com.sun.jna.ptr.FloatByReference th = new com.sun.jna.ptr.FloatByReference();
				SDL3.INSTANCE.SDL_GetTextureSize(textTex, tw, th);
				SDL3.INSTANCE.SDL_SetTextureBlendMode(textTex, SDLConstants.SDL_BLENDMODE_BLEND);
				SDLStructs.SDL_FRect dst = new SDLStructs.SDL_FRect(fontX, fontY, tw.getValue(), th.getValue());
				SDL3.INSTANCE.SDL_RenderTexture(renderer, textTex, null, dst);
				SDL3.INSTANCE.SDL_DestroyTexture(textTex);
			}
		}
	}

	/**
	 * TTF font draw a string (white)
	 */
	public static void printTTFFont(int fontX, int fontY, String fontStr) {
		printTTFFont(fontX, fontY, fontStr, COLOR_WHITE);
	}

	/**
	 * Draws the string using bitmap font
	 * @param fontX X-coordinate
	 * @param fontY Y-coordinate
	 * @param fontStr String
	 * @param fontColor Letter color
	 * @param scale Enlargement factor (2.0f, 1.0f, or 0.5f)
	 */
	public static void printFont(int fontX, int fontY, String fontStr, int fontColor, float scale) {
		Pointer renderer = NullpoMinoSDL.renderer;
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
					SDLStructs.SDL_FRect rectSrc = new SDLStructs.SDL_FRect(sx, sy, 32, 32);
					SDLStructs.SDL_FRect rectDst = new SDLStructs.SDL_FRect(dx, dy, 32, 32);
					SDL3.INSTANCE.SDL_RenderTexture(renderer, ResourceHolderSDL.imgFontBig, rectSrc, rectDst);
					dx = dx + 32;
				} else if(scale == 1.0f) {
					int sx = ((stringChar - 32) % 32) * 16;
					int sy = ((stringChar - 32) / 32) * 16 + fontColor * 48;
					SDLStructs.SDL_FRect rectSrc = new SDLStructs.SDL_FRect(sx, sy, 16, 16);
					SDLStructs.SDL_FRect rectDst = new SDLStructs.SDL_FRect(dx, dy, 16, 16);
					SDL3.INSTANCE.SDL_RenderTexture(renderer, ResourceHolderSDL.imgFont, rectSrc, rectDst);
					dx = dx + 16;
				} else if(scale == 0.5f) {
					int sx = ((stringChar - 32) % 32) * 8;
					int sy = ((stringChar - 32) / 32) * 8 + fontColor * 24;
					SDLStructs.SDL_FRect rectSrc = new SDLStructs.SDL_FRect(sx, sy, 8, 8);
					SDLStructs.SDL_FRect rectDst = new SDLStructs.SDL_FRect(dx, dy, 8, 8);
					SDL3.INSTANCE.SDL_RenderTexture(renderer, ResourceHolderSDL.imgFontSmall, rectSrc, rectDst);
					dx = dx + 8;
				}
			}
		}
	}

	public static void printFont(int fontX, int fontY, String fontStr, int fontColor) {
		printFont(fontX, fontY, fontStr, fontColor, 1.0f);
	}

	public static void printFont(int fontX, int fontY, String fontStr) {
		printFont(fontX, fontY, fontStr, COLOR_WHITE);
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
