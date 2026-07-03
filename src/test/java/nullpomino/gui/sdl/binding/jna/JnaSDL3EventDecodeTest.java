package nullpomino.gui.sdl.binding.jna;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.function.Consumer;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;

/**
 * Covers {@link JnaSDL3}'s decoding of the 128-byte SDL_Event union into the
 * backend-neutral event POJO, in particular the non-null text pointer branch
 * of SDL_EVENT_TEXT_INPUT (UTF-8, including multi-byte).
 */
class JnaSDL3EventDecodeTest {

	/**
	 * Build a RawSDL3 whose SDL_PollEvent fills the caller's event buffer via
	 * {@code filler} and reports one pending event. All other methods throw.
	 */
	private static RawSDL3 fakePoll(Consumer<Pointer> filler) {
		return (RawSDL3) Proxy.newProxyInstance(
			RawSDL3.class.getClassLoader(),
			new Class<?>[] { RawSDL3.class },
			(proxy, method, args) -> {
				if(method.getName().equals("SDL_PollEvent")) {
					filler.accept((Pointer) args[0]);
					return (byte) 1;
				}
				throw new UnsupportedOperationException(method.getName());
			});
	}

	private static SDLStructs.SDL_Event poll(Consumer<Pointer> filler) {
		JnaSDL3 sdl = new JnaSDL3(fakePoll(filler));
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		assertEquals((byte) 1, sdl.SDL_PollEvent(ev));
		return ev;
	}

	private static Memory utf8(String text) {
		Memory mem = new Memory(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1L);
		mem.setString(0, text, "UTF-8");
		return mem;
	}

	@Test
	void decodesTextInputTextWhenPointerIsNonNull() {
		Memory textMem = utf8("Hello");
		SDLStructs.SDL_Event ev = poll(buf -> {
			buf.setInt(0, SDLConstants.SDL_EVENT_TEXT_INPUT);
			buf.setPointer(24, textMem);
		});

		assertEquals(SDLConstants.SDL_EVENT_TEXT_INPUT, ev.getType());
		assertEquals("Hello", ev.getTextInputText());
	}

	@Test
	void decodesMultiByteUtf8TextInput() {
		Memory textMem = utf8("テスト");
		SDLStructs.SDL_Event ev = poll(buf -> {
			buf.setInt(0, SDLConstants.SDL_EVENT_TEXT_INPUT);
			buf.setPointer(24, textMem);
		});

		assertEquals("テスト", ev.getTextInputText());
	}

	@Test
	void decodesNullTextPointerAsEmptyString() {
		SDLStructs.SDL_Event ev = poll(buf -> {
			buf.setInt(0, SDLConstants.SDL_EVENT_TEXT_INPUT);
			buf.setPointer(24, null);
		});

		assertEquals("", ev.getTextInputText());
	}

	@Test
	void decodesTextEditingCursorFields() {
		Memory textMem = utf8("かな");
		SDLStructs.SDL_Event ev = poll(buf -> {
			buf.setInt(0, SDLConstants.SDL_EVENT_TEXT_EDITING);
			buf.setPointer(24, textMem);
			buf.setInt(32, 3);
			buf.setInt(36, 2);
		});

		assertEquals("かな", ev.getTextInputText());
		assertEquals(3, ev.getTextEditingStart());
		assertEquals(2, ev.getTextEditingLength());
	}

	@Test
	void decodesKeyDownFields() {
		SDLStructs.SDL_Event ev = poll(buf -> {
			buf.setInt(0, SDLConstants.SDL_EVENT_KEY_DOWN);
			buf.setInt(24, SDLConstants.SDL_SCANCODE_UP);
			buf.setInt(28, 0x40000052);
			buf.setShort(32, (short) SDLConstants.SDL_KMOD_LSHIFT);
			buf.setByte(36, (byte) 1);
			buf.setByte(37, (byte) 1);
		});

		assertEquals(SDLConstants.SDL_SCANCODE_UP, ev.getScancode());
		assertEquals(0x40000052, ev.getKeycode());
		assertEquals(SDLConstants.SDL_KMOD_LSHIFT, ev.getKeymod());
		assertTrue(ev.isKeyDown());
		assertTrue(ev.isKeyRepeat());
	}

	@Test
	void decodesKeymodAsUnsignedSixteenBit() {
		SDLStructs.SDL_Event ev = poll(buf -> {
			buf.setInt(0, SDLConstants.SDL_EVENT_KEY_DOWN);
			buf.setShort(32, (short) 0xFFFF);
		});

		// A naive `int` widening of a signed short would yield -1, so the
		// `& 0xFFFF` mask in the decoder is load-bearing.
		assertEquals(0xFFFF, ev.getKeymod());
	}

	@Test
	void decodesWindowDataFields() {
		SDLStructs.SDL_Event ev = poll(buf -> {
			buf.setInt(0, SDLConstants.SDL_EVENT_WINDOW_RESIZED);
			buf.setInt(20, 800);
			buf.setInt(24, 600);
		});

		assertEquals(800, ev.getWindowData1());
		assertEquals(600, ev.getWindowData2());
	}

	@Test
	void decodesMouseWheelDeltas() {
		SDLStructs.SDL_Event ev = poll(buf -> {
			buf.setInt(0, SDLConstants.SDL_EVENT_MOUSE_WHEEL);
			buf.setFloat(24, -1.5f);
			buf.setFloat(28, 2.0f);
		});

		assertEquals(-1.5f, ev.getMouseWheelX());
		assertEquals(2.0f, ev.getMouseWheelY());
	}
}
