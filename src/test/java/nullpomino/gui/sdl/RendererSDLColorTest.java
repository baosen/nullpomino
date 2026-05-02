package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link RendererSDL}: the color-packing
 * helper {@link RendererSDL#getColorValue}, the per-block-color
 * lookups {@link RendererSDL#getColorByID} (dim) and
 * {@link RendererSDL#getColorByIDBright} (bright), the sticky-skin
 * predicate, and {@link RendererSDL#getKeyNameByButtonID} which
 * resolves a button ID through the active player's keymap.
 *
 * <p>The renderer's drawing methods need an SDL context, but these
 * helpers are pure logic against the static color tables, the
 * blockStickyFlagList, and GameKeySDL.gamekey.
 */
class RendererSDLColorTest {

	private GameKeySDL[] originalGameKey;
	private LinkedList<Boolean> originalStickyList;
	private CustomProperties originalPropConfig;

	@BeforeEach
	void setUp() {
		originalGameKey = GameKeySDL.gamekey;
		originalStickyList = ResourceHolderSDL.blockStickyFlagList;
		originalPropConfig = NullpoMinoSDL.propConfig;
		GameKeySDL.initGlobalGameKeySDL();
		ResourceHolderSDL.blockStickyFlagList = new LinkedList<Boolean>();
		// RendererSDL constructor reads from NullpoMinoSDL.propConfig.
		NullpoMinoSDL.propConfig = new CustomProperties();
	}

	@AfterEach
	void tearDown() {
		GameKeySDL.gamekey = originalGameKey;
		ResourceHolderSDL.blockStickyFlagList = originalStickyList;
		NullpoMinoSDL.propConfig = originalPropConfig;
	}

	@Test
	void getColorValuePacksRgbIntoTwentyFourBitLong() {
		RendererSDL renderer = new RendererSDL();

		assertEquals(0L, renderer.getColorValue(0, 0, 0));
		assertEquals(0xFFFFFFL, renderer.getColorValue(255, 255, 255));
		// red goes into bits 16..23, green 8..15, blue 0..7.
		assertEquals(0x123456L, renderer.getColorValue(0x12, 0x34, 0x56));
	}

	@Test
	void getColorByIDReturnsDimVariantForEachBlockColor() {
		// The dim table is the half-bright palette used for normal blocks.
		// Pin the documented values so a re-skin has to acknowledge.
		RendererSDL renderer = new RendererSDL();

		assertEquals(renderer.getColorValue( 64,  64,  64),
				renderer.getColorByID(Block.BLOCK_COLOR_GRAY));
		assertEquals(renderer.getColorValue(128,   0,   0),
				renderer.getColorByID(Block.BLOCK_COLOR_RED));
		assertEquals(renderer.getColorValue(128,  64,   0),
				renderer.getColorByID(Block.BLOCK_COLOR_ORANGE));
		assertEquals(renderer.getColorValue(128, 128,   0),
				renderer.getColorByID(Block.BLOCK_COLOR_YELLOW));
		assertEquals(renderer.getColorValue(  0, 128,   0),
				renderer.getColorByID(Block.BLOCK_COLOR_GREEN));
		assertEquals(renderer.getColorValue(  0, 128, 128),
				renderer.getColorByID(Block.BLOCK_COLOR_CYAN));
		assertEquals(renderer.getColorValue(  0,   0, 128),
				renderer.getColorByID(Block.BLOCK_COLOR_BLUE));
		assertEquals(renderer.getColorValue(128,   0, 128),
				renderer.getColorByID(Block.BLOCK_COLOR_PURPLE));
	}

	@Test
	void getColorByIDFallsBackToBlackForUnknownColor() {
		// Color IDs outside the GRAY..PURPLE range surface as RGB(0,0,0).
		RendererSDL renderer = new RendererSDL();

		assertEquals(0L, renderer.getColorByID(Block.BLOCK_COLOR_NONE));
		assertEquals(0L, renderer.getColorByID(-1));
		assertEquals(0L, renderer.getColorByID(99999));
	}

	@Test
	void getColorByIDBrightReturnsFullBrightVariantForEachBlockColor() {
		RendererSDL renderer = new RendererSDL();

		assertEquals(renderer.getColorValue(128, 128, 128),
				renderer.getColorByIDBright(Block.BLOCK_COLOR_GRAY));
		assertEquals(renderer.getColorValue(255,   0,   0),
				renderer.getColorByIDBright(Block.BLOCK_COLOR_RED));
		assertEquals(renderer.getColorValue(255, 128,   0),
				renderer.getColorByIDBright(Block.BLOCK_COLOR_ORANGE));
		assertEquals(renderer.getColorValue(255, 255,   0),
				renderer.getColorByIDBright(Block.BLOCK_COLOR_YELLOW));
		assertEquals(renderer.getColorValue(  0, 255,   0),
				renderer.getColorByIDBright(Block.BLOCK_COLOR_GREEN));
		assertEquals(renderer.getColorValue(  0, 255, 255),
				renderer.getColorByIDBright(Block.BLOCK_COLOR_CYAN));
		assertEquals(renderer.getColorValue(  0,   0, 255),
				renderer.getColorByIDBright(Block.BLOCK_COLOR_BLUE));
		assertEquals(renderer.getColorValue(255,   0, 255),
				renderer.getColorByIDBright(Block.BLOCK_COLOR_PURPLE));
	}

	@Test
	void getColorByIDBrightFallsBackToBlackForUnknownColor() {
		RendererSDL renderer = new RendererSDL();

		assertEquals(0L, renderer.getColorByIDBright(Block.BLOCK_COLOR_NONE));
		assertEquals(0L, renderer.getColorByIDBright(99999));
	}

	@Test
	void isStickySkinReturnsFalseForOutOfRangeIndices() {
		RendererSDL renderer = new RendererSDL();
		ResourceHolderSDL.blockStickyFlagList.add(Boolean.TRUE);
		ResourceHolderSDL.blockStickyFlagList.add(Boolean.FALSE);

		// In-range with TRUE flag.
		assertTrue(renderer.isStickySkin(0));
		assertFalse(renderer.isStickySkin(1));
		// Out-of-range above and below.
		assertFalse(renderer.isStickySkin(-1));
		assertFalse(renderer.isStickySkin(2));
		assertFalse(renderer.isStickySkin(99));
	}

	@Test
	void isStickySkinReturnsFalseForEmptyStickyList() {
		RendererSDL renderer = new RendererSDL();
		// Empty list -> any skin index falls through to false.

		assertFalse(renderer.isStickySkin(0));
		assertFalse(renderer.isStickySkin(5));
	}

	@Test
	void getKeyNameByButtonIDLooksUpThroughIngameKeymapWhenInGame() {
		RendererSDL renderer = new RendererSDL();
		GameEngine engine = freshEngine();
		engine.isInGame = true;
		// Spoil the in-game keymap entry with a known scancode.
		GameKeySDL.gamekey[engine.playerID].keymap[GameKeySDL.BUTTON_A] =
				SDLConstants.SDL_SCANCODE_RETURN;
		// And keymapNav has a different entry; we should NOT see this.
		GameKeySDL.gamekey[engine.playerID].keymapNav[GameKeySDL.BUTTON_A] =
				SDLConstants.SDL_SCANCODE_ESCAPE;

		assertEquals(SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_RETURN],
				renderer.getKeyNameByButtonID(engine, GameKeySDL.BUTTON_A),
				"isInGame=true must read keymap, not keymapNav");
	}

	@Test
	void getKeyNameByButtonIDLooksUpThroughKeymapNavWhenNotInGame() {
		RendererSDL renderer = new RendererSDL();
		GameEngine engine = freshEngine();
		engine.isInGame = false;
		GameKeySDL.gamekey[engine.playerID].keymap[GameKeySDL.BUTTON_A] =
				SDLConstants.SDL_SCANCODE_RETURN;
		GameKeySDL.gamekey[engine.playerID].keymapNav[GameKeySDL.BUTTON_A] =
				SDLConstants.SDL_SCANCODE_ESCAPE;

		assertEquals(SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_ESCAPE],
				renderer.getKeyNameByButtonID(engine, GameKeySDL.BUTTON_A));
	}

	@Test
	void getKeyNameByButtonIDReturnsEmptyForOutOfRangeButtonOrKeycode() {
		RendererSDL renderer = new RendererSDL();
		GameEngine engine = freshEngine();
		engine.isInGame = false;

		// Out-of-range button.
		assertEquals("", renderer.getKeyNameByButtonID(engine, -1));
		assertEquals("", renderer.getKeyNameByButtonID(engine, 9999));

		// Out-of-range scancode (keymap entry < 0 -> the SCANCODE_NAMES
		// guard returns empty).
		GameKeySDL.gamekey[engine.playerID].keymapNav[GameKeySDL.BUTTON_A] = -1;
		assertEquals("", renderer.getKeyNameByButtonID(engine, GameKeySDL.BUTTON_A));
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}
}
