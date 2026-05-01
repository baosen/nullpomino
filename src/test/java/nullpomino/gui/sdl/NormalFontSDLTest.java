package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NormalFontSDLTest {

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
	void fontColorLookupReturnsDefensiveCopies() {
		int[] red = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_RED);
		red[0] = 0;

		assertArrayEquals(new int[] {255, 0, 0},
				NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_RED));
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
}
