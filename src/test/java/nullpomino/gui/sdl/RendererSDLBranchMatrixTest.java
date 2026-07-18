package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.mode.GameMode;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.EffectObject;
import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.util.CustomProperties;

/**
 * Headless branch matrix for the SDL renderer.  The fake backend records the
 * draw calls while deliberately accepting null/marker texture handles, which
 * lets the renderer's layout and state decisions be tested without native SDL.
 */
class RendererSDLBranchMatrixTest {
	private static final AtomicInteger SDL_CALLS = new AtomicInteger();
	private static final AtomicInteger TEXTURE_SIZE = new AtomicInteger(1024);
	private static final SdlTexture TEXTURE = new SdlTexture() {};
	private static final SdlRenderer RENDERER = new SdlRenderer() {};

	private Harness renderer;

	@BeforeAll
	static void installBackend() {
		SdlBackend.set(new SdlBackend.Backend() {
			@Override public SDL3 sdl3() { return sdlStub(); }
			@Override public SDL3Image image() { return stub(SDL3Image.class); }
			@Override public SDL3TTF ttf() { return stub(SDL3TTF.class); }
			@Override public SDL3Mixer mixerOrNull() { return null; }
		});
	}

	@BeforeEach
	void setUp() {
		SDL_CALLS.set(0);
		TEXTURE_SIZE.set(1024);
		NullpoMinoSDL.renderer = RENDERER;
		NullpoMinoSDL.propConfig = new CustomProperties();
		NullpoMinoSDL.propGlobal = new CustomProperties();
		NullpoMinoSDL.propLang = new CustomProperties();
		NullpoMinoSDL.propLangDefault = new CustomProperties();

		ResourceHolderSDL.imgNormalBlockList = textures(2);
		ResourceHolderSDL.imgSmallBlockList = textures(2);
		ResourceHolderSDL.imgBigBlockList = textures(2);
		ResourceHolderSDL.blockStickyFlagList = new LinkedList<Boolean>(Arrays.asList(false, true));
		ResourceHolderSDL.imgFont = TEXTURE;
		ResourceHolderSDL.imgFontSmall = TEXTURE;
		ResourceHolderSDL.imgFontBig = TEXTURE;
		ResourceHolderSDL.imgSprite = TEXTURE;
		ResourceHolderSDL.imgMenu = TEXTURE;
		ResourceHolderSDL.imgFrame = TEXTURE;
		ResourceHolderSDL.imgFieldbg = TEXTURE;
		ResourceHolderSDL.imgFieldbg2 = TEXTURE;
		ResourceHolderSDL.imgFieldbg2Small = TEXTURE;
		ResourceHolderSDL.imgFieldbg2Big = TEXTURE;
		ResourceHolderSDL.imgPlayBG = new SdlTexture[] {TEXTURE, TEXTURE};
		ResourceHolderSDL.imgBreak = new SdlTexture[8][2];
		for(SdlTexture[] row : ResourceHolderSDL.imgBreak) Arrays.fill(row, TEXTURE);
		ResourceHolderSDL.imgPErase = new SdlTexture[ResourceHolderSDL.PERASE_MAX];
		Arrays.fill(ResourceHolderSDL.imgPErase, TEXTURE);
		ResourceHolderSDL.ttfFont = null;
		ResourceHolderSDL.soundManager = new SoundManagerSDL();

		renderer = new Harness();
	}

