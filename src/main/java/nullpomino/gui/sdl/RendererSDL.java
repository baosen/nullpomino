// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import java.util.ArrayList;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.EffectObject;
import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.util.CustomProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Game event Processing and rendering process (SDLVersion)
 */
public class RendererSDL extends EventReceiver {
	/** Log */
	static Logger log = LoggerFactory.getLogger(RendererSDL.class);

	/** Production Object */
	protected ArrayList<EffectObject> effectlist;

	/** Line clearDisplay Effects */
	protected boolean showlineeffect;

	/** Heavy production use */
	protected boolean heavyeffect;

	/** fieldBackgroundThe brightness of the */
	protected int fieldbgbright;

	/** Show field BG grid */
	protected boolean showfieldbggrid;

	/** NEXTDarken the field */
	protected boolean darknextarea;

	/** ghost On top of the pieceNEXTDisplay */
	protected boolean nextshadow;

	/** Line clear effect speed */
	protected int lineeffectspeed;

	/**
	 * Reusable rect scratch space for {@link #drawBlock}. A filled Marathon
	 * field draws hundreds of blocks per frame, each needing a source and
	 * destination rect (plus up to four more pairs for sticky-skin corners);
	 * allocating fresh rects for all of them every frame is cheap on a
	 * desktop JVM but produces enough garbage to pressure the browser's JS GC.
	 * The rects are fully overwritten and consumed
	 * synchronously by {@link #renderTexture} before the next reuse, so
	 * sharing them across calls on the single game thread is safe.
	 */
	private final SDLStructs.SDL_FRect blockRectSrc = new SDLStructs.SDL_FRect();
	private final SDLStructs.SDL_FRect blockRectDst = new SDLStructs.SDL_FRect();
	private final SDLStructs.SDL_FRect blockRectSrc2 = new SDLStructs.SDL_FRect();
	private final SDLStructs.SDL_FRect blockRectDst2 = new SDLStructs.SDL_FRect();

	/**
	 * Constructor
	 */
	public RendererSDL() {
		effectlist = new ArrayList<EffectObject>(10*4);

		showbg = NullpoMinoSDL.propConfig.getProperty("option.showbg", true);
		showlineeffect = NullpoMinoSDL.propConfig.getProperty("option.showlineeffect", true);
		heavyeffect = NullpoMinoSDL.propConfig.getProperty("option.heavyeffect", false);
		fieldbgbright = NullpoMinoSDL.propConfig.getProperty("option.fieldbgbright", 128);
		showfieldbggrid = NullpoMinoSDL.propConfig.getProperty("option.showfieldbggrid", true);
		showmeter = NullpoMinoSDL.propConfig.getProperty("option.showmeter", true);
		darknextarea = NullpoMinoSDL.propConfig.getProperty("option.darknextarea", true);
		nextshadow = NullpoMinoSDL.propConfig.getProperty("option.nextshadow", false);
		lineeffectspeed = NullpoMinoSDL.propConfig.getProperty("option.lineeffectspeed", 0);
		outlineghost = NullpoMinoSDL.propConfig.getProperty("option.outlineghost", false);
		sidenext = NullpoMinoSDL.propConfig.getProperty("option.sidenext", false);
		bigsidenext = NullpoMinoSDL.propConfig.getProperty("option.bigsidenext", false);
	}

	/** Helper: get the global renderer handle. */
	private SdlRenderer renderer() {
		return NullpoMinoSDL.renderer;
	}

	private static void setRect(SDLStructs.SDL_FRect rect, float x, float y, float w, float h) {
		rect.x = x; rect.y = y; rect.w = w; rect.h = h;
	}

	/**
	 * Pack an RGB triplet into a single int for use with fillColorRect.
	 */
	public long getColorValue(int r, int g, int b) {
		return ((long)r << 16) | ((long)g << 8) | (long)b;
	}

	/**
	 * Helper to set the renderer draw color from a packed color value and draw a filled rect.
	 */
	private void fillColorRect(int x, int y, int w, int h, long color) {
		int r = (int)((color >> 16) & 0xFF);
		int g = (int)((color >> 8) & 0xFF);
		int b = (int)(color & 0xFF);
		SDL3.setDrawColor(renderer(), r, g, b, 255);
		SDL3.INSTANCE.SDL_RenderFillRect(renderer(), new SDLStructs.SDL_FRect(x, y, w, h));
	}

	/**
	 * Block colorIDDepending onSDLGets the color value for
	 * @param colorID Block colorID
	 * @return SDLColor values ​​for
	 */
	public long getColorByID(int colorID) {
		switch(colorID) {
		case Block.BLOCK_COLOR_GRAY:   return getColorValue( 64, 64, 64);
		case Block.BLOCK_COLOR_RED:    return getColorValue(128,  0,  0);
		case Block.BLOCK_COLOR_ORANGE: return getColorValue(128, 64,  0);
		case Block.BLOCK_COLOR_YELLOW: return getColorValue(128,128,  0);
		case Block.BLOCK_COLOR_GREEN:  return getColorValue(  0,128,  0);
		case Block.BLOCK_COLOR_CYAN:   return getColorValue(  0,128,128);
		case Block.BLOCK_COLOR_BLUE:   return getColorValue(  0,  0,128);
		case Block.BLOCK_COLOR_PURPLE: return getColorValue(128,  0,128);
		}
		return getColorValue(0,0,0);
	}

	public long getColorByIDBright(int colorID) {
		switch(colorID) {
		case Block.BLOCK_COLOR_GRAY:   return getColorValue(128,128,128);
		case Block.BLOCK_COLOR_RED:    return getColorValue(255,  0,  0);
		case Block.BLOCK_COLOR_ORANGE: return getColorValue(255,128,  0);
		case Block.BLOCK_COLOR_YELLOW: return getColorValue(255,255,  0);
		case Block.BLOCK_COLOR_GREEN:  return getColorValue(  0,255,  0);
		case Block.BLOCK_COLOR_CYAN:   return getColorValue(  0,255,255);
		case Block.BLOCK_COLOR_BLUE:   return getColorValue(  0,  0,255);
		case Block.BLOCK_COLOR_PURPLE: return getColorValue(255,  0,255);
		}
		return getColorValue(0,0,0);
	}

