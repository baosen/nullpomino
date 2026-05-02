package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class NormalFontSDLStaticHelpersTest {

    // getFontColorRGB tests
    @Test
    void getFontColorRGBReturnsWhiteForIndex0() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_WHITE);
        assertArrayEquals(new int[]{255, 255, 255}, rgb);
    }

    @Test
    void getFontColorRGBReturnsBlueForIndex1() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_BLUE);
        assertArrayEquals(new int[]{0, 0, 255}, rgb);
    }

    @Test
    void getFontColorRGBReturnsRedForIndex2() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_RED);
        assertArrayEquals(new int[]{255, 0, 0}, rgb);
    }

    @Test
    void getFontColorRGBReturnsGreenForIndex4() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_GREEN);
        assertArrayEquals(new int[]{0, 255, 0}, rgb);
    }

    @Test
    void getFontColorRGBReturnsYellowForIndex5() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_YELLOW);
        assertArrayEquals(new int[]{255, 255, 0}, rgb);
    }

    @Test
    void getFontColorRGBReturnsCyanForIndex6() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_CYAN);
        assertArrayEquals(new int[]{0, 255, 255}, rgb);
    }

    @Test
    void getFontColorRGBReturnsOrangeForIndex7() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_ORANGE);
        assertArrayEquals(new int[]{255, 128, 0}, rgb);
    }

    @Test
    void getFontColorRGBReturnsPurpleForIndex8() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_PURPLE);
        assertArrayEquals(new int[]{255, 0, 255}, rgb);
    }

    @Test
    void getFontColorRGBReturnsDarkBlueForIndex9() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_DARKBLUE);
        assertArrayEquals(new int[]{0, 0, 128}, rgb);
    }

    @Test
    void getFontColorRGBReturnsLightGrayForIndex10() {
        int[] rgb = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_LIGHTGRAY);
        assertArrayEquals(new int[]{192, 192, 192}, rgb);
    }

    @Test
    void getFontColorRGBReturnsWhiteForNegativeIndex() {
        int[] rgb = NormalFontSDL.getFontColorRGB(-1);
        assertArrayEquals(new int[]{255, 255, 255}, rgb, "negative index should fall back to white");
    }

    @Test
    void getFontColorRGBReturnsWhiteForTooLargeIndex() {
        int[] rgb = NormalFontSDL.getFontColorRGB(11);
        assertArrayEquals(new int[]{255, 255, 255}, rgb, "out-of-range index should fall back to white");
    }

    @Test
    void getFontColorRGBReturnsDefensiveCopy() {
        int[] rgb1 = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_RED);
        rgb1[0] = 0;
        int[] rgb2 = NormalFontSDL.getFontColorRGB(NormalFontSDL.COLOR_RED);
        assertEquals(255, rgb2[0], "getFontColorRGB should return a defensive copy");
    }

    // safeString tests
    @Test
    void safeStringReturnsEmptyForNull() {
        assertEquals("", NormalFontSDL.safeString(null));
    }

    @Test
    void safeStringReturnsEmptyForEmptyString() {
        assertEquals("", NormalFontSDL.safeString(""));
    }

    @Test
    void safeStringPreservesUppercaseAndDigits() {
        assertEquals("HELLO 123", NormalFontSDL.safeString("HELLO 123"));
    }

    @Test
    void safeStringUppercasesLowercase() {
        assertEquals("HELLO", NormalFontSDL.safeString("hello"));
    }

    @Test
    void safeStringReplacesNonAsciiWithQuestionMark() {
        assertEquals("H?LLO", NormalFontSDL.safeString("héllo"));
    }

    @Test
    void safeStringPreservesNewlines() {
        assertEquals("HELLO\nWORLD", NormalFontSDL.safeString("hello\nworld"));
    }

    @Test
    void safeStringReplacesControlCharactersWithQuestionMark() {
        assertEquals("A?B", NormalFontSDL.safeString("A\tB"));
    }

    @Test
    void safeStringPreservesPrintableAsciiRange() {
        assertEquals(" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`ABCDEFGHIJKLMNOPQRSTUVWXYZ{|}~",
                NormalFontSDL.safeString(" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~"));
    }
}
