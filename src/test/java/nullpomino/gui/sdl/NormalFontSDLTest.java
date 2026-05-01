package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NormalFontSDLTest {

	@AfterEach
	void clearTtfFont() {
		// Some tests below temporarily install a non-null ttfFont pointer to
		// exercise the font-loaded short-circuit branches; reset to the
		// suite-wide default of null so we don't leak state across tests.
		ResourceHolderSDL.ttfFont = null;
	}

	@Test
	void fontColorLookupKeepsLegacyRgbValues() {
		assertArrayEquals(new int[] {255, 255, 255},
				NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_WHITE));
		assertArrayEquals(new int[] {0, 0, 255},
				NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_BLUE));
		assertArrayEquals(new int[] {192, 192, 192},
				NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_LIGHTGRAY));
		assertArrayEquals(new int[] {255, 255, 255},
				NormalFontSDL.getFontColorRGB(999));
	}

	@Test
	void fontColorLookupClampsNegativeFontColorToWhite() {
		assertArrayEquals(new int[] {255, 255, 255},
				NormalFontSDL.getFontColorRGB(-1));
	}

	@Test
	void fontColorLookupReturnsDefensiveCopies() {
		int[] red = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_RED);
		red[0] = 0;

		assertArrayEquals(new int[] {255, 0, 0},
				NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_RED));
	}

	@Test
	void colorConstantsCoverTheBitmapAtlasPlusOneTtfOnlyEntry() {
		// Bitmap atlas rows 0..9 plus a TTF-only LIGHTGRAY at row 10.
		assertEquals(0, NormalFontSDL.COLOR_WHITE);
		assertEquals(1, NormalFontSDL.COLOR_BLUE);
		assertEquals(2, NormalFontSDL.COLOR_RED);
		assertEquals(3, NormalFontSDL.COLOR_PINK);
		assertEquals(4, NormalFontSDL.COLOR_GREEN);
		assertEquals(5, NormalFontSDL.COLOR_YELLOW);
		assertEquals(6, NormalFontSDL.COLOR_CYAN);
		assertEquals(7, NormalFontSDL.COLOR_ORANGE);
		assertEquals(8, NormalFontSDL.COLOR_PURPLE);
		assertEquals(9, NormalFontSDL.COLOR_DARKBLUE);
		assertEquals(10, NormalFontSDL.COLOR_LIGHTGRAY);
	}

	@Test
	void safeStringUppercasesAsciiAndKeepsNewlines() {
		assertEquals("ABC 123\nZ", NormalFontSDL.safeString("abc 123\nz"));
	}

	@Test
	void safeStringReplacesControlAndNonAsciiCharacters() {
		String raw = "a\t" + (char)0x2603 + "~";

		assertEquals("A??~", NormalFontSDL.safeString(raw));
		assertEquals("", NormalFontSDL.safeString(null));
	}

	@Test
	void safeStringPassesUppercaseAsciiAndPunctuationThroughUnchanged() {
		assertEquals("HELLO, WORLD! 0123456789",
				NormalFontSDL.safeString("HELLO, WORLD! 0123456789"));
	}

	@Test
	void safeStringConvertsTab() {
		// Tab is below the 32..126 printable range and is not the special
		// newline pass-through, so it falls into the '?' bucket.
		assertEquals("?", NormalFontSDL.safeString("\t"));
	}

	@Test
	void getTtfStringWidthIsZeroWhenFontIsUnloaded() {
		ResourceHolderSDL.ttfFont = null;

		assertEquals(0, NormalFontSDL.getTTFStringWidth("hello"));
	}


	@Test
	void printTtfFontIsNoOpWhenFontIsUnloaded() {
		ResourceHolderSDL.ttfFont = null;

		// Both overloads return early on the null-font guard. The renderer
		// pointer is not touched, so this stays headless-safe.
		NormalFontSDL.printTTFFont(0, 0, "x");
		NormalFontSDL.printTTFFont(0, 0, "x", NormalFontSDL.COLOR_RED);
	}

	@Test
	void printFontWithEmptyStringDoesNotInvokeTheBitmapRenderer() {
		// The for-loop body that calls SDL3.INSTANCE never runs for an empty
		// string, so this exercises the overload routing without hitting SDL.
		NormalFontSDL.printFont(0, 0, "");
		NormalFontSDL.printFont(0, 0, "", NormalFontSDL.COLOR_RED);
		NormalFontSDL.printFont(0, 0, "", NormalFontSDL.COLOR_RED, 1.0f);
		NormalFontSDL.printFont(0, 0, "", false, NormalFontSDL.COLOR_WHITE, NormalFontSDL.COLOR_RED);
		NormalFontSDL.printFont(0, 0, "", true, NormalFontSDL.COLOR_WHITE, NormalFontSDL.COLOR_RED);
		NormalFontSDL.printFont(0, 0, "", false);
		NormalFontSDL.printFont(0, 0, "", true);
		NormalFontSDL.printFont(0, 0, "", false, NormalFontSDL.COLOR_WHITE, NormalFontSDL.COLOR_RED, 1.0f);
		NormalFontSDL.printFont(0, 0, "", true, NormalFontSDL.COLOR_WHITE, NormalFontSDL.COLOR_RED, 1.0f);
		NormalFontSDL.printFont(0, 0, "", true, 1.0f);
	}

	@Test
	void printFontGridWithEmptyStringExercisesEveryGridOverload() {
		NormalFontSDL.printFontGrid(0, 0, "");
		NormalFontSDL.printFontGrid(0, 0, "", NormalFontSDL.COLOR_RED);
		NormalFontSDL.printFontGrid(0, 0, "", false, NormalFontSDL.COLOR_WHITE, NormalFontSDL.COLOR_RED);
		NormalFontSDL.printFontGrid(0, 0, "", true, NormalFontSDL.COLOR_WHITE, NormalFontSDL.COLOR_RED);
		NormalFontSDL.printFontGrid(0, 0, "", false);
		NormalFontSDL.printFontGrid(0, 0, "", true);
	}

	@Test
	void printFontWithNewlineOnlyAdvancesYByScaleSpecificStep() {
		// "\n" enters the if-stringChar==0x0A branch for each scale and
		// exits without rendering any glyph. Each scale takes a different
		// branch (32 / 16 / 8 vertical advance), so this hits all three.
		NormalFontSDL.printFont(0, 0, "\n", NormalFontSDL.COLOR_WHITE, 2.0f);
		NormalFontSDL.printFont(0, 0, "\n", NormalFontSDL.COLOR_WHITE, 1.0f);
		NormalFontSDL.printFont(0, 0, "\n", NormalFontSDL.COLOR_WHITE, 0.5f);
	}
}
