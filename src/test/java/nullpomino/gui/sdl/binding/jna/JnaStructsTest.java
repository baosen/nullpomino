package nullpomino.gui.sdl.binding.jna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.binding.SDLStructs;

/**
 * The JNA field order is what defines the native memory layout, so these
 * checks are load-bearing for marshalling correctness.
 */
class JnaStructsTest {

	@Test
	void fRectFieldOrderListsXYWH() {
		assertEquals(Arrays.asList("x", "y", "w", "h"),
				new JnaStructs.SDL_FRect().getFieldOrder());
	}

	@Test
	void rectFieldOrderListsXYWH() {
		assertEquals(Arrays.asList("x", "y", "w", "h"),
				new JnaStructs.SDL_Rect().getFieldOrder());
	}

	@Test
	void audioSpecFieldOrderListsFormatChannelsFreq() {
		assertEquals(Arrays.asList("format", "channels", "freq"),
				new JnaStructs.SDL_AudioSpec().getFieldOrder());
	}

	@Test
	void colorFieldOrderListsRGBA() {
		assertEquals(Arrays.asList("r", "g", "b", "a"),
				new JnaStructs.SDL_Color().getFieldOrder());
	}

	@Test
	void frectConverterCopiesAllFieldsAndPassesNullThrough() {
		JnaStructs.SDL_FRect r = JnaStructs.frect(new SDLStructs.SDL_FRect(1f, 2f, 3f, 4f));
		assertEquals(1f, r.x);
		assertEquals(2f, r.y);
		assertEquals(3f, r.w);
		assertEquals(4f, r.h);

		assertNull(JnaStructs.frect(null));
	}

	@Test
	void rectConverterCopiesAllFieldsAndPassesNullThrough() {
		JnaStructs.SDL_Rect r = JnaStructs.rect(new SDLStructs.SDL_Rect(10, 20, 30, 40));
		assertEquals(10, r.x);
		assertEquals(20, r.y);
		assertEquals(30, r.w);
		assertEquals(40, r.h);

		assertNull(JnaStructs.rect(null));
	}

	@Test
	void colorConverterPreservesUnsignedComponentValues() {
		JnaStructs.SDL_Color.ByValue c = JnaStructs.color(new SDLStructs.SDL_Color(255, 200, 128, 64));
		assertEquals((byte) 0xFF, c.r);
		assertEquals((byte) 200, c.g);
		assertEquals((byte) 128, c.b);
		assertEquals((byte) 64, c.a);
	}
}