	/*
	 * Menu Drawing a string for
	 */
	@Override
	public void drawMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
		int x2 = (scale == 0.5f) ? x * 8 : x * 16;
		int y2 = (scale == 0.5f) ? y * 8 : y * 16;
		if(!engine.owner.menuOnly) {
			x2 += getFieldDisplayPositionX(engine, playerID) + 4;
			if(engine.displaysize == -1) {
				y2 += getFieldDisplayPositionY(engine, playerID) + 4;
			} else {
				y2 += getFieldDisplayPositionY(engine, playerID) + 52;
			}
		}
		NormalFontSDL.printFont(x2, y2, str, color, scale);
	}

	/*
	 * Menu A string forTTF font Drawing on
	 */
	@Override
	public void drawTTFMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		int x2 = x * 16;
		int y2 = y * 16;
		if(!engine.owner.menuOnly) {
			x2 += getFieldDisplayPositionX(engine, playerID) + 4;
			if(engine.displaysize == -1) {
				y2 += getFieldDisplayPositionY(engine, playerID) + 4;
			} else {
				y2 += getFieldDisplayPositionY(engine, playerID) + 52;
			}
		}
		NormalFontSDL.printTTFFont(x2, y2, str, color);
	}

	/*
	 * Render scoreFor font Draw a
	 */
	@Override
	public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
		if(engine.owner.menuOnly) return;

		int size = (scale == 0.5f) ? 8 : 16;
		NormalFontSDL.printFont(getScoreDisplayPositionX(engine, playerID) + (x * size),
								getScoreDisplayPositionY(engine, playerID) + (y * size),
								str, color, scale);
	}

	/*
	 * Render scoreFor font ATTF font Drawing on
	 */
	@Override
	public void drawTTFScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		if(engine.owner.menuOnly) return;

		NormalFontSDL.printTTFFont(getScoreDisplayPositionX(engine, playerID) + (x * 16),
								   getScoreDisplayPositionY(engine, playerID) + (y * 16),
								   str, color);
	}

	/*
	 * Draws the string to the specified coordinates I direct
	 */
	@Override
	public void drawDirectFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
		NormalFontSDL.printFont(x, y, str, color, scale);
	}

	/*
	 * I can draw directly to the specified coordinatesTTF font Draw a
	 */
	@Override
	public void drawTTFDirectFont(GameEngine engine, int playerID, int x, int y, String str, int color) {
		NormalFontSDL.printTTFFont(x, y, str, color);
	}

	/*
	 * SpeedMeterDraw a
	 */
	@Override
	public void drawSpeedMeter(GameEngine engine, int playerID, int x, int y, int s) {
		if(engine.owner.menuOnly) return;

		int dx1 = getScoreDisplayPositionX(engine, playerID) + 6 + (x * 16);
		int dy1 = getScoreDisplayPositionY(engine, playerID) + 6 + (y * 16);

		SDLStructs.SDL_FRect rectSrc = new SDLStructs.SDL_FRect(0, 0, 42, 4);
		SDLStructs.SDL_FRect rectDst = new SDLStructs.SDL_FRect(dx1, dy1, 42, 4);
		SDL3.INSTANCE.SDL_RenderTexture(renderer(), ResourceHolderSDL.imgSprite, rectSrc, rectDst);

		int tempSpeedMeter = s;
		if((tempSpeedMeter < 0) || (tempSpeedMeter > 40)) tempSpeedMeter = 40;

		if(tempSpeedMeter > 0) {
			SDLStructs.SDL_FRect rectSrc2 = new SDLStructs.SDL_FRect(0, 4, tempSpeedMeter, 2);
			SDLStructs.SDL_FRect rectDst2 = new SDLStructs.SDL_FRect(dx1 + 1, dy1 + 1, tempSpeedMeter, 2);
			SDL3.INSTANCE.SDL_RenderTexture(renderer(), ResourceHolderSDL.imgSprite, rectSrc2, rectDst2);
		}
	}

	/*
	 * Get key name by button ID
	 */
	@Override
	public String getKeyNameByButtonID(GameEngine engine, int btnID) {
		int[] keymap = engine.isInGame ? GameKeySDL.gamekey[engine.playerID].keymap : GameKeySDL.gamekey[engine.playerID].keymapNav;

		if((btnID >= 0) && (btnID < keymap.length)) {
			int keycode = keymap[btnID];

			if((keycode >= 0) && (keycode < SDLConstants.SDL_SCANCODE_COUNT)) {
				return SDLConstants.SCANCODE_NAMES[keycode];
			}
		}

		return "";
	}

	/*
	 * Is the skin sticky?
	 */
	@Override
	public boolean isStickySkin(int skin) {
		if((skin >= 0) && (skin < ResourceHolderSDL.blockStickyFlagList.size()) && (ResourceHolderSDL.blockStickyFlagList.get(skin) == true)) {
			return true;
		}
		return false;
	}

	/*
	 * Sound effectsPlayback
	 */
	@Override
	public void playSE(String name) {
		ResourceHolderSDL.soundManager.play(name);
	}

	/*
	 * Set the target surface drawing — now a no-op; the renderer is global.
	 */
	@Override
	public void setGraphics(Object g) {
		// No-op: SDL3 renderer is accessed via NullpoMinoSDL.renderer
	}

	/*
	 * Save the replay
	 */
	@Override
	public void saveReplay(GameManager owner, CustomProperties prop) {
		if(owner.mode.isNetplayMode()) return;

		saveReplay(owner, prop, NullpoMinoSDL.propGlobal.getProperty("custom.replay.directory", "replay"));
	}

	/**
	 * Helper to render a texture to the global renderer.
	 */
	private void renderTexture(SdlTexture texture, SDLStructs.SDL_FRect src, SDLStructs.SDL_FRect dst) {
		SDL3.INSTANCE.SDL_RenderTexture(renderer(), texture, src, dst);
	}

	/**
	 * Helper to render a full texture to fill the entire renderer output.
	 */
	private void renderTextureFullscreen(SdlTexture texture) {
		SDL3.INSTANCE.SDL_RenderTexture(renderer(), texture, null, null);
	}

	/**
	 * Get the width of a texture.
	 */
	private int getTextureWidth(SdlTexture texture) {
		if(texture == null) return -1;
		FloatRef w = new FloatRef();
		FloatRef h = new FloatRef();
		SDL3.INSTANCE.SDL_GetTextureSize(texture, w, h);
		return (int)w.getValue();
	}

	/**
	 * Get the height of a texture.
	 */
	private int getTextureHeight(SdlTexture texture) {
		if(texture == null) return -1;
		FloatRef w = new FloatRef();
		FloatRef h = new FloatRef();
		SDL3.INSTANCE.SDL_GetTextureSize(texture, w, h);
		return (int)h.getValue();
	}

	/**
	 * Draw a dark overlay rectangle using the renderer's blend mode.
	 * @param x X position
	 * @param y Y position
	 * @param w Width
	 * @param h Height
	 * @param alpha Alpha value (0-255)
	 */
	private void drawDarkOverlay(int x, int y, int w, int h, int alpha) {
		SDL3.INSTANCE.SDL_SetRenderDrawBlendMode(renderer(), SDLConstants.SDL_BLENDMODE_BLEND);
		SDL3.setDrawColor(renderer(), 0, 0, 0, alpha);
		SDL3.INSTANCE.SDL_RenderFillRect(renderer(), new SDLStructs.SDL_FRect(x, y, w, h));
		SDL3.INSTANCE.SDL_SetRenderDrawBlendMode(renderer(), SDLConstants.SDL_BLENDMODE_NONE);
	}

	/**
	 * Draw a bright overlay rectangle using the renderer's blend mode.
	 * @param x X position
	 * @param y Y position
	 * @param w Width
	 * @param h Height
	 * @param alpha Alpha value (0-255)
	 */
	private void drawBrightOverlay(int x, int y, int w, int h, int alpha) {
		SDL3.INSTANCE.SDL_SetRenderDrawBlendMode(renderer(), SDLConstants.SDL_BLENDMODE_BLEND);
		SDL3.setDrawColor(renderer(), 255, 255, 255, alpha);
		SDL3.INSTANCE.SDL_RenderFillRect(renderer(), new SDLStructs.SDL_FRect(x, y, w, h));
		SDL3.INSTANCE.SDL_SetRenderDrawBlendMode(renderer(), SDLConstants.SDL_BLENDMODE_NONE);
	}

	/**
	 * Draw a block
	 * @param x X pos
	 * @param y Y pos
	 * @param color Color
	 * @param skin Skin
	 * @param bone true to use bone block ([][][][])
	 * @param darkness Darkness or brightness
	 * @param alpha Alpha
	 * @param scale Size (0.5f, 1.0f, 2.0f)
	 * @param attr Attribute
	 */
	protected void drawBlock(int x, int y, int color, int skin, boolean bone, float darkness, float alpha, float scale, int attr) {
		if(color <= Block.BLOCK_COLOR_INVALID) return;
		if(skin >= ResourceHolderSDL.imgNormalBlockList.size()) skin = 0;

		boolean isSpecialBlocks = (color >= Block.BLOCK_COLOR_COUNT);
		boolean isSticky = ResourceHolderSDL.blockStickyFlagList.get(skin);

		int size = (int)(16 * scale);
		SdlTexture img = null;
		if(scale == 0.5f)
			img = ResourceHolderSDL.imgSmallBlockList.get(skin);
		else if(scale == 2.0f)
			img = ResourceHolderSDL.imgBigBlockList.get(skin);
		else
			img = ResourceHolderSDL.imgNormalBlockList.get(skin);

		int sx = color * size;
		if(bone) sx += 9 * size;
		int sy = 0;
		if(isSpecialBlocks) sx = ((color - Block.BLOCK_COLOR_COUNT) + 18) * size;

		if(isSticky) {
			if(isSpecialBlocks) {
				sx = (color - Block.BLOCK_COLOR_COUNT) * size;
				sy = 18 * size;
			} else {
				sx = 0;
				if((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) sx |= 0x1;
				if((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) sx |= 0x2;
				if((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0) sx |= 0x4;
				if((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0) sx |= 0x8;
				sx *= size;
				sy = color * size;
				if(bone) sy += 9 * size;
			}
		}

		int imageWidth = getTextureWidth(img);
		if((sx >= imageWidth) && (imageWidth != -1)) sx = 0;
		int imageHeight = getTextureHeight(img);
		if((sy >= imageHeight) && (imageHeight != -1)) sy = 0;

		blockRectSrc.x = sx; blockRectSrc.y = sy; blockRectSrc.w = size; blockRectSrc.h = size;
		blockRectDst.x = x; blockRectDst.y = y; blockRectDst.w = size; blockRectDst.h = size;

		if(alpha < 1.0f) {
			int alphalv = (int)(255 * alpha);
			SDL3.setTextureAlpha(img, alphalv);
		} else {
			SDL3.setTextureAlpha(img, 255);
		}

		renderTexture(img, blockRectSrc, blockRectDst);

		if(isSticky && !isSpecialBlocks) {
			int d = 16 * size;
			int h = (size/2);

			if( ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0) ) {
				setRect(blockRectDst2, x, y, h, h);
				setRect(blockRectSrc2, d, sy, h, h);
				renderTexture(img, blockRectSrc2, blockRectDst2);
			}
			if( ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_UP) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0) ) {
				setRect(blockRectDst2, x + h, y, h, h);
				setRect(blockRectSrc2, d + h, sy, h, h);
				renderTexture(img, blockRectSrc2, blockRectDst2);
			}
			if( ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) != 0) ) {
				setRect(blockRectDst2, x, y + h, h, h);
				setRect(blockRectSrc2, d, sy + h, h, h);
				renderTexture(img, blockRectSrc2, blockRectDst2);
			}
			if( ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_DOWN) != 0) && ((attr & Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT) != 0) ) {
				setRect(blockRectDst2, x + h, y + h, h, h);
				setRect(blockRectSrc2, d + h, sy + h, h, h);
				renderTexture(img, blockRectSrc2, blockRectDst2);
			}
		}

		if(darkness > 0) {
			int alphalv = (int)(255 * darkness);
			drawDarkOverlay(x, y, size, size, alphalv);
		} else if(darkness < 0) {
			int alphalv = (int)(255 * -darkness);
			drawBrightOverlay(x, y, size, size, alphalv);
		}
	}

	/**
	 * BlockDraw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param color Color
	 * @param skin Pattern
	 * @param bone BoneBlock
	 * @param darkness Lightness or darkness
	 * @param alpha Transparency
	 * @param scale Enlargement factor
	 */
	protected void drawBlock(int x, int y, int color, int skin, boolean bone, float darkness, float alpha, float scale) {
		drawBlock(x, y, color, skin, bone, darkness, alpha, scale, 0);
	}

	/**
	 * BlockUsing an instance of the classBlockDraw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk BlockInstance of a class
	 */
	protected void drawBlock(int x, int y, Block blk) {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), blk.darkness, blk.alpha, 1.0f, blk.attribute);
		drawItemGlyph(x, y, blk.item, 1.0f);
	}

	/**
	 * BlockUsing an instance of the classBlockDraw a (You can specify the magnification)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk BlockInstance of a class
	 * @param scale Enlargement factor
	 */
	protected void drawBlock(int x, int y, Block blk, float scale) {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), blk.darkness, blk.alpha, scale, blk.attribute);
		drawItemGlyph(x, y, blk.item, scale);
	}

	/**
	 * BlockUsing an instance of the classBlockDraw a (You can specify the magnification and dark)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param blk BlockInstance of a class
	 * @param scale Enlargement factor
	 * @param darkness Lightness or darkness
	 */
	protected void drawBlock(int x, int y, Block blk, float scale, float darkness) {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), darkness, blk.alpha, scale, blk.attribute);
		drawItemGlyph(x, y, blk.item, scale);
	}

	protected void drawBlockForceVisible(int x, int y, Block blk, float scale) {
		drawBlock(x, y, blk.getDrawColor(), blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE), blk.darkness,
				(0.5f*blk.alpha)+0.5f, scale, blk.attribute);
		drawItemGlyph(x, y, blk.item, scale);
	}

	/**
	 * Draws the compact identifying glyph used by scripted item blocks.
	 */
	private void drawItemGlyph(int x, int y, int item, float scale) {
		if((item != Block.BLOCK_ITEM_FREE_FALL) && (item != Block.BLOCK_ITEM_DEL_EVEN)) return;

		int pixel = Math.max(1, (int)(2 * scale));
		int size = (int)(16 * scale);
		int glyphSize = pixel * 8;
		int glyphX = x + ((size - glyphSize) / 2);
		int glyphY = y + ((size - glyphSize) / 2);
		drawItemGlyphLayer(glyphX + pixel, glyphY + pixel, pixel, item, getColorValue(0, 0, 0));
		drawItemGlyphLayer(glyphX, glyphY, pixel, item, getColorValue(255, 255, 255));
	}

	private void drawItemGlyphLayer(int x, int y, int pixel, int item, long color) {
		if(item == Block.BLOCK_ITEM_FREE_FALL) {
			// exclamation mark: stroke rows 0-4, gap row 5, dot row 6
			for(int i = 0; i <= 4; i++) {
				drawItemGlyphPixel(x, y, pixel, 3, i, color);
				drawItemGlyphPixel(x, y, pixel, 4, i, color);
			}
			drawItemGlyphPixel(x, y, pixel, 3, 6, color);
			drawItemGlyphPixel(x, y, pixel, 4, 6, color);
		} else {
			for(int i = 1; i <= 6; i++) {
				drawItemGlyphPixel(x, y, pixel, i, 1, color);
				drawItemGlyphPixel(x, y, pixel, i, 3, color);
				drawItemGlyphPixel(x, y, pixel, i, 5, color);
			}
		}
	}

	private void drawItemGlyphPixel(int x, int y, int pixel, int pixelX, int pixelY, long color) {
		fillColorRect(x + (pixelX * pixel), y + (pixelY * pixel), pixel, pixel, color);
	}

	static String getItemLabel(Piece piece) {
		if((piece == null) || (piece.block == null)) return null;
		for(Block block : piece.block) {
			if(block.item == Block.BLOCK_ITEM_FREE_FALL) return "FREE\nFALL";
			if(block.item == Block.BLOCK_ITEM_DEL_EVEN) return "DELETE\nEVEN";
		}
		return null;
	}

	/**
	 * BlockDraw a piece
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece Peace to draw
	 */
	protected void drawPiece(int x, int y, Piece piece) {
		drawPiece(x, y, piece, 1.0f);
	}

	/**
	 * BlockDraw a piece (You can specify the magnification)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece Peace to draw
	 * @param scale Enlargement factor
	 */
	protected void drawPiece(int x, int y, Piece piece, float scale) {
		drawPiece(x, y, piece, scale, 0f);
	}

	/**
	 * BlockDraw a piece (You can specify the brightness or darkness)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param piece Peace to draw
	 * @param scale Enlargement factor
	 * @param darkness Lightness or darkness
	 */
	protected void drawPiece(int x, int y, Piece piece, float scale, float darkness) {
		for(int i = 0; i < piece.getMaxBlock(); i++) {
			int x2 = x + (int)(piece.dataX[piece.direction][i] * 16 * scale);
			int y2 = y + (int)(piece.dataY[piece.direction][i] * 16 * scale);

			Block blkTemp = new Block(piece.block[i]);
			blkTemp.darkness = darkness;

			drawBlock(x2, y2, blkTemp, scale);
		}
	}

	/**
	 * Currently working onBlockDraw a piece (Y-coordinateThe0MoreBlockDisplay only)
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected void drawCurrentPiece(int x, int y, GameEngine engine, float scale) {
		Piece piece = engine.nowPieceObject;
		int blksize = (int)(16 * scale);

		if(piece != null) {
			for(int i = 0; i < piece.getMaxBlock(); i++) {
				if(!piece.big) {
					int x2 = engine.nowPieceX + piece.dataX[piece.direction][i];
					int y2 = engine.nowPieceY + piece.dataY[piece.direction][i];

					if(y2 >= 0) {
						Block blkTemp = piece.block[i];
						if(engine.nowPieceColorOverride >= 0) {
							blkTemp = new Block(piece.block[i]);
							blkTemp.color = engine.nowPieceColorOverride;
						}
						drawBlock(x + (x2 * blksize), y + (y2 * blksize), blkTemp, scale);
					}
				} else {
					int x2 = engine.nowPieceX + (piece.dataX[piece.direction][i] * 2);
					int y2 = engine.nowPieceY + (piece.dataY[piece.direction][i] * 2);

					Block blkTemp = piece.block[i];
					if(engine.nowPieceColorOverride >= 0) {
						blkTemp = new Block(piece.block[i]);
						blkTemp.color = engine.nowPieceColorOverride;
					}
					drawBlock(x + (x2 * blksize), y + (y2 * blksize), blkTemp, scale * 2.0f);
				}
			}
		}
	}

	/**
	 * Currently working onBlockOf Peaceghost Draw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 * @param scale Display magnification
	 */
	protected void drawGhostPiece(int x, int y, GameEngine engine, float scale) {
		Piece piece = engine.nowPieceObject;
		int blksize = (int)(16 * scale);

		if(piece != null) {
			for(int i = 0; i < piece.getMaxBlock(); i++) {
				if(!piece.big) {
					int x2 = engine.nowPieceX + piece.dataX[piece.direction][i];
					int y2 = engine.nowPieceBottomY + piece.dataY[piece.direction][i];

					if(y2 >= 0) {
						if(outlineghost) {
							Block blkTemp = piece.block[i];
							int x3 = x + (x2 * blksize);
							int y3 = y + (y2 * blksize);

							int colorID = blkTemp.getDrawColor();
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) colorID = -1;
							long color = getColorByID(colorID);
							fillColorRect(x3, y3, blksize, blksize, color);

							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x3,y3,blksize,1));
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x3,y3+1,blksize,1));
							}
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x3,y3 + blksize-1,blksize,1));
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x3,y3 + blksize-2,blksize,1));
							}
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT)) {
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x3,y3,1,blksize));
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x3+1,y3,1,blksize));
							}
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT)) {
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x3 + blksize-1,y3,1,blksize));
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x3 + blksize-2,y3,1,blksize));
							}

							long whiteColor = getColorValue(255, 255, 255);
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
								fillColorRect(x3, y3, 2, 2, whiteColor);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
								fillColorRect(x3, y3 + (blksize-2), 2, 2, whiteColor);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
								fillColorRect(x3 + (blksize-2), y3, 2, 2, whiteColor);
							}
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
								fillColorRect(x3 + (blksize-2), y3 + (blksize-2), 2, 2, whiteColor);
							}
						} else {
							Block blkTemp = new Block(piece.block[i]);
							blkTemp.darkness = 0.3f;
							if(engine.nowPieceColorOverride >= 0) {
								blkTemp.color = engine.nowPieceColorOverride;
							}
							drawBlock(x + (x2 * blksize), y + (y2 * blksize), blkTemp, scale);
						}
					}
				} else {
					int x2 = engine.nowPieceX + (piece.dataX[piece.direction][i] * 2);
					int y2 = engine.nowPieceBottomY + (piece.dataY[piece.direction][i] * 2);

					if(outlineghost) {
						Block blkTemp = piece.block[i];
						int x3 = x + (x2 * blksize);
						int y3 = y + (y2 * blksize);

						int colorID = blkTemp.getDrawColor();
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) colorID = -1;
						long color = getColorByID(colorID);
						fillColorRect(x3, y3, blksize * 2, blksize * 2, color);

						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize*2,1), new SDLStructs.SDL_FRect(x3,y3,blksize*2,1));
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize*2,1), new SDLStructs.SDL_FRect(x3,y3+1,blksize*2,1));
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize*2,1), new SDLStructs.SDL_FRect(x3,y3 + blksize*2-1,blksize*2,1));
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize*2,1), new SDLStructs.SDL_FRect(x3,y3 + blksize*2-2,blksize*2,1));
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT)) {
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize*2), new SDLStructs.SDL_FRect(x3,y3,1,blksize*2));
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize*2), new SDLStructs.SDL_FRect(x3+1,y3,1,blksize*2));
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT)) {
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize*2), new SDLStructs.SDL_FRect(x3 + blksize*2-1,y3,1,blksize*2));
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize*2), new SDLStructs.SDL_FRect(x3 + blksize*2-2,y3,1,blksize*2));
						}

						long whiteColor = getColorValue(255, 255, 255);
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							fillColorRect(x3, y3, 2, 2, whiteColor);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							fillColorRect(x3, y3 + (blksize*2-2), 2, 2, whiteColor);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							fillColorRect(x3 + (blksize*2-2), y3, 2, 2, whiteColor);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							fillColorRect(x3 + (blksize*2-2), y3 + (blksize*2-2), 2, 2, whiteColor);
						}
					} else {
						Block blkTemp = new Block(piece.block[i]);
						blkTemp.darkness = 0.3f;
						if(engine.nowPieceColorOverride >= 0) {
							blkTemp.color = engine.nowPieceColorOverride;
						}
						drawBlock(x + (x2 * blksize), y + (y2 * blksize), blkTemp, scale * 2.0f);
					}
				}
			}
		}
	}

	protected void drawHintPiece(int x, int y, GameEngine engine, float scale) {
		Piece piece = engine.aiHintPiece;
		if (piece != null) {
			piece.direction=engine.ai.bestRt;
			piece.updateConnectData();
			int blksize = (int)(16 * scale);

			for(int i = 0; i < piece.getMaxBlock(); i++) {
					if(!piece.big) {
						int x2 = engine.ai.bestX + piece.dataX[piece.direction][i];
						int y2 = engine.ai.bestY + piece.dataY[piece.direction][i];

						if(y2 >= 0) {

							Block blkTemp = piece.block[i];
							int x3 = x + (x2 * blksize);
							int y3 = y + (y2 * blksize);
							int ls = (blksize-1);

							int colorID = blkTemp.getDrawColor();
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) colorID = -1;
							long color = getColorByIDBright(colorID);

							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP))
								fillColorRect(x3, y3, ls, 2, color);
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN))
								fillColorRect(x3, y3 + ls - 1, ls, 2, color);
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT))
								fillColorRect(x3, y3, 2, ls, color);
							if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT))
								fillColorRect(x3 + ls - 1, y3, 2, ls, color);
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_UP))
								fillColorRect(x3, y3, 2, 2, color);
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN))
								fillColorRect(x3, y3 + (blksize-2), 2, 2, color);
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_UP))
								fillColorRect(x3 + (blksize-2), y3, 2, 2, color);
							if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN))
								fillColorRect(x3 + (blksize-2), y3 + (blksize-2), 2, 2, color);
						}
					} else {
						int x2 = engine.ai.bestX + (piece.dataX[piece.direction][i] * 2);
						int y2 = engine.ai.bestY + (piece.dataY[piece.direction][i] * 2);

						Block blkTemp = piece.block[i];
						int x3 = x + (x2 * blksize);
						int y3 = y + (y2 * blksize);

						int colorID = blkTemp.getDrawColor();
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) colorID = -1;
						long color = getColorByID(colorID);

						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize*2,1), new SDLStructs.SDL_FRect(x3,y3,blksize*2,1));
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize*2,1), new SDLStructs.SDL_FRect(x3,y3+1,blksize*2,1));
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize*2,1), new SDLStructs.SDL_FRect(x3,y3 + blksize*2-1,blksize*2,1));
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize*2,1), new SDLStructs.SDL_FRect(x3,y3 + blksize*2-2,blksize*2,1));
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT)) {
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize*2), new SDLStructs.SDL_FRect(x3,y3,1,blksize*2));
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize*2), new SDLStructs.SDL_FRect(x3+1,y3,1,blksize*2));
						}
						if(!blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT)) {
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize*2), new SDLStructs.SDL_FRect(x3 + blksize*2-1,y3,1,blksize*2));
							renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize*2), new SDLStructs.SDL_FRect(x3 + blksize*2-2,y3,1,blksize*2));
						}

						long whiteColor = getColorValue(255, 255, 255);
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							fillColorRect(x3, y3, 2, 2, whiteColor);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							fillColorRect(x3, y3 + (blksize*2-2), 2, 2, whiteColor);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_UP)) {
							fillColorRect(x3 + (blksize*2-2), y3, 2, 2, whiteColor);
						}
						if(blkTemp.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN)) {
							fillColorRect(x3 + (blksize*2-2), y3 + (blksize*2-2), 2, 2, whiteColor);
						}

					}
			}
		}
	}

	/**
	 * fieldOfBlockDraw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 * @param size Display size (-1=small, 0=normal, 1=big)
	 */
	protected void drawField(int x, int y, GameEngine engine, int size) {
		int blksize = 16;
		float scale = 1.0f;
		if (size == -1) {
			blksize = 8;
			scale = 0.5f;
		} else if (size == 1){
			blksize = 32;
			scale = 2.0f;
		}

		Field field = engine.field;
		int width = 10;
		int height = 20;
		int viewHeight = 20;

		if(field != null) {
			width = field.getWidth();
			viewHeight = height = field.getHeight();
		}
		if((engine.heboHiddenEnable) && (engine.gameActive) && (field != null)) {
			viewHeight -= engine.heboHiddenYNow;
		}

		int outlineType = engine.blockOutlineType;
		if(engine.owBlockOutlineType != -1) outlineType = engine.owBlockOutlineType;

		SdlTexture imgFieldbg = ResourceHolderSDL.imgFieldbg;
		if(engine.owner.getPlayers() < 2)
			SDL3.setTextureAlpha(imgFieldbg, fieldbgbright);
		else
			SDL3.setTextureAlpha(imgFieldbg, 255);

		for(int i = 0; i < viewHeight; i++) {
			for(int j = 0; j < width; j++) {
				int x2 = x + (j * blksize);
				int y2 = y + (i * blksize);

				Block blk = null;
				if(field != null) blk = field.getBlock(j, i);

				if((field != null) && (blk.color > Block.BLOCK_COLOR_NONE)) {
					if(blk.getAttribute(Block.BLOCK_ATTRIBUTE_WALL)) {
						drawBlock(x2, y2, Block.BLOCK_COLOR_NONE, blk.skin, blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE),
								  blk.darkness, blk.alpha, scale, blk.attribute);
					} else if (engine.owner.replayMode && engine.owner.replayShowInvisible) {
						drawBlockForceVisible(x2, y2, blk, scale);
					} else if(blk.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE)) {
						drawBlock(x2, y2, blk, scale);
					}

					if( (!blk.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE) || (blk.alpha < 1.0f)) && (fieldbgbright > 0) ) {
						if( ((width > 10) && (height > 20)) || (!showfieldbggrid) ) {
							int sx = (((i % 2 == 0) && (j % 2 == 0)) || ((i % 2 != 0) && (j % 2 != 0))) ? 0 : 32;
							renderTexture(imgFieldbg, new SDLStructs.SDL_FRect(sx,0,blksize,blksize), new SDLStructs.SDL_FRect(x2,y2,blksize,blksize));
						}
					}

					if(blk.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE) && !blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) {
						SDL3.setTextureAlpha(ResourceHolderSDL.imgSprite, (int)(255 * blk.alpha));

						if(outlineType == GameEngine.BLOCK_OUTLINE_NORMAL) {
							if(field.getBlockColor(j, i - 1) == Block.BLOCK_COLOR_NONE)
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x2,y2,blksize,1));
							if(field.getBlockColor(j, i + 1) == Block.BLOCK_COLOR_NONE)
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x2,y2 + blksize-1,blksize,1));
							if(field.getBlockColor(j - 1, i) == Block.BLOCK_COLOR_NONE)
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x2,y2,1,blksize));
							if(field.getBlockColor(j + 1, i) == Block.BLOCK_COLOR_NONE)
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x2 + blksize-1,y2,1,blksize));
						} else if(outlineType == GameEngine.BLOCK_OUTLINE_CONNECT) {
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP))
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x2,y2,blksize,1));
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN))
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x2,y2 + blksize-1,blksize,1));
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT))
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x2,y2,1,blksize));
							if(!blk.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT))
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x2 + blksize-1,y2,1,blksize));
						} else if(outlineType == GameEngine.BLOCK_OUTLINE_SAMECOLOR) {
							if(field.getBlockColor(j, i - 1) != blk.color)
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x2,y2,blksize,1));
							if(field.getBlockColor(j, i + 1) != blk.color)
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(1,16,blksize,1), new SDLStructs.SDL_FRect(x2,y2 + blksize-1,blksize,1));
							if(field.getBlockColor(j - 1, i) != blk.color)
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x2,y2,1,blksize));
							if(field.getBlockColor(j + 1, i) != blk.color)
								renderTexture(ResourceHolderSDL.imgSprite, new SDLStructs.SDL_FRect(0,16,1,blksize), new SDLStructs.SDL_FRect(x2 + blksize-1,y2,1,blksize));
						}

						SDL3.setTextureAlpha(ResourceHolderSDL.imgSprite, 255);
					}
				} else if(fieldbgbright > 0) {
					if( ((width > 10) && (height > 20)) || (!showfieldbggrid) ) {
						int sx = (((i % 2 == 0) && (j % 2 == 0)) || ((i % 2 != 0) && (j % 2 != 0))) ? 0 : 32;
						renderTexture(imgFieldbg, new SDLStructs.SDL_FRect(sx,0,blksize,blksize), new SDLStructs.SDL_FRect(x2,y2,blksize,blksize));
					}
				}
			}
		}

		// BunglerHIDDEN
		if((engine.heboHiddenEnable) && (engine.gameActive) && (field != null)) {
			int maxY = engine.heboHiddenYNow;
			if(maxY > height) maxY = height;
			for(int i = 0; i < maxY; i++) {
				for(int j = 0; j < width; j++) {
					drawBlock(x + (j * blksize), y + ((height - 1 - i) * blksize), Block.BLOCK_COLOR_GRAY, 0, false, 0.0f, 1.0f, scale);
				}
			}
		}
	}

	/**
	 * Field frameDraw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 * @param displaysize Display size (-1=small, 0=normal, 1=big)
	 */
	protected void drawFrame(int x, int y, GameEngine engine, int displaysize) {
		int size = 4;
		if (displaysize == -1)
			size = 2;
		else if (displaysize == 1)
			size = 8;
		int width = 10;
		int height = 20;
		int offsetX = 0;

		if(engine.field != null) {
			width = engine.field.getWidth();
			height = engine.field.getHeight();
		}
		offsetX = engine.framecolor * 16;

		// Field Background
		if(fieldbgbright > 0) {
			if((width <= 10) && (height <= 20) && (showfieldbggrid)) {
				SdlTexture img = ResourceHolderSDL.imgFieldbg2;
				if(displaysize == -1) img = ResourceHolderSDL.imgFieldbg2Small;
				if(displaysize == 1) img = ResourceHolderSDL.imgFieldbg2Big;

				if(engine.owner.getPlayers() < 2)
					SDL3.setTextureAlpha(img, fieldbgbright);
				else
					SDL3.setTextureAlpha(img, 255);

				renderTexture(img, new SDLStructs.SDL_FRect(0, 0, width*size*4, height*size*4), new SDLStructs.SDL_FRect(x + 4, y + 4, width*size*4, height*size*4));
			}
		}

		SDLStructs.SDL_FRect rectSrc = null;
		SDLStructs.SDL_FRect rectDst = null;

		// UpAnd the lower
		int maxWidth = (width * size);
		if(showmeter) maxWidth = (width * size) + 2;

		for(int i = 0; i < maxWidth; i++) {
			rectSrc = new SDLStructs.SDL_FRect(offsetX + 4, 0, 4, 4);
			rectDst = new SDLStructs.SDL_FRect(x + ((i + 1) * 4), y, 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);

			rectSrc = new SDLStructs.SDL_FRect(offsetX + 4, 8, 4, 4);
			rectDst = new SDLStructs.SDL_FRect(x + ((i + 1) * 4), y + (height * size * 4) + 4, 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);
		}

		// Left and Right
		for(int i = 0; i < height * size; i++) {
			rectSrc = new SDLStructs.SDL_FRect(offsetX + 0, 4, 4, 4);
			rectDst = new SDLStructs.SDL_FRect(x, y + ((i + 1) * 4), 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);

			rectSrc = new SDLStructs.SDL_FRect(offsetX + 8, 4, 4, 4);
			if(showmeter) rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 12, y + ((i + 1) * 4), 4, 4);
			else rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 4, y + ((i + 1) * 4), 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);
		}

		// Upper left
		rectSrc = new SDLStructs.SDL_FRect(offsetX + 0, 0, 4, 4);
		rectDst = new SDLStructs.SDL_FRect(x, y, 4, 4);
		renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);

		// Lower left
		rectSrc = new SDLStructs.SDL_FRect(offsetX + 0, 8, 4, 4);
		rectDst = new SDLStructs.SDL_FRect(x, y + (height * size * 4) + 4, 4, 4);
		renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);

		if(showmeter) {
			// MeterONWhen the upper right corner of the
			rectSrc = new SDLStructs.SDL_FRect(offsetX + 8, 0, 4, 4);
			rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 12, y, 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);

			// MeterONWhen the lower-right corner of
			rectSrc = new SDLStructs.SDL_FRect(offsetX + 8, 8, 4, 4);
			rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 12, y + (height * size * 4) + 4, 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);

			// RightMeterFrame
			for(int i = 0; i < height * size; i++) {
				rectSrc = new SDLStructs.SDL_FRect(offsetX + 12, 4, 4, 4);
				rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 4, y + ((i + 1) * 4), 4, 4);
				renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);
			}

			rectSrc = new SDLStructs.SDL_FRect(offsetX + 12, 0, 4, 4);
			rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 4, y, 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);

			rectSrc = new SDLStructs.SDL_FRect(offsetX + 12, 8, 4, 4);
			rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 4, y + (height * size * 4) + 4, 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);

			// RightMeter
			int maxHeight = height * size * 4;
			if(engine.meterValueSub > 0 || engine.meterValue > 0)
				maxHeight -= Math.max(engine.meterValue, engine.meterValueSub);

			for(int i = 0; i < maxHeight; i++) {
				rectSrc = new SDLStructs.SDL_FRect(59, 0, 4, 1);
				rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 8, y + 4 + i, 4, 1);
				renderTexture(ResourceHolderSDL.imgSprite, rectSrc, rectDst);
			}

			if (engine.meterValueSub > Math.max(engine.meterValue, 0)) {
				int value = engine.meterValueSub;
				if(value > height * size * 4) value = height * size * 4;

				for(int i = 0; i < value; i++) {
					rectSrc = new SDLStructs.SDL_FRect(63 + (engine.meterColorSub * 4), 0, 4, 1);
					rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 8, y + (height * size * 4) + 3 - i, 4, 1);
					renderTexture(ResourceHolderSDL.imgSprite, rectSrc, rectDst);
				}
			}
			if (engine.meterValue > 0) {
				int value = engine.meterValue;
				if(value > height * size * 4) value = height * size * 4;

				for(int i = 0; i < value; i++) {
					rectSrc = new SDLStructs.SDL_FRect(63 + (engine.meterColor * 4), 0, 4, 1);
					rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 8, y + (height * size * 4) + 3 - i, 4, 1);
					renderTexture(ResourceHolderSDL.imgSprite, rectSrc, rectDst);
				}
			}
		} else {
			// MeterOFFWhen the upper right corner of the
			rectSrc = new SDLStructs.SDL_FRect(offsetX + 8, 0, 4, 4);
			rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 4, y, 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);

			// MeterOFFWhen the lower-right corner of
			rectSrc = new SDLStructs.SDL_FRect(offsetX + 8, 8, 4, 4);
			rectDst = new SDLStructs.SDL_FRect(x + (width * size * 4) + 4, y + (height * size * 4) + 4, 4, 4);
			renderTexture(ResourceHolderSDL.imgFrame, rectSrc, rectDst);
		}
	}

	/**
	 * NEXTDraw a
	 * @param x X-coordinate
	 * @param y Y-coordinate
	 * @param engine GameEngineInstance of
	 */
	protected void drawNext(int x, int y, GameEngine engine) {
		int fldWidth = 10;
		int fldBlkSize = 16;
		int meterWidth = showmeter ? 8 : 0;
		if(engine.field != null) {
			fldWidth = engine.field.getWidth();
			if(engine.displaysize == 1) fldBlkSize = 32;
		}

		// NEXT area background
		if(showbg && darknextarea) {
			if(getNextDisplayType() == 2) {
				int x2 = x + 8 + (fldWidth * fldBlkSize) + meterWidth;
				int maxNext = engine.isNextVisible ? engine.ruleopt.nextDisplay : 0;

				// HOLD area
				if(engine.ruleopt.holdEnable && engine.isHoldVisible) {
					drawDarkOverlay(x - 64, y + 48 + 8, 64, 64 - 16, 255);

					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						drawDarkOverlay(x - 64, y + 47 + i, 64, 1, alpha);
					}
					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						drawDarkOverlay(x - 64, y + 112 - i, 64, 1, alpha);
					}
				}

				// NEXT area
				if(maxNext > 0) {
					drawDarkOverlay(x2, y + 48 + 8, 64, (64 * maxNext) - 16, 255);

					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						drawDarkOverlay(x2, y + 47 + i, 64, 1, alpha);
					}
					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						drawDarkOverlay(x2, y + 48+(64*maxNext)-i, 64, 1, alpha);
					}
				}
			} else if(getNextDisplayType() == 1) {
				int x2 = x + 8 + (fldWidth * fldBlkSize) + meterWidth;
				int maxNext = engine.isNextVisible ? engine.ruleopt.nextDisplay : 0;

				// HOLD area
				if(engine.ruleopt.holdEnable && engine.isHoldVisible) {
					drawDarkOverlay(x - 32, y + 48 + 8, 32, 32 - 16, 255);

					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						drawDarkOverlay(x - 32, y + 47 + i, 32, 1, alpha);
					}
					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						drawDarkOverlay(x - 32, y + 80 - i, 32, 1, alpha);
					}
				}

				// NEXT area
				if(maxNext > 0) {
					drawDarkOverlay(x2, y + 48 + 8, 32, (32 * maxNext) - 16, 255);

					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						drawDarkOverlay(x2, y + 47 + i, 32, 1, alpha);
					}
					for(int i = 0; i <= 8; i++) {
						int alpha = (int)(((float)i / (float)8) * 255);
						drawDarkOverlay(x2, y + 48+(32*maxNext)-i, 32, 1, alpha);
					}
				}
			} else {
				int w = (fldWidth * fldBlkSize) + 15;

				drawDarkOverlay(x + 20, y, w - 40, 48, 255);

				for(int i = 0; i <= 20; i++) {
					int alpha = (int)(((float)i / (float)20) * 255);
					drawDarkOverlay(x + i - 1, y, 1, 48, alpha);
				}
				for(int i = 0; i <= 20; i++) {
					int alpha = (int)(((float)(20 - i) / (float)20) * 255);
					drawDarkOverlay(x + i + (w-20), y, 1, 48, alpha);
				}
			}
		}

		if(engine.isNextVisible) {
			if(getNextDisplayType() == 2) {
				if(engine.ruleopt.nextDisplay >= 1) {
					int x2 = x + 8 + (fldWidth * fldBlkSize) + meterWidth;
					NormalFontSDL.printFont(x2 + 16, y + 40, NullpoMinoSDL.getUIText("InGame_Next"), COLOR_ORANGE, 0.5f);
					String itemLabel = getItemLabel(engine.getNextObject(engine.nextPieceCount));
					if(itemLabel != null) NormalFontSDL.printFont(x2 + 64, y + 72, itemLabel, COLOR_WHITE, 0.5f);

					for(int i = 0; i < engine.ruleopt.nextDisplay; i++) {
						Piece piece = engine.getNextObject(engine.nextPieceCount + i);

						if(piece != null) {
							int centerX = ( (64 - ((piece.getWidth() + 1) * 16)) / 2 ) - (piece.getMinimumBlockX() * 16);
							int centerY = ( (64 - ((piece.getHeight() + 1) * 16)) / 2 ) - (piece.getMinimumBlockY() * 16);
							drawPiece(x2 + centerX, y + 48 + (i * 64) + centerY, piece, 1.0f);
						}
					}
				}
			} else if(getNextDisplayType() == 1) {
				if(engine.ruleopt.nextDisplay >= 1) {
					int x2 = x + 8 + (fldWidth * fldBlkSize) + meterWidth;
					NormalFontSDL.printFont(x2, y + 40, NullpoMinoSDL.getUIText("InGame_Next"), COLOR_ORANGE, 0.5f);
					String itemLabel = getItemLabel(engine.getNextObject(engine.nextPieceCount));
					if(itemLabel != null) NormalFontSDL.printFont(x2 + 32, y + 56, itemLabel, COLOR_WHITE, 0.5f);

					for(int i = 0; i < engine.ruleopt.nextDisplay; i++) {
						Piece piece = engine.getNextObject(engine.nextPieceCount + i);

						if(piece != null) {
							int centerX = ( (32 - ((piece.getWidth() + 1) * 8)) / 2 ) - (piece.getMinimumBlockX() * 8);
							int centerY = ( (32 - ((piece.getHeight() + 1) * 8)) / 2 ) - (piece.getMinimumBlockY() * 8);
							drawPiece(x2 + centerX, y + 48 + (i * 32) + centerY, piece, 0.5f);
						}
					}
				}
			} else {
				// NEXT1
				if(engine.ruleopt.nextDisplay >= 1) {
					Piece piece = engine.getNextObject(engine.nextPieceCount);
					NormalFontSDL.printFont(x + 60, y, NullpoMinoSDL.getUIText("InGame_Next"), COLOR_ORANGE, 0.5f);
					if(piece != null) {
						int x2 = x + 4 + engine.getSpawnPosX(engine.field, piece) * fldBlkSize;
						int y2 = y + 48 - ((piece.getMaximumBlockY() + 1) * 16);
						drawPiece(x2, y2, piece);

						String itemLabel = getItemLabel(piece);
						if(itemLabel != null) {
							int blockRight = x2 + (piece.getMaximumBlockX() + 1) * 16;
							int blockCenterY = y2 + ((piece.getMinimumBlockY() + piece.getMaximumBlockY() + 1) * 16) / 2;
							NormalFontSDL.printFont(blockRight + 4, blockCenterY - 8, itemLabel, COLOR_WHITE, 0.5f);
						}
					}
				}

				// NEXT2·3
				for(int i = 0; i < engine.ruleopt.nextDisplay - 1; i++) {
					if(i >= 2) break;

					Piece piece = engine.getNextObject(engine.nextPieceCount + i + 1);

					if(piece != null) {
						drawPiece(x + 124 + (i * 40), y + 48 - ((piece.getMaximumBlockY() + 1) * 8), piece, 0.5f);
					}
				}

				// NEXT4~
				for(int i = 0; i < engine.ruleopt.nextDisplay - 3; i++) {
					Piece piece = engine.getNextObject(engine.nextPieceCount + i + 3);

					if(piece != null) {
						if(showmeter)
							drawPiece(x + 176, y + (i * 40) + 88 - ((piece.getMaximumBlockY() + 1) * 8), piece, 0.5f);
						else
							drawPiece(x + 168, y + (i * 40) + 88 - ((piece.getMaximumBlockY() + 1) * 8), piece, 0.5f);
					}
				}
			}
		}

		if(engine.isHoldVisible) {
			// HOLD
			int holdRemain = engine.ruleopt.holdLimit - engine.holdUsedCount;
			int x2 = sidenext ? (x - 32) : x;
			int y2 = sidenext ? (y + 40) : y;
			if(getNextDisplayType() == 2) x2 = x - 48;

			if( (engine.ruleopt.holdEnable == true) && ((engine.ruleopt.holdLimit < 0) || (holdRemain > 0)) ) {
				int tempColor = COLOR_GREEN;
				if(engine.holdDisable == true) tempColor = COLOR_WHITE;

				if(engine.ruleopt.holdLimit < 0) {
					NormalFontSDL.printFont(x2, y2, NullpoMinoSDL.getUIText("InGame_Hold"), tempColor, 0.5f);
				} else {
					if(!engine.holdDisable) {
						if(holdRemain <= 10) tempColor = COLOR_YELLOW;
						if(holdRemain <= 5) tempColor = COLOR_RED;
					}

					NormalFontSDL.printFont(x2, y2, NullpoMinoSDL.getUIText("InGame_Hold") + "\ne " + holdRemain, tempColor, 0.5f);
				}

				if(engine.holdPieceObject != null) {
					float dark = 0f;
					if(engine.holdDisable == true) dark = 0.3f;
					Piece piece = new Piece(engine.holdPieceObject);
					piece.resetOffsetArray();

					if(getNextDisplayType() == 2) {
						int centerX = ( (64 - ((piece.getWidth() + 1) * 16)) / 2 ) - (piece.getMinimumBlockX() * 16);
						int centerY = ( (64 - ((piece.getHeight() + 1) * 16)) / 2 ) - (piece.getMinimumBlockY() * 16);
						drawPiece((x - 64) + centerX, y + 48 + centerY, piece, 1.0f, dark);
					} else if(getNextDisplayType() == 1) {
						int centerX = ( (32 - ((piece.getWidth() + 1) * 8)) / 2 ) - (piece.getMinimumBlockX() * 8);
						int centerY = ( (32 - ((piece.getHeight() + 1) * 8)) / 2 ) - (piece.getMinimumBlockY() * 8);
						drawPiece(x2 + centerX, y + 48 + centerY, piece, 0.5f, dark);
					} else {
						drawPiece(x2, y + 48 - ((piece.getMaximumBlockY() + 1) * 8), piece, 0.5f, dark);
					}
				}
			}
		}
	}

	/**
	 * Draw shadow nexts
	 * @param x X coord
	 * @param y Y coord
	 * @param engine GameEngine
	 * @param scale Display size of piece
	 * @author Wojtek
	 */
	protected void drawShadowNexts(int x, int y, GameEngine engine, float scale) {
		Piece piece = engine.nowPieceObject;
		int blksize = (int) (16 * scale);

		if (piece != null) {
			int shadowX = engine.nowPieceX;
			int shadowY = engine.nowPieceBottomY + piece.getMinimumBlockY();

			for (int i = 0; i < engine.ruleopt.nextDisplay - 1; i++) {
				if (i >= 3)
					break;

				Piece next = engine.getNextObject(engine.nextPieceCount + i);

				if (next != null) {
					int size = ((piece.big || engine.displaysize == 1) ? 2 : 1);
					int shadowCenter = blksize * piece.getMinimumBlockX() + blksize
							* (piece.getWidth() + size) / 2;
					int nextCenter = blksize / 2 * next.getMinimumBlockX() + blksize / 2
							* (next.getWidth() + 1) / 2;
					int vPos = blksize * shadowY - (i + 1) * 24 - 8;

					if (vPos >= -blksize / 2)
						drawPiece(x + blksize * shadowX + shadowCenter - nextCenter, y
								+ vPos, next, 0.5f * scale, 0.1f);
				}
			}
		}
	}

	/*
	 * Each frame Drawing process of the first
	 */
	@Override
	public void renderFirst(GameEngine engine, int playerID) {
		// Background
		if(playerID == 0) {
			if(engine.owner.menuOnly) {
				renderTextureFullscreen(ResourceHolderSDL.imgMenu);
			} else {
				int bg = engine.owner.backgroundStatus.bg;
				if(engine.owner.backgroundStatus.fadesw && !heavyeffect) {
					bg = engine.owner.backgroundStatus.fadebg;
				}

				if((ResourceHolderSDL.imgPlayBG != null) && (bg >= 0) && (bg < ResourceHolderSDL.imgPlayBG.length) && (showbg == true)) {
					renderTextureFullscreen(ResourceHolderSDL.imgPlayBG[bg]);

					if(engine.owner.backgroundStatus.fadesw && heavyeffect) {
						int alphalv = engine.owner.backgroundStatus.fadestat ? (100 - engine.owner.backgroundStatus.fadecount) : engine.owner.backgroundStatus.fadecount;
						drawDarkOverlay(0, 0, 640, 480, alphalv * 2);
					}
				} else if(bg != -2) {
					SDL3.setDrawColor(renderer(), 0, 0, 0, 255);
					SDL3.INSTANCE.SDL_RenderFillRect(renderer(), null);
				}
			}
		}

		// NEXTなど
		if(!engine.owner.menuOnly && engine.isVisible) {
			int offsetX = getFieldDisplayPositionX(engine, playerID);
			int offsetY = getFieldDisplayPositionY(engine, playerID);

			if(engine.displaysize != -1) {
				drawNext(offsetX, offsetY, engine);
				drawFrame(offsetX, offsetY + 48, engine, engine.displaysize);
				drawField(offsetX + 4, offsetY + 52, engine, engine.displaysize);
			} else {
				drawFrame(offsetX, offsetY, engine, -1);
				drawField(offsetX + 4, offsetY + 4, engine, -1);
			}
		}
	}

	/*
	 * ReadyProcess of drawing the screen
	 */
	@Override
	public void renderReady(GameEngine engine, int playerID) {
		if(engine.allowTextRenderByReceiver == false) return;

		int offsetX = getFieldDisplayPositionX(engine, playerID);
		int offsetY = getFieldDisplayPositionY(engine, playerID);

		if(engine.statc[0] > 0) {
			if(engine.displaysize != -1) {
				if((engine.statc[0] >= engine.readyStart) && (engine.statc[0] < engine.readyEnd))
					NormalFontSDL.printFont(offsetX + 44, offsetY + 204, "READY", COLOR_WHITE, 1.0f);
				else if((engine.statc[0] >= engine.goStart) && (engine.statc[0] < engine.goEnd))
					NormalFontSDL.printFont(offsetX + 62, offsetY + 204, "GO!", COLOR_WHITE, 1.0f);
			} else {
				if((engine.statc[0] >= engine.readyStart) && (engine.statc[0] < engine.readyEnd))
					NormalFontSDL.printFont(offsetX + 24, offsetY + 80, "READY", COLOR_WHITE, 0.5f);
				else if((engine.statc[0] >= engine.goStart) && (engine.statc[0] < engine.goEnd))
					NormalFontSDL.printFont(offsetX + 32, offsetY + 80, "GO!", COLOR_WHITE, 0.5f);
			}
		}
	}

	/*
	 * BlockHandling when moving piece
	 */
	@Override
	public void renderMove(GameEngine engine, int playerID) {
		if(!engine.isVisible) return;

		int offsetX = getFieldDisplayPositionX(engine, playerID);
		int offsetY = getFieldDisplayPositionY(engine, playerID);

		if((engine.statc[0] > 1) || (engine.ruleopt.moveFirstFrame)) {
			if(engine.displaysize == 1) {
				if(nextshadow) drawShadowNexts(offsetX + 4, offsetY + 52, engine, 2.0f);
				if(engine.ghost && engine.ruleopt.ghost) drawGhostPiece(offsetX + 4, offsetY + 52, engine, 2.0f);
				if((engine.ai!=null) && (engine.aiShowHint)&& engine.aiHintReady) drawHintPiece(offsetX + 4, offsetY + 52, engine, 2.0f);
				drawCurrentPiece(offsetX + 4, offsetY + 52, engine, 2.0f);
			} else if(engine.displaysize == 0) {
				if(nextshadow) drawShadowNexts(offsetX + 4, offsetY + 52, engine, 1.0f);
				if(engine.ghost && engine.ruleopt.ghost) drawGhostPiece(offsetX + 4, offsetY + 52, engine, 1.0f);
				if((engine.ai!=null) && (engine.aiShowHint ) && engine.aiHintReady) drawHintPiece(offsetX + 4, offsetY + 52, engine, 1.0f);
				drawCurrentPiece(offsetX + 4, offsetY + 52, engine, 1.0f);
			} else {
				if(engine.ghost && engine.ruleopt.ghost) drawGhostPiece(offsetX + 4, offsetY + 4, engine, 0.5f);
				if((engine.ai!=null) && (engine.aiShowHint) &&engine.aiHintReady) drawHintPiece(offsetX + 4, offsetY + 4, engine, 0.5f);
				drawCurrentPiece(offsetX + 4, offsetY + 4, engine, 0.5f);
			}
		}
	}

	/*
	 * BlockWhen you issue the production process to turn off the
	 */
	@Override
	public void blockBreak(GameEngine engine, int playerID, int x, int y, Block blk) {
		if(showlineeffect && (blk != null) && engine.displaysize != -1) {
			int color = blk.getDrawColor();
			// UsuallyBlock
			if((color >= Block.BLOCK_COLOR_GRAY) && (color <= Block.BLOCK_COLOR_PURPLE) && !blk.getAttribute(Block.BLOCK_ATTRIBUTE_BONE)) {
				EffectObject obj =
					new EffectObject(1,
										getFieldDisplayPositionX(engine, playerID) + 4 + (x * 16),
										getFieldDisplayPositionY(engine, playerID) + 52 + (y * 16),
										color);
				effectlist.add(obj);
			}
			// JewelBlock
			else if(blk.isGemBlock()) {
				EffectObject obj =
					new EffectObject(2,
										getFieldDisplayPositionX(engine, playerID) + 4 + (x * 16),
										getFieldDisplayPositionY(engine, playerID) + 52 + (y * 16),
										color);
				effectlist.add(obj);
			}
		}
	}

	/*
	 * EXCELLENTProcess of drawing the screen
	 */
	@Override
	public void renderExcellent(GameEngine engine, int playerID) {
		if(engine.allowTextRenderByReceiver == false) return;
		if(!engine.isVisible) return;

		int offsetX = getFieldDisplayPositionX(engine, playerID);
		int offsetY = getFieldDisplayPositionY(engine, playerID);

		if(engine.displaysize != -1) {
			if(engine.statc[1] == 0)
				NormalFontSDL.printFont(offsetX + 4, offsetY + 204, "EXCELLENT!", COLOR_ORANGE, 1.0f);
			else if(engine.owner.getPlayers() < 3)
				NormalFontSDL.printFont(offsetX + 52, offsetY + 204, "WIN!", COLOR_ORANGE, 1.0f);
			else
				NormalFontSDL.printFont(offsetX + 4, offsetY + 204, "1ST PLACE!", COLOR_ORANGE, 1.0f);
		} else {
			if(engine.statc[1] == 0)
				NormalFontSDL.printFont(offsetX + 4, offsetY + 80, "EXCELLENT!", COLOR_ORANGE, 0.5f);
			else if(engine.owner.getPlayers() < 3)
				NormalFontSDL.printFont(offsetX + 33, offsetY + 80, "WIN!", COLOR_ORANGE, 0.5f);
			else
				NormalFontSDL.printFont(offsetX + 4, offsetY + 80, "1ST PLACE!", COLOR_ORANGE, 0.5f);
		}
	}

	/*
	 * game overProcess of drawing the screen
	 */
	@Override
	public void renderGameOver(GameEngine engine, int playerID) {
		if(engine.allowTextRenderByReceiver == false) return;
		if(!engine.isVisible) return;

		if((engine.statc[0] >= engine.field.getHeight() + 1) && (engine.statc[0] < engine.field.getHeight() + 1 + 180)) {
			int offsetX = getFieldDisplayPositionX(engine, playerID);
			int offsetY = getFieldDisplayPositionY(engine, playerID);

			if(engine.displaysize != -1) {
				if(engine.owner.getPlayers() < 2)
					NormalFontSDL.printFont(offsetX + 12, offsetY + 204, "GAME OVER", COLOR_WHITE, 1.0f);
				else if(engine.owner.getWinner() == -2)
					NormalFontSDL.printFont(offsetX + 52, offsetY + 204, "DRAW", COLOR_GREEN, 1.0f);
				else if(engine.owner.getPlayers() < 3)
					NormalFontSDL.printFont(offsetX + 52, offsetY + 204, "LOSE", COLOR_WHITE, 1.0f);
			} else {
				if(engine.owner.getPlayers() < 2)
					NormalFontSDL.printFont(offsetX + 4, offsetY + 80, "GAME OVER", COLOR_WHITE, 0.5f);
				else if(engine.owner.getWinner() == -2)
					NormalFontSDL.printFont(offsetX + 28, offsetY + 80, "DRAW", COLOR_GREEN, 0.5f);
				else if(engine.owner.getPlayers() < 3)
					NormalFontSDL.printFont(offsetX + 28, offsetY + 80, "LOSE", COLOR_WHITE, 0.5f);
			}
		}
	}

	/*
	 * Render results screenProcessing
	 */
	@Override
	public void renderResult(GameEngine engine, int playerID) {
		if(engine.allowTextRenderByReceiver == false) return;
		if(!engine.isVisible) return;

		int offsetX = getFieldDisplayPositionX(engine, playerID);
		int offsetY = getFieldDisplayPositionY(engine, playerID);

		int tempColor;

		if(engine.statc[0] == 0)
			tempColor = COLOR_RED;
		else
			tempColor = COLOR_WHITE;
		NormalFontSDL.printFont(offsetX + 12, offsetY + 340, "RETRY", tempColor, 1.0f);

		if(engine.statc[0] == 1)
			tempColor = COLOR_RED;
		else
			tempColor = COLOR_WHITE;
		NormalFontSDL.printFont(offsetX + 108, offsetY + 340, "END", tempColor, 1.0f);
	}

	/*
	 * fieldDrawing process of edit screen
	 */
	@Override
	public void renderFieldEdit(GameEngine engine, int playerID) {
		int x = getFieldDisplayPositionX(engine, playerID) + 4 + (engine.fldeditX * 16);
		int y = getFieldDisplayPositionY(engine, playerID) + 52 + (engine.fldeditY * 16);
		float bright = (engine.fldeditFrames % 60 >= 30) ? -0.5f : -0.2f;
		drawBlock(x, y, engine.fldeditColor, engine.getSkin(), false, bright, 1.0f, 1.0f);
	}

	/*
	 * Each frame Processing that takes place at the end of the
	 */
	@Override
	public void onLast(GameEngine engine, int playerID) {
		if(playerID == engine.owner.getPlayers() - 1) effectUpdate();
	}

	/*
	 * Each frame Drawing process that takes place at the end of the
	 */
	@Override
	public void renderLast(GameEngine engine, int playerID) {
		if(playerID == engine.owner.getPlayers() - 1) effectRender();
	}

	/**
	 * Update effects
	 */
	protected void effectUpdate() {
		boolean emptyflag = true;

		for(int i = 0; i < effectlist.size(); i++) {
			EffectObject obj = effectlist.get(i);

			if(obj.effect != 0) emptyflag = false;

			// Normal Block
			if(obj.effect == 1) {
				obj.anim += (lineeffectspeed + 1);
				if(obj.anim >= 36) obj.effect = 0;
			}
			// Gem Block
			if(obj.effect == 2) {
				obj.anim += (lineeffectspeed + 1);
				if(obj.anim >= 60) obj.effect = 0;
			}
		}

		if(emptyflag) effectlist.clear();
	}

	/**
	 * Render effects
	 */
	protected void effectRender() {
		for(int i = 0; i < effectlist.size(); i++) {
			EffectObject obj = effectlist.get(i);

			// Normal Block
			if(obj.effect == 1) {
				int x = obj.x - 40;
				int y = obj.y - 15;
				int color = obj.param - Block.BLOCK_COLOR_GRAY;

				int srcx = ((obj.anim-1) % 6) * 96;
				int srcy = ((obj.anim-1) / 6) * 96;
				if(obj.anim >= 30) {
					srcx = ((obj.anim-30) % 6) * 96;
					srcy = ((obj.anim-30) / 6) * 96;
				}

				SDLStructs.SDL_FRect rectSrc = new SDLStructs.SDL_FRect(srcx, srcy, 96, 96);
				SDLStructs.SDL_FRect rectDst = new SDLStructs.SDL_FRect(x, y, 96, 96);

				if(ResourceHolderSDL.imgBreak != null) {
					if(obj.anim < 30) {
						renderTexture(ResourceHolderSDL.imgBreak[color][0], rectSrc, rectDst);
					} else {
						renderTexture(ResourceHolderSDL.imgBreak[color][1], rectSrc, rectDst);
					}
				}
			}
			// Gem Block
			if(obj.effect == 2) {
				int x = obj.x - 8;
				int y = obj.y - 8;
				int srcx = ((obj.anim-1) % 10) * 32;
				int srcy = ((obj.anim-1) / 10) * 32;
				int color = obj.param - Block.BLOCK_COLOR_GEM_RED;

				SDLStructs.SDL_FRect rectSrc = new SDLStructs.SDL_FRect(srcx, srcy, 32, 32);
				SDLStructs.SDL_FRect rectDst = new SDLStructs.SDL_FRect(x, y, 32, 32);

				if(ResourceHolderSDL.imgPErase != null) {
					renderTexture(ResourceHolderSDL.imgPErase[color], rectSrc, rectDst);
				}
			}
		}
	}
}