	@Test
	void fontSpeedBlockAndPieceMatricesRenderHeadlessly() {
		GameEngine engine = engine(1);
		GameKeySDL.initGlobalGameKeySDL();
		GameKeySDL.gamekey[0].keymapNav[GameKeySDL.BUTTON_A] = SDLConstants.SDL_SCANCODE_COUNT;
		assertEquals("", renderer.getKeyNameByButtonID(engine, GameKeySDL.BUTTON_A));
		renderer.playSE("not-loaded");

		engine.owner.menuOnly = true;
		renderer.drawMenuFont(engine, 0, 1, 2, "A", 0, 0.5f);
		renderer.drawTTFMenuFont(engine, 0, 1, 2, "A", 0);
		renderer.drawScoreFont(engine, 0, 1, 2, "A", 0, 1f);
		renderer.drawTTFScoreFont(engine, 0, 1, 2, "A", 0);
		renderer.drawSpeedMeter(engine, 0, 0, 0, 20);

		engine.owner.menuOnly = false;
		for(int display : new int[] {-1, 0}) {
			engine.displaysize = display;
			renderer.drawMenuFont(engine, 0, 1, 2, "A", 0, display == -1 ? 0.5f : 1f);
			renderer.drawTTFMenuFont(engine, 0, 1, 2, "A", 0);
		}
		renderer.drawScoreFont(engine, 0, 1, 2, "A", 0, 0.5f);
		renderer.drawScoreFont(engine, 0, 1, 2, "A", 0, 1f);
		renderer.drawTTFScoreFont(engine, 0, 1, 2, "A", 0);
		renderer.drawDirectFont(engine, 0, 0, 0, "A", 0, 1f);
		renderer.drawTTFDirectFont(engine, 0, 0, 0, "A", 0);
		for(int speed : new int[] {-1, 0, 20, 41}) renderer.drawSpeedMeter(engine, 0, 0, 0, speed);

		int allConnections = Block.BLOCK_ATTRIBUTE_CONNECT_UP | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN |
				Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT;
		renderer.drawBlock(0, 0, Block.BLOCK_COLOR_INVALID, 0, false, 0f, 1f, 1f, 0);
		renderer.drawBlock(0, 0, Block.BLOCK_COLOR_RED, 99, false, 0f, 1f, 0.5f, 0);
		renderer.drawBlock(0, 0, Block.BLOCK_COLOR_RED, 0, true, 0.25f, 0.5f, 2f, 0);
		renderer.drawBlock(0, 0, Block.BLOCK_COLOR_RED, 1, false, -0.25f, 1f, 1f, allConnections);
		renderer.drawBlock(0, 0, Block.BLOCK_COLOR_RED, 1, true, 0f, 1f, 1f,
				Block.BLOCK_ATTRIBUTE_CONNECT_UP);
		renderer.drawBlock(0, 0, Block.BLOCK_COLOR_RED, 1, false, 0f, 1f, 1f,
				Block.BLOCK_ATTRIBUTE_CONNECT_DOWN);
		renderer.drawBlock(0, 0, Block.BLOCK_COLOR_GEM_RED, 1, false, 0f, 1f, 1f, 0);
		TEXTURE_SIZE.set(0);
		renderer.drawBlock(0, 0, Block.BLOCK_COLOR_RED, 0, false, 0f, 1f, 1f, 0);
		TEXTURE_SIZE.set(1024);
		ResourceHolderSDL.imgNormalBlockList.set(0, null);
		renderer.drawBlock(0, 0, Block.BLOCK_COLOR_RED, 0, false, 0f, 1f, 1f, 0);
		ResourceHolderSDL.imgNormalBlockList.set(0, TEXTURE);

		Block block = new Block(Block.BLOCK_COLOR_BLUE, 0);
		block.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		renderer.drawBlock(0, 0, block);
		renderer.drawBlock(0, 0, block, 0.5f);
		renderer.drawBlock(0, 0, block, 2f, -0.2f);
		renderer.drawBlockForceVisible(0, 0, block, 1f);
		block.item = Block.BLOCK_ITEM_FREE_FALL;
		renderer.drawBlock(0, 0, block);
		block.item = Block.BLOCK_ITEM_DEL_EVEN;
		renderer.drawBlock(0, 0, block, 0.5f);
		assertEquals(null, RendererSDL.getItemLabel(null));
		Piece blockless = piece(Piece.PIECE_O);
		blockless.block = null;
		assertEquals(null, RendererSDL.getItemLabel(blockless));

		Piece piece = piece(Piece.PIECE_T);
		renderer.drawPiece(0, 0, piece);
		renderer.drawPiece(0, 0, piece, 0.5f);
		renderer.drawPiece(0, 0, piece, 2f, 0.2f);

		engine.nowPieceObject = null;
		renderer.drawCurrentPiece(0, 0, engine, 1f);
		engine.nowPieceObject = piece;
		engine.nowPieceX = 3;
		engine.nowPieceY = 0;
		engine.nowPieceColorOverride = -1;
		renderer.drawCurrentPiece(0, 0, engine, 1f);
		engine.nowPieceColorOverride = Block.BLOCK_COLOR_GREEN;
		renderer.drawCurrentPiece(0, 0, engine, 1f);
		engine.nowPieceY = -10;
		renderer.drawCurrentPiece(0, 0, engine, 1f);
		engine.nowPieceY = 0;
		piece.big = true;
		renderer.drawCurrentPiece(0, 0, engine, 1f);
		engine.nowPieceColorOverride = -1;
		renderer.drawCurrentPiece(0, 0, engine, 1f);

		assertTrue(SDL_CALLS.get() > 0);
	}

