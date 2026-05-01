package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the small per-frame helpers on {@link NullpoMinoSDL} that the menu
 * states depend on every frame: text-input drain, escape-edge detection,
 * and translated UI text lookup. None of these need an SDL native context,
 * but the suite-wide static state has to be snapshotted so tests do not
 * leak into each other.
 */
class NullpoMinoSDLHelpersTest {

	private CustomProperties originalPropLang;
	private CustomProperties originalPropLangDefault;

	@BeforeEach
	void snapshotState() {
		originalPropLang = NullpoMinoSDL.propLang;
		originalPropLangDefault = NullpoMinoSDL.propLangDefault;
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.pendingTextInput.setLength(0);
	}

	@AfterEach
	void restoreState() {
		NullpoMinoSDL.propLang = originalPropLang;
		NullpoMinoSDL.propLangDefault = originalPropLangDefault;
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.pendingTextInput.setLength(0);
	}

	@Test
	void consumeTextInputReturnsEmptyStringWhenNothingHasBeenTyped() {
		assertEquals("", NullpoMinoSDL.consumeTextInput());
	}

	@Test
	void consumeTextInputReturnsAndClearsTheBufferAtomically() {
		NullpoMinoSDL.pendingTextInput.append("hello");

		assertEquals("hello", NullpoMinoSDL.consumeTextInput());
		assertEquals("", NullpoMinoSDL.consumeTextInput(),
				"second consume must observe the cleared buffer");
		assertEquals(0, NullpoMinoSDL.pendingTextInput.length());
	}

	@Test
	void consumeTextInputAccumulatesAcrossMultipleAppendsBeforeTheCall() {
		// processEvent calls SDL_TEXT_INPUT can fire multiple times per frame.
		NullpoMinoSDL.pendingTextInput.append("foo");
		NullpoMinoSDL.pendingTextInput.append("bar");

		assertEquals("foobar", NullpoMinoSDL.consumeTextInput());
	}

	@Test
	void isEscapePushedThisFrameReturnsFalseWhenNoEscapeEventInFrameBuffer() {
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(
				SDLConstants.SDL_SCANCODE_A, 0, false));

		assertFalse(NullpoMinoSDL.isEscapePushedThisFrame());
	}

	@Test
	void isEscapePushedThisFrameDetectsFreshEscapeKeyDownEvent() {
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(
				SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));

		assertTrue(NullpoMinoSDL.isEscapePushedThisFrame());
	}

	@Test
	void isEscapePushedThisFrameIgnoresKeyAutoRepeats() {
		// Auto-repeat events should not retrigger menu cancel — the user
		// pressed escape once, not held it.
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(
				SDLConstants.SDL_SCANCODE_ESCAPE, 0, true));

		assertFalse(NullpoMinoSDL.isEscapePushedThisFrame());
	}

	@Test
	void isEscapePushedThisFrameMatchesAcrossMultipleEventsInTheSameFrame() {
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(
				SDLConstants.SDL_SCANCODE_A, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(
				SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(new NullpoMinoSDL.KeyEvent(
				SDLConstants.SDL_SCANCODE_B, 0, false));

		assertTrue(NullpoMinoSDL.isEscapePushedThisFrame());
	}

	@Test
	void getUITextPrefersPropLangWhenKeyIsPresent() {
		NullpoMinoSDL.propLang = new CustomProperties();
		NullpoMinoSDL.propLangDefault = new CustomProperties();
		NullpoMinoSDL.propLang.setProperty("Title_Start", "JOUER");
		NullpoMinoSDL.propLangDefault.setProperty("Title_Start", "PLAY");

		assertEquals("JOUER", NullpoMinoSDL.getUIText("Title_Start"));
	}

	@Test
	void getUITextFallsBackToPropLangDefaultWhenKeyMissingFromOverride() {
		NullpoMinoSDL.propLang = new CustomProperties();
		NullpoMinoSDL.propLangDefault = new CustomProperties();
		NullpoMinoSDL.propLangDefault.setProperty("Title_Start", "PLAY");

		assertEquals("PLAY", NullpoMinoSDL.getUIText("Title_Start"));
	}

	@Test
	void getUITextReturnsTheKeyItselfWhenAbsentFromBothFiles() {
		NullpoMinoSDL.propLang = new CustomProperties();
		NullpoMinoSDL.propLangDefault = new CustomProperties();

		// Neither file knows the key — getUIText returns the lookup key
		// verbatim, so missing translations show up as the raw key in the
		// UI rather than as a blank label that hides the hole.
		assertEquals("Missing_Key", NullpoMinoSDL.getUIText("Missing_Key"));
	}
}
