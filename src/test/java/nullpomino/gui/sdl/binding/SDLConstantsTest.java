package nullpomino.gui.sdl.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SDLConstantsTest {

	@Test
	void mouseButtonMasksMatchOneShiftedByButtonIndexMinusOne() {
		assertEquals(1 << (SDLConstants.SDL_BUTTON_LEFT - 1), SDLConstants.SDL_BUTTON_LMASK);
		assertEquals(1 << (SDLConstants.SDL_BUTTON_MIDDLE - 1), SDLConstants.SDL_BUTTON_MMASK);
		assertEquals(1 << (SDLConstants.SDL_BUTTON_RIGHT - 1), SDLConstants.SDL_BUTTON_RMASK);
		assertEquals(1 << (SDLConstants.SDL_BUTTON_X1 - 1), SDLConstants.SDL_BUTTON_X1MASK);
		assertEquals(1 << (SDLConstants.SDL_BUTTON_X2 - 1), SDLConstants.SDL_BUTTON_X2MASK);
	}

	@Test
	void compositeKeymodsAreUnionsOfTheirLeftAndRightHalves() {
		assertEquals(
				SDLConstants.SDL_KMOD_LCTRL | SDLConstants.SDL_KMOD_RCTRL,
				SDLConstants.SDL_KMOD_CTRL);
		assertEquals(
				SDLConstants.SDL_KMOD_LSHIFT | SDLConstants.SDL_KMOD_RSHIFT,
				SDLConstants.SDL_KMOD_SHIFT);
		assertEquals(
				SDLConstants.SDL_KMOD_LALT | SDLConstants.SDL_KMOD_RALT,
				SDLConstants.SDL_KMOD_ALT);
	}

	@Test
	void scancodeNameTableLengthMatchesScancodeCount() {
		assertEquals(SDLConstants.SDL_SCANCODE_COUNT, SDLConstants.SCANCODE_NAMES.length);
	}

	@Test
	void scancodeNameTableNamesEveryLetterAndDigitSlot() {
		assertEquals("A", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_A]);
		assertEquals("M", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_M]);
		assertEquals("Z", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_Z]);
		assertEquals("0", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_0]);
		assertEquals("9", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_9]);
	}

	@Test
	void scancodeNameTableNamesNavigationAndEditingKeys() {
		assertEquals("RETURN", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_RETURN]);
		assertEquals("ESCAPE", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_ESCAPE]);
		assertEquals("BACKSPACE", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_BACKSPACE]);
		assertEquals("TAB", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_TAB]);
		assertEquals("SPACE", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_SPACE]);
		assertEquals("LEFT", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_LEFT]);
		assertEquals("RIGHT", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_RIGHT]);
		assertEquals("UP", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_UP]);
		assertEquals("DOWN", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_DOWN]);
		assertEquals("F1", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_F1]);
		assertEquals("F12", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_F12]);
	}

	@Test
	void scancodeNameTableNamesPunctuationAndKeypadKeys() {
		assertEquals("MINUS", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_MINUS]);
		assertEquals("EQUALS", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_EQUALS]);
		assertEquals("LEFTBRACKET", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_LEFTBRACKET]);
		assertEquals("BACKSLASH", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_BACKSLASH]);
		assertEquals("KP_DIVIDE", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_KP_DIVIDE]);
		assertEquals("KP_ENTER", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_KP_ENTER]);
		assertEquals("KP_0", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_KP_0]);
	}

	@Test
	void scancodeNameTableNamesModifierKeys() {
		assertEquals("LCTRL", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_LCTRL]);
		assertEquals("LSHIFT", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_LSHIFT]);
		assertEquals("LALT", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_LALT]);
		assertEquals("RCTRL", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_RCTRL]);
		assertEquals("RSHIFT", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_RSHIFT]);
		assertEquals("RALT", SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_RALT]);
	}

	@Test
	void unmappedScancodeSlotsFallBackToParenthesizedIndex() {
		// Slot 50 is unused (between BACKSLASH=49 and SEMICOLON=51).
		assertEquals("(50)", SDLConstants.SCANCODE_NAMES[50]);
		// Slot 70-72 are also unused (between F12=69 and INSERT=73).
		assertEquals("(70)", SDLConstants.SCANCODE_NAMES[70]);
		assertEquals("(72)", SDLConstants.SCANCODE_NAMES[72]);
		// And the very last slot in the table is unused too.
		assertEquals("(" + (SDLConstants.SDL_SCANCODE_COUNT - 1) + ")",
				SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_COUNT - 1]);
	}

	@Test
	void initFlagsAreDistinctNonZeroBits() {
		assertNotEquals(0, SDLConstants.SDL_INIT_AUDIO);
		assertNotEquals(0, SDLConstants.SDL_INIT_VIDEO);
		assertNotEquals(0, SDLConstants.SDL_INIT_JOYSTICK);
		assertEquals(0, SDLConstants.SDL_INIT_AUDIO & SDLConstants.SDL_INIT_VIDEO);
		assertEquals(0, SDLConstants.SDL_INIT_VIDEO & SDLConstants.SDL_INIT_JOYSTICK);
	}

	@Test
	void blendModesIncludeNoneAndBlend() {
		assertEquals(0, SDLConstants.SDL_BLENDMODE_NONE);
		assertEquals(1, SDLConstants.SDL_BLENDMODE_BLEND);
	}

	@Test
	void renderingModeValuesMatchSdl3Headers() {
		assertEquals(0, SDLConstants.SDL_SCALEMODE_NEAREST);
		assertEquals(2, SDLConstants.SDL_LOGICAL_PRESENTATION_LETTERBOX);
	}

	@Test
	void fullscreenWindowEventsMatchSdl3StableValues() {
		// Must equal the real SDL3-stable SDL_EventType values so the desktop
		// (JNA) path, which forwards native event.type verbatim, decodes them
		// correctly. Distinctness guards against a value collision that would
		// misfire the guarded resync branches in processEvent().
		assertEquals(0x217, SDLConstants.SDL_EVENT_WINDOW_ENTER_FULLSCREEN);
		assertEquals(0x218, SDLConstants.SDL_EVENT_WINDOW_LEAVE_FULLSCREEN);
		assertNotEquals(SDLConstants.SDL_EVENT_WINDOW_ENTER_FULLSCREEN,
				SDLConstants.SDL_EVENT_WINDOW_LEAVE_FULLSCREEN);
		assertNotEquals(SDLConstants.SDL_EVENT_WINDOW_ENTER_FULLSCREEN,
				SDLConstants.SDL_EVENT_WINDOW_CLOSE_REQUESTED);
		assertNotEquals(SDLConstants.SDL_EVENT_WINDOW_LEAVE_FULLSCREEN,
				SDLConstants.SDL_EVENT_KEY_DOWN);
	}
}