	@Test
	void ghostHintFieldAndFrameMatricesRenderAllDisplaySizes() {
		GameEngine engine = engine(1);
		Piece piece = piece(Piece.PIECE_L);
		engine.nowPieceObject = piece;
		engine.nowPieceX = 3;
		engine.nowPieceY = 0;
		engine.nowPieceBottomY = 10;
		engine.nowPieceColorOverride = Block.BLOCK_COLOR_CYAN;

		renderer.setOutlineGhost(false);
		renderer.drawGhostPiece(0, 0, engine, 1f);
		renderer.setOutlineGhost(true);
		renderer.drawGhostPiece(0, 0, engine, 1f);
		engine.nowPieceBottomY = -10;
		renderer.drawGhostPiece(0, 0, engine, 1f);
		engine.nowPieceBottomY = 10;
		piece.setAttribute(Block.BLOCK_ATTRIBUTE_BONE, true);
		piece.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN |
				Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
		renderer.drawGhostPiece(0, 0, engine, 1f);
		piece.big = true;
		renderer.drawGhostPiece(0, 0, engine, 1f);
		piece.setAttribute(Block.BLOCK_ATTRIBUTE_BONE | Block.BLOCK_ATTRIBUTE_CONNECT_UP |
				Block.BLOCK_ATTRIBUTE_CONNECT_DOWN | Block.BLOCK_ATTRIBUTE_CONNECT_LEFT |
				Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, false);
		renderer.drawGhostPiece(0, 0, engine, 1f);
		renderer.setOutlineGhost(false);
		engine.nowPieceColorOverride = Block.BLOCK_COLOR_GREEN;
		renderer.drawGhostPiece(0, 0, engine, 1f);
		engine.nowPieceColorOverride = -1;
		renderer.drawGhostPiece(0, 0, engine, 1f);
		engine.nowPieceObject = null;
		renderer.drawGhostPiece(0, 0, engine, 1f);
		engine.nowPieceObject = piece;

		engine.ai = new DummyAI();
		engine.ai.bestX = 3;
		engine.ai.bestY = 5;
		engine.ai.bestRt = Piece.DIRECTION_RIGHT;
		engine.aiHintPiece = piece(Piece.PIECE_J);
		renderer.drawHintPiece(0, 0, engine, 1f);
		engine.ai.bestY = -10;
		renderer.drawHintPiece(0, 0, engine, 1f);
		engine.ai.bestY = 5;
		engine.aiHintPiece.setAttribute(Block.BLOCK_ATTRIBUTE_BONE, true);
		engine.aiHintPiece.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN |
				Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
		renderer.drawHintPiece(0, 0, engine, 1f);
		engine.aiHintPiece.big = true;
		renderer.drawHintPiece(0, 0, engine, 1f);
		engine.aiHintPiece.setAttribute(Block.BLOCK_ATTRIBUTE_BONE, false);
		renderer.drawHintPiece(0, 0, engine, 1f);
		engine.aiHintPiece = null;
		renderer.drawHintPiece(0, 0, engine, 1f);

		engine.field = decoratedField(2, 2);
		for(int outline : new int[] {GameEngine.BLOCK_OUTLINE_NORMAL, GameEngine.BLOCK_OUTLINE_CONNECT,
				GameEngine.BLOCK_OUTLINE_SAMECOLOR}) {
			engine.blockOutlineType = outline;
			for(int display : new int[] {-1, 0, 1}) renderer.drawField(0, 0, engine, display);
		}
		engine.owBlockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT;
		engine.owner.replayMode = true;
		engine.owner.replayShowInvisible = true;
		renderer.showfieldbggrid = false;
		renderer.drawField(0, 0, engine, 0);
		engine.owner.replayShowInvisible = false;
		renderer.drawField(0, 0, engine, 0);
		engine.heboHiddenEnable = true;
		engine.gameActive = false;
		renderer.drawField(0, 0, engine, 0);
		engine.gameActive = true;
		engine.heboHiddenYNow = 99;
		renderer.drawField(0, 0, engine, 0);
		engine.heboHiddenYNow = 1;
		renderer.drawField(0, 0, engine, 0);
		engine.field = null;
		renderer.drawField(0, 0, engine, 0);
		engine.heboHiddenEnable = false;
		engine.gameActive = false;
		engine.heboHiddenYNow = 0;
		engine.field = new Field(11, 21, 0);
		engine.field.setBlockE(0, 0, new Block(Block.BLOCK_COLOR_BLUE));
		renderer.showfieldbggrid = true;
		renderer.drawField(0, 0, engine, 0);
		engine.field = new Field(11, 2, 0);
		engine.field.setBlockE(0, 0, new Block(Block.BLOCK_COLOR_BLUE));
		renderer.drawField(0, 0, engine, 0);
		renderer.showfieldbggrid = false;
		renderer.drawField(0, 0, engine, 0);
		renderer.showfieldbggrid = true;

		Field parity = new Field(2, 2, 0);
		for(int y = 0; y < 2; y++) for(int x = 0; x < 2; x++) {
			Block invisible = new Block(Block.BLOCK_COLOR_BLUE);
			invisible.alpha = (x == 0 && y == 0) ? 0.5f : 1f;
			if(x == 0 && y == 0) invisible.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
			parity.setBlockE(x, y, invisible);
		}
		engine.field = parity;
		renderer.drawField(0, 0, engine, 0);
		renderer.showfieldbggrid = false;
		renderer.drawField(0, 0, engine, 0);
		renderer.showfieldbggrid = true;
		renderer.fieldbgbright = 0;
		parity.getBlock(0, 0).setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false);
		renderer.drawField(0, 0, engine, 0);
		engine.field = new Field(1, 1, 0);
		renderer.drawField(0, 0, engine, 0);
		renderer.fieldbgbright = 128;

