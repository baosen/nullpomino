package nullpomino.gui.sdl.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;

import org.junit.jupiter.api.Test;

/**
 * Covers the non-null pointer branch in
 * {@link SDLStructs.SDL_Event#getTextInputText()} (line 177).
 */
class SDLStructsTextInputTest {

	@Test
	void getTextInputTextReturnsUtf8StringWhenPointerIsNonNull() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();

		// Allocate native memory for the text string and write UTF-8 text into it.
		String expected = "Hello";
		Memory textMem = new Memory(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1);
		textMem.setString(0, expected, "UTF-8");

		// Place the pointer at offset 24 (the text-input text pointer field).
		ev.getPointer().setPointer(24, textMem);

		assertEquals(expected, ev.getTextInputText());
	}

	@Test
	void getTextInputTextReturnsMultiByteUtf8String() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();

		String expected = "テスト";
		Memory textMem = new Memory(expected.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + 1);
		textMem.setString(0, expected, "UTF-8");

		ev.getPointer().setPointer(24, textMem);

		assertEquals(expected, ev.getTextInputText());
	}
}
