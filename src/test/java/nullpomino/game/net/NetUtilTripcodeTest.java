package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

/**
 * Tests for NetUtil tripcode generation and Shift_JIS conversion methods.
 *
 * <p>Covers {@link NetUtil#createTripCode(String, int)},
 * {@link NetUtil#stringToShiftJIS(String)}, and
 * {@link NetUtil#shiftJIStoString(byte[])}.
 */
class NetUtilTripcodeTest {

    // ========================
    // createTripCode tests
    // ========================

    @Test
    void tripCodeKnownValues() {
        assertEquals("ozOtJW9BFA", NetUtil.createTripCode("password", 10));
        assertEquals("5G6R5bcZ.A", NetUtil.createTripCode("a:b", 10));
        assertEquals("t2.tko2NFA", NetUtil.createTripCode("a[b", 10));
        assertEquals("H6UXeNPUgM", NetUtil.createTripCode("a\nb", 10));
    }

    @Test
    void tripCodeMaxlenTruncation() {
        // maxlen shorter than the 13-char crypt output takes the last N chars
        assertEquals("o2NFA", NetUtil.createTripCode("a[b", 5));
        assertEquals("A", NetUtil.createTripCode("a[b", 1));
    }

    @Test
    void tripCodeEmptyKey() {
        // Empty tripkey should not throw; salt falls back to H/. appended bytes
        String result = NetUtil.createTripCode("", 10);
        assertNotNull(result);
        assertEquals(10, result.length());
        // Pinned output from DES crypt(.., empty) truncated to last 10 chars
        assertEquals("8NBuQ4l6uQ", result);
    }

    @Test
    void tripCodeNullKey() {
        assertThrows(NullPointerException.class,
                () -> NetUtil.createTripCode(null, 10));
    }

    @Test
    void tripCodeMaxlenZero() {
        assertEquals("", NetUtil.createTripCode("a[b", 0));
    }

    @Test
    void tripCodeMaxlenExceedsCryptOutput() {
        // When maxlen > 13 (crypt output length), the full string is returned
        String result = NetUtil.createTripCode("a", 100);
        assertNotNull(result);
        assertEquals(13, result.length());
    }

    @Test
    void tripCodeSpecialCharacters() {
        // Various special / whitespace characters
        assertNotNull(NetUtil.createTripCode("hello world", 10));
        assertNotNull(NetUtil.createTripCode("tab\there", 10));
        assertNotNull(NetUtil.createTripCode("unicode→test", 10));
        assertNotNull(NetUtil.createTripCode("!@#$%^&*()", 10));
    }

    @Test
    void tripCodeConsistency() {
        // Same input always produces the same output
        String first = NetUtil.createTripCode("testKey123", 10);
        String second = NetUtil.createTripCode("testKey123", 10);
        assertEquals(first, second);
    }

    // ========================
    // stringToShiftJIS tests
    // ========================

    @Test
    void stringToShiftJisAscii() {
        assertArrayEquals(
                new byte[] {'H', 'e', 'l', 'l', 'o'},
                NetUtil.stringToShiftJIS("Hello"));
    }

    @Test
    void stringToShiftJisEmpty() {
        assertArrayEquals(new byte[0], NetUtil.stringToShiftJIS(""));
    }

    @Test
    void stringToShiftJisNull() {
        assertThrows(NullPointerException.class,
                () -> NetUtil.stringToShiftJIS(null));
    }

    @Test
    void stringToShiftJisJapanese() {
        String japanese = "日本語";
        byte[] expected = japanese.getBytes(Charset.forName("Shift_JIS"));
        assertArrayEquals(expected, NetUtil.stringToShiftJIS(japanese));
    }

    @Test
    void stringToShiftJisMatchesStandardCharset() {
        String[] samples = {"", "abc", "テスト", "あいう", "Hello, 世界!"};
        Charset sjis = Charset.forName("Shift_JIS");
        for (String s : samples) {
            assertArrayEquals(s.getBytes(sjis), NetUtil.stringToShiftJIS(s));
        }
    }

    // ========================
    // shiftJIStoString tests
    // ========================

    @Test
    void shiftJisToStringAscii() {
        byte[] input = {'H', 'e', 'l', 'l', 'o'};
        assertEquals("Hello", NetUtil.shiftJIStoString(input));
    }

    @Test
    void shiftJisToStringEmpty() {
        assertEquals("", NetUtil.shiftJIStoString(new byte[0]));
    }

    @Test
    void shiftJisToStringNull() {
        assertThrows(NullPointerException.class,
                () -> NetUtil.shiftJIStoString(null));
    }

    @Test
    void shiftJisToStringJapanese() {
        String japanese = "日本語";
        byte[] sjis = japanese.getBytes(Charset.forName("Shift_JIS"));
        assertEquals(japanese, NetUtil.shiftJIStoString(sjis));
    }

    @Test
    void shiftJisRoundTrip() {
        String[] samples = {"", "Hello", "テスト", "あいうえお",
                            "NullpoMino!", "a\nb\tc", "!@#$%^&*()"};
        for (String s : samples) {
            assertEquals(s, NetUtil.shiftJIStoString(NetUtil.stringToShiftJIS(s)));
        }
    }

    @Test
    void shiftJisRoundTripWithAllAscii() {
        // Every ASCII printable character (0x20-0x7E) round-trips cleanly
        StringBuilder sb = new StringBuilder(95);
        for (int c = 0x20; c <= 0x7E; c++) {
            sb.append((char) c);
        }
        String ascii = sb.toString();
        assertEquals(ascii, NetUtil.shiftJIStoString(NetUtil.stringToShiftJIS(ascii)));
    }
}