		Field surrounded = new Field(3, 3, 0);
		for(int y = 0; y < 3; y++) for(int x = 0; x < 3; x++) {
			Block red = new Block(Block.BLOCK_COLOR_RED);
			red.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
			red.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP | Block.BLOCK_ATTRIBUTE_CONNECT_DOWN |
					Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
			surrounded.setBlockE(x, y, red);
		}
		surrounded.getBlock(1, 1).setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true);
		engine.field = surrounded;
		engine.owBlockOutlineType = -1;
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL;
		renderer.drawField(0, 0, engine, 0);
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT;
		renderer.drawField(0, 0, engine, 0);
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR;
		renderer.drawField(0, 0, engine, 0);
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE;
		renderer.drawField(0, 0, engine, 0);

		Field isolated = new Field(3, 3, 0);
		Block center = new Block(Block.BLOCK_COLOR_RED);
		center.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_OUTLINE, true);
		isolated.setBlockE(1, 1, center);
		engine.field = isolated;
		engine.owBlockOutlineType = -1;
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL;
		renderer.drawField(0, 0, engine, 0);
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_CONNECT;
		renderer.drawField(0, 0, engine, 0);
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR;
		renderer.drawField(0, 0, engine, 0);

		engine.field = new Field(2, 2, 0);
		renderer.fieldbgbright = 128;
		for(int display : new int[] {-1, 0, 1}) renderer.drawFrame(0, 0, engine, display);
		engine.meterValue = 99;
		engine.meterValueSub = 100;
		renderer.drawFrame(0, 0, engine, 0);
		engine.meterValue = 100;
		engine.meterValueSub = 1;
		renderer.drawFrame(0, 0, engine, 0);
		engine.meterValue = 1;
		engine.meterValueSub = 2;
		renderer.drawFrame(0, 0, engine, 0);
		engine.meterValue = 1;
		engine.meterValueSub = 0;
		renderer.drawFrame(0, 0, engine, 0);
		engine.meterValue = 0;
		engine.field = new Field(11, 2, 0);
		renderer.drawFrame(0, 0, engine, 0);
		engine.field = new Field(2, 21, 0);
		renderer.drawFrame(0, 0, engine, 0);
		engine.field = new Field(2, 2, 0);
		renderer.showfieldbggrid = false;
		renderer.drawFrame(0, 0, engine, 0);
		renderer.showfieldbggrid = true;
		renderer.setShowMeter(false);
		renderer.drawFrame(0, 0, engine, 0);
		renderer.fieldbgbright = 0;
		renderer.drawFrame(0, 0, engine, 0);
		engine.field = null;
		renderer.drawFrame(0, 0, engine, 0);

		GameEngine multiplayer = engine(2);
		multiplayer.field = new Field(1, 1, 0);
		renderer.fieldbgbright = 128;
		renderer.drawField(0, 0, multiplayer, 0);
		renderer.drawFrame(0, 0, multiplayer, 0);

		assertTrue(SDL_CALLS.get() > 100);
	}

	@Test
	void nextHoldShadowAndRenderHookMatrices() {
		GameEngine engine = engine(1);
		engine.field = new Field(10, 20, 0);
		engine.nextPieceArrayObject = new Piece[] {
				piece(Piece.PIECE_T), null, piece(Piece.PIECE_I), piece(Piece.PIECE_O), piece(Piece.PIECE_J)
		};
		engine.nextPieceArrayObject[0].block[0].item = Block.BLOCK_ITEM_FREE_FALL;
		engine.ruleopt.nextDisplay = 5;
		engine.isNextVisible = true;
		engine.isHoldVisible = true;
		engine.ruleopt.holdEnable = true;
		engine.ruleopt.holdLimit = -1;
		engine.holdPieceObject = piece(Piece.PIECE_L);
		engine.nowPieceObject = piece(Piece.PIECE_S);
		engine.nowPieceX = 3;
		engine.nowPieceBottomY = 10;

		renderer.setSideNext(false, false);
		renderer.drawNext(0, 0, engine);
		renderer.setSideNext(true, false);
		renderer.drawNext(0, 0, engine);
		renderer.setSideNext(true, true);
		renderer.drawNext(0, 0, engine);
		engine.displaysize = 1;
		renderer.drawNext(0, 0, engine);
		engine.displaysize = 0;
		renderer.setShowMeter(false);
		renderer.drawNext(0, 0, engine);
		renderer.setShowMeter(true);
		renderer.setDarkNextArea(false);
		renderer.drawNext(0, 0, engine);
		renderer.setDarkNextArea(true);
		Field nextField = engine.field;
		engine.field = null;
		renderer.drawNext(0, 0, engine);
		engine.field = nextField;
		engine.isNextVisible = false;
		renderer.drawNext(0, 0, engine);
		engine.isNextVisible = true;
		engine.ruleopt.holdEnable = false;
		renderer.drawNext(0, 0, engine);
		engine.ruleopt.holdEnable = true;
		engine.isHoldVisible = false;
		renderer.drawNext(0, 0, engine);
		engine.isHoldVisible = true;

		renderer.setSideNext(true, false);
		engine.isNextVisible = false;
		renderer.drawNext(0, 0, engine);
		engine.isNextVisible = true;
		engine.isHoldVisible = false;
		renderer.drawNext(0, 0, engine);
		engine.isHoldVisible = true;
		engine.ruleopt.holdEnable = false;
		renderer.drawNext(0, 0, engine);
		engine.ruleopt.holdEnable = true;

		Piece firstNext = engine.nextPieceArrayObject[0];
		firstNext.block[0].item = Block.BLOCK_ITEM_NONE;
		for(int type = 0; type < 3; type++) {
			renderer.setSideNext(type > 0, type == 2);
			renderer.drawNext(0, 0, engine);
		}
		engine.nextPieceArrayObject[0] = null;
		for(int type = 0; type < 3; type++) {
			renderer.setSideNext(type > 0, type == 2);
			renderer.drawNext(0, 0, engine);
		}
		engine.nextPieceArrayObject[0] = firstNext;
		Piece fourthNext = engine.nextPieceArrayObject[3];
		engine.nextPieceArrayObject[3] = null;
		renderer.setSideNext(false, false);
		renderer.drawNext(0, 0, engine);
		engine.nextPieceArrayObject[3] = fourthNext;
		renderer.setShowMeter(false);
		renderer.drawNext(0, 0, engine);
		renderer.setShowMeter(true);

		engine.ruleopt.holdLimit = 10;
		engine.holdUsedCount = 5;
		engine.holdDisable = false;
		renderer.drawNext(0, 0, engine);
		engine.holdDisable = true;
		renderer.drawNext(0, 0, engine);
		engine.holdDisable = false;
		engine.holdUsedCount = 0;
		renderer.drawNext(0, 0, engine);
		engine.ruleopt.holdLimit = 11;
		renderer.drawNext(0, 0, engine);
		engine.ruleopt.holdLimit = 10;
		engine.holdPieceObject = null;
		renderer.drawNext(0, 0, engine);
		engine.holdPieceObject = piece(Piece.PIECE_L);
		engine.ruleopt.holdLimit = 0;
		engine.holdUsedCount = 0;
		renderer.drawNext(0, 0, engine);
		engine.ruleopt.holdEnable = false;
		renderer.drawNext(0, 0, engine);
		engine.ruleopt.holdEnable = true;
		engine.holdPieceObject = null;
		engine.isNextVisible = false;
		engine.isHoldVisible = false;
		renderer.drawNext(0, 0, engine);
		renderer.setShowBackground(false);
		renderer.drawNext(0, 0, engine);
		renderer.setShowBackground(true);
		engine.isNextVisible = true;
		engine.isHoldVisible = true;
		engine.ruleopt.nextDisplay = 0;
		for(int type = 0; type < 3; type++) {
			renderer.setSideNext(type > 0, type == 2);
			renderer.drawNext(0, 0, engine);
		}

		engine.isNextVisible = true;
		engine.ruleopt.nextDisplay = 5;
		engine.nowPieceObject = null;
		renderer.drawShadowNexts(0, 0, engine, 1f);
		engine.nowPieceObject = piece(Piece.PIECE_T);
		engine.nowPieceBottomY = -20;
		renderer.drawShadowNexts(0, 0, engine, 1f);
		engine.nowPieceBottomY = 10;
		engine.nowPieceObject.big = true;
		renderer.drawShadowNexts(0, 0, engine, 1f);
		engine.nowPieceObject.big = false;
		engine.displaysize = 1;
		renderer.drawShadowNexts(0, 0, engine, 1f);
		engine.displaysize = 0;

		engine.owner.menuOnly = true;
		renderer.renderFirst(engine, 0);
		engine.owner.menuOnly = false;
		engine.isVisible = false;
		renderer.renderFirst(engine, 0);
		engine.isVisible = true;
		engine.owner.backgroundStatus.bg = 0;
		renderer.setShowBackground(true);
		ResourceHolderSDL.imgPlayBG = new SdlTexture[] {TEXTURE, TEXTURE};
		renderer.renderFirst(engine, 0);
		renderer.setShowBackground(false);
		renderer.renderFirst(engine, 0);
		renderer.setShowBackground(true);
		engine.owner.backgroundStatus.bg = -1;
		renderer.renderFirst(engine, 0);
		engine.owner.backgroundStatus.bg = 2;
		renderer.renderFirst(engine, 0);
		engine.owner.backgroundStatus.bg = 0;
		engine.owner.backgroundStatus.fadesw = true;
		engine.owner.backgroundStatus.fadebg = 1;
		renderer.heavyeffect = false;
		renderer.renderFirst(engine, 0);
		renderer.heavyeffect = true;
		engine.owner.backgroundStatus.fadestat = true;
		renderer.renderFirst(engine, 0);
		engine.owner.backgroundStatus.fadestat = false;
		renderer.renderFirst(engine, 0);
		engine.owner.backgroundStatus.bg = -2;
		ResourceHolderSDL.imgPlayBG = null;
		renderer.renderFirst(engine, 0);
		engine.owner.backgroundStatus.bg = -1;
		renderer.renderFirst(engine, 0);
		engine.displaysize = -1;
		ResourceHolderSDL.imgPlayBG = new SdlTexture[] {TEXTURE};
		engine.owner.backgroundStatus.fadesw = false;
		engine.owner.backgroundStatus.bg = 0;
		renderer.renderFirst(engine, 0);
		renderer.renderFirst(engine, 1);

		assertTrue(SDL_CALLS.get() > 100);
	}

	@Test
	void statusAndEffectHooksCoverVisibleTextAndPlayerCountBranches() throws Exception {
		GameEngine one = engine(1);
		one.field = new Field(10, 20, 0);
		one.owner.mode = new PlayersMode(1, true);
		renderer.saveReplay(one.owner, new CustomProperties());
		one.owner.mode = new PlayersMode(1, false);
		NullpoMinoSDL.propGlobal.setProperty("custom.replay.directory",
				Files.createTempDirectory("nullpomino-renderer-replay-").toString());
		renderer.saveReplay(one.owner, new CustomProperties());
		one.allowTextRenderByReceiver = false;
		renderer.renderReady(one, 0);
		renderer.renderExcellent(one, 0);
		renderer.renderGameOver(one, 0);
		renderer.renderResult(one, 0);
		one.allowTextRenderByReceiver = true;
		one.isVisible = true;

		for(int display : new int[] {-1, 0}) {
			one.displaysize = display;
			one.statc[0] = 1;
			renderer.renderReady(one, 0);
			one.readyStart = 10;
			renderer.renderReady(one, 0);
			one.readyStart = GameEngine.READY_START;
			one.statc[0] = one.readyStart;
			renderer.renderReady(one, 0);
			one.statc[0] = one.goStart;
			renderer.renderReady(one, 0);
			one.statc[0] = one.readyEnd;
			renderer.renderReady(one, 0);
			one.statc[0] = one.goEnd;
			renderer.renderReady(one, 0);
			one.statc[0] = 0;
			renderer.renderReady(one, 0);
			one.statc[1] = 0;
			renderer.renderExcellent(one, 0);
			one.statc[1] = 1;
			renderer.renderExcellent(one, 0);
			one.statc[0] = one.field.getHeight() + 1;
			renderer.renderGameOver(one, 0);
			one.statc[0] = 0;
			renderer.renderResult(one, 0);
			one.statc[0] = 1;
			renderer.renderResult(one, 0);
			one.statc[0] = 2;
			renderer.renderResult(one, 0);
		}

		one.isVisible = false;
		renderer.renderExcellent(one, 0);
		renderer.renderGameOver(one, 0);
		renderer.renderResult(one, 0);
		renderer.renderMove(one, 0);
		one.isVisible = true;
		one.nowPieceObject = piece(Piece.PIECE_T);
		one.nowPieceX = 3;
		one.nowPieceY = 0;
		one.nowPieceBottomY = 10;
		one.ghost = true;
		one.ruleopt.ghost = true;
		one.ruleopt.moveFirstFrame = true;
		one.ai = new DummyAI();
		one.ai.bestX = 3;
		one.ai.bestY = 5;
		one.aiHintPiece = piece(Piece.PIECE_O);
		one.aiHintReady = true;
		one.aiShowHint = true;
		renderer.nextshadow = true;
		for(int display : new int[] {-1, 0, 1}) {
			one.displaysize = display;
			renderer.renderMove(one, 0);
		}
		one.statc[0] = 0;
		one.ruleopt.moveFirstFrame = true;
		for(int display : new int[] {-1, 0, 1}) {
			one.displaysize = display;
			renderer.nextshadow = false;
			one.ghost = false;
			one.ruleopt.ghost = true;
			one.ai = null;
			renderer.renderMove(one, 0);

			one.ghost = true;
			one.ruleopt.ghost = false;
			one.ai = new DummyAI();
			one.aiShowHint = false;
			one.aiHintReady = true;
			renderer.renderMove(one, 0);

			one.ruleopt.ghost = true;
			one.aiShowHint = true;
			one.aiHintReady = false;
			renderer.renderMove(one, 0);
		}
		one.displaysize = 0;
		one.ghost = true;
		one.ruleopt.ghost = false;
		one.aiShowHint = false;
		renderer.renderMove(one, 0);
		one.ai = null;
		renderer.renderMove(one, 0);
		one.ghost = false;
		one.ruleopt.ghost = true;
		renderer.renderMove(one, 0);
		one.statc[0] = 0;
		one.ruleopt.moveFirstFrame = false;
		renderer.renderMove(one, 0);

		Block normal = new Block(Block.BLOCK_COLOR_RED);
		normal.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		Block gem = new Block(Block.BLOCK_COLOR_GEM_RED);
		gem.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		renderer.blockBreak(one, 0, 0, 0, null);
		one.displaysize = -1;
		renderer.blockBreak(one, 0, 0, 0, normal);
		one.displaysize = 0;
		renderer.blockBreak(one, 0, 0, 0, normal);
		renderer.blockBreak(one, 0, 1, 0, gem);
		normal.setAttribute(Block.BLOCK_ATTRIBUTE_BONE, true);
		renderer.blockBreak(one, 0, 2, 0, normal);
		renderer.showlineeffect = false;
		renderer.blockBreak(one, 0, 3, 0, gem);
		renderer.showlineeffect = true;
		Block empty = new Block(Block.BLOCK_COLOR_NONE);
		renderer.blockBreak(one, 0, 4, 0, empty);
		assertEquals(2, renderer.effectlist.size());

		renderer.effectRender();
		for(int i = 0; i < 61; i++) renderer.effectUpdate();
		renderer.effectUpdate();
		renderer.effectRender();
		assertTrue(renderer.effectlist.isEmpty());
		renderer.effectlist.add(new EffectObject(1, 0, 0, Block.BLOCK_COLOR_RED));
		renderer.effectlist.get(0).anim = 30;
		renderer.effectRender();
		ResourceHolderSDL.imgBreak = null;
		ResourceHolderSDL.imgPErase = null;
		renderer.effectlist.add(new EffectObject(2, 0, 0, Block.BLOCK_COLOR_GEM_RED));
		renderer.effectRender();

		one.fldeditColor = Block.BLOCK_COLOR_GREEN;
		one.fldeditFrames = 0;
		renderer.renderFieldEdit(one, 0);
		one.fldeditFrames = 30;
		renderer.renderFieldEdit(one, 0);
		renderer.onLast(one, 1);
		renderer.renderLast(one, 1);
		renderer.onLast(one, 0);
		renderer.renderLast(one, 0);

		GameEngine two = engine(2);
		for(GameEngine e : two.owner.engine) {
			e.field = new Field(10, 20, 0);
			e.allowTextRenderByReceiver = true;
			e.isVisible = true;
			e.stat = GameEngine.Status.GAMEOVER;
			e.statc[0] = 21;
			e.displaysize = 0;
		}
		renderer.renderExcellent(two, 0);
		renderer.renderGameOver(two, 0);
		two.owner.engine[1].stat = GameEngine.Status.MOVE;
		renderer.renderGameOver(two, 0);
		two.displaysize = -1;
		two.owner.engine[1].stat = GameEngine.Status.GAMEOVER;
		renderer.renderGameOver(two, 0);
		two.owner.engine[1].stat = GameEngine.Status.MOVE;
		renderer.renderGameOver(two, 0);
		two.statc[0] = two.field.getHeight() + 181;
		renderer.renderGameOver(two, 0);

		GameEngine three = engine(3);
		three.field = new Field(10, 20, 0);
		three.allowTextRenderByReceiver = true;
		three.isVisible = true;
		three.statc[1] = 1;
		three.displaysize = 0;
		renderer.renderExcellent(three, 0);
		three.displaysize = -1;
		renderer.renderExcellent(three, 0);
		three.statc[0] = three.field.getHeight() + 1;
		three.displaysize = 0;
		renderer.renderGameOver(three, 0);
		three.displaysize = -1;
		renderer.renderGameOver(three, 0);

		assertDoesNotThrow(() -> renderer.renderGameOver(one, 0));
		assertTrue(SDL_CALLS.get() > 0);
	}

	private static SDL3 sdlStub() {
		return (SDL3) Proxy.newProxyInstance(SDL3.class.getClassLoader(), new Class<?>[] {SDL3.class},
				(proxy, method, args) -> {
					SDL_CALLS.incrementAndGet();
					if(method.getName().equals("SDL_GetTextureSize")) {
						((FloatRef) args[1]).value = TEXTURE_SIZE.get();
						((FloatRef) args[2]).value = TEXTURE_SIZE.get();
					}
					return defaultValue(method.getReturnType());
				});
	}

	private static <T> T stub(Class<T> iface) {
		return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
				(proxy, method, args) -> defaultValue(method.getReturnType())));
	}

	private static Object defaultValue(Class<?> type) {
		if(type == byte.class) return (byte) 1;
		if(type == short.class) return (short) 0;
		if(type == int.class) return 0;
		if(type == long.class) return 0L;
		if(type == float.class) return 0f;
		if(type == double.class) return 0d;
		if(type == boolean.class) return false;
		return null;
	}

	private static LinkedList<SdlTexture> textures(int count) {
		LinkedList<SdlTexture> result = new LinkedList<SdlTexture>();
		for(int i = 0; i < count; i++) result.add(TEXTURE);
		return result;
	}

	private GameEngine engine(int players) {
		GameManager manager = new GameManager(renderer);
		manager.mode = new PlayersMode(players);
		manager.init();
		for(GameEngine engine : manager.engine) engine.init();
		return manager.engine[0];
	}

	private static Piece piece(int id) {
		Piece piece = new Piece(id);
		piece.setSkin(0);
		piece.setColor(Block.BLOCK_COLOR_RED);
		piece.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		piece.updateConnectData();
		return piece;
	}

	private static Field decoratedField(int width, int height) {
		Field field = new Field(width, height, 0);
		Block visible = new Block(Block.BLOCK_COLOR_RED, 0);
		visible.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_OUTLINE, true);
		field.setBlockE(0, 0, visible);
		Block invisible = new Block(Block.BLOCK_COLOR_BLUE, 0);
		invisible.alpha = 0.5f;
		invisible.setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true);
		field.setBlockE(1, 0, invisible);
		Block wall = new Block(Block.BLOCK_COLOR_GRAY, 0);
		wall.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_WALL, true);
		field.setBlockE(0, 1, wall);
		Block bone = new Block(Block.BLOCK_COLOR_GREEN, 0);
		bone.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_OUTLINE |
				Block.BLOCK_ATTRIBUTE_BONE, true);
		field.setBlockE(1, 1, bone);
		return field;
	}

	private static class PlayersMode implements GameMode {
		private final int players;
		private final boolean netplay;
		PlayersMode(int players) { this(players, false); }
		PlayersMode(int players, boolean netplay) { this.players = players; this.netplay = netplay; }
		@Override public String getName() { return "renderer-test"; }
		@Override public int getPlayers() { return players; }
		@Override public int getGameStyle() { return 0; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void renderInput(GameEngine engine, int playerID) {}
		@Override public boolean isNetplayMode() { return netplay; }
	}

	private static final class Harness extends RendererSDL {
		void setOutlineGhost(boolean value) { outlineghost = value; }
		void setShowMeter(boolean value) { showmeter = value; }
		void setSideNext(boolean side, boolean big) { sidenext = side; bigsidenext = big; }
		void setShowBackground(boolean value) { showbg = value; }
		void setDarkNextArea(boolean value) { darknextarea = value; }
	}
}
