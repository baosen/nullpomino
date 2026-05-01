package nullpomino.gui.sdl.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SDLStructsTest {

	@Test
	void fRectFieldInitConstructorAssignsAllFour() {
		SDLStructs.SDL_FRect r = new SDLStructs.SDL_FRect(1f, 2f, 3f, 4f);

		assertEquals(1f, r.x);
		assertEquals(2f, r.y);
		assertEquals(3f, r.w);
		assertEquals(4f, r.h);
	}

	@Test
	void fRectDefaultConstructorZeroesAllFields() {
		SDLStructs.SDL_FRect r = new SDLStructs.SDL_FRect();

		assertEquals(0f, r.x);
		assertEquals(0f, r.y);
		assertEquals(0f, r.w);
		assertEquals(0f, r.h);
	}

	@Test
	void fRectFieldOrderListsXYWH() {
		assertEquals(java.util.Arrays.asList("x", "y", "w", "h"),
				new SDLStructs.SDL_FRect().getFieldOrder());
	}

	@Test
	void fRectByValueAndByReferenceCanBeConstructedWithoutCrash() {
		SDLStructs.SDL_FRect.ByReference ref = new SDLStructs.SDL_FRect.ByReference();
		assertNotNull(ref);

		SDLStructs.SDL_FRect.ByValue val = new SDLStructs.SDL_FRect.ByValue(5f, 6f, 7f, 8f);
		assertEquals(5f, val.x);
		assertEquals(6f, val.y);
		assertEquals(7f, val.w);
		assertEquals(8f, val.h);

		SDLStructs.SDL_FRect.ByValue empty = new SDLStructs.SDL_FRect.ByValue();
		assertEquals(0f, empty.x);
	}

	@Test
	void rectFieldInitConstructorAssignsAllFour() {
		SDLStructs.SDL_Rect r = new SDLStructs.SDL_Rect(10, 20, 30, 40);

		assertEquals(10, r.x);
		assertEquals(20, r.y);
		assertEquals(30, r.w);
		assertEquals(40, r.h);
	}

	@Test
	void rectDefaultConstructorZeroesAllFields() {
		SDLStructs.SDL_Rect r = new SDLStructs.SDL_Rect();

		assertEquals(0, r.x);
		assertEquals(0, r.y);
		assertEquals(0, r.w);
		assertEquals(0, r.h);
	}

	@Test
	void rectFieldOrderListsXYWH() {
		assertEquals(java.util.Arrays.asList("x", "y", "w", "h"),
				new SDLStructs.SDL_Rect().getFieldOrder());
	}

	@Test
	void rectByReferenceCanBeConstructed() {
		SDLStructs.SDL_Rect.ByReference ref = new SDLStructs.SDL_Rect.ByReference();
		assertNotNull(ref);
	}

	@Test
	void audioSpecFieldInitConstructorAssignsFormatChannelsAndFreq() {
		SDLStructs.SDL_AudioSpec spec = new SDLStructs.SDL_AudioSpec(0x8010, 2, 44100);

		assertEquals(0x8010, spec.format);
		assertEquals(2, spec.channels);
		assertEquals(44100, spec.freq);
	}

	@Test
	void audioSpecDefaultConstructorZeroesAllFields() {
		SDLStructs.SDL_AudioSpec spec = new SDLStructs.SDL_AudioSpec();

		assertEquals(0, spec.format);
		assertEquals(0, spec.channels);
		assertEquals(0, spec.freq);
	}

	@Test
	void audioSpecFieldOrderListsFormatChannelsFreq() {
		assertEquals(java.util.Arrays.asList("format", "channels", "freq"),
				new SDLStructs.SDL_AudioSpec().getFieldOrder());
	}

	@Test
	void colorRgbConstructorAssumesFullyOpaqueAlpha() {
		SDLStructs.SDL_Color c = new SDLStructs.SDL_Color(10, 20, 30);

		assertEquals((byte) 10, c.r);
		assertEquals((byte) 20, c.g);
		assertEquals((byte) 30, c.b);
		assertEquals((byte) 255, c.a);
	}

	@Test
	void colorRgbaConstructorAssignsAllFourComponents() {
		SDLStructs.SDL_Color c = new SDLStructs.SDL_Color(1, 2, 3, 4);

		assertEquals((byte) 1, c.r);
		assertEquals((byte) 2, c.g);
		assertEquals((byte) 3, c.b);
		assertEquals((byte) 4, c.a);
	}

	@Test
	void colorByteFieldsTruncateValuesAboveOneTwentySeven() {
		SDLStructs.SDL_Color c = new SDLStructs.SDL_Color(255, 200, 128, 64);

		assertEquals((byte) 0xFF, c.r);
		assertEquals((byte) 200, c.g);
		assertEquals((byte) 128, c.b);
		assertEquals((byte) 64, c.a);
	}

	@Test
	void colorFieldOrderListsRGBA() {
		assertEquals(java.util.Arrays.asList("r", "g", "b", "a"),
				new SDLStructs.SDL_Color().getFieldOrder());
	}

	@Test
	void colorByValueConstructorsMatchTheParentConstructors() {
		SDLStructs.SDL_Color.ByValue empty = new SDLStructs.SDL_Color.ByValue();
		assertEquals((byte) 0, empty.r);

		SDLStructs.SDL_Color.ByValue rgb = new SDLStructs.SDL_Color.ByValue(1, 2, 3);
		assertEquals((byte) 1, rgb.r);
		assertEquals((byte) 255, rgb.a);

		SDLStructs.SDL_Color.ByValue rgba = new SDLStructs.SDL_Color.ByValue(10, 20, 30, 40);
		assertEquals((byte) 40, rgba.a);
	}

	@Test
	void eventDefaultStateReturnsZeroForEveryAccessor() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();

		assertNotNull(ev.getPointer());
		assertEquals(0, ev.getType());
		assertEquals(0, ev.getScancode());
		assertEquals(0, ev.getKeycode());
		assertEquals(0, ev.getKeymod());
		assertFalse(ev.isKeyDown());
		assertFalse(ev.isKeyRepeat());
		assertEquals(0, ev.getTextEditingStart());
		assertEquals(0, ev.getTextEditingLength());
		assertEquals(0f, ev.getMouseWheelX());
		assertEquals(0f, ev.getMouseWheelY());
		assertEquals(0, ev.getWindowData1());
		assertEquals(0, ev.getWindowData2());
		assertEquals("", ev.getTextInputText());
	}

	@Test
	void eventKeyboardAccessorsReadFromTheirDocumentedOffsets() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		ev.getPointer().setInt(0, SDLConstants.SDL_EVENT_KEY_DOWN);
		ev.getPointer().setInt(24, SDLConstants.SDL_SCANCODE_A);
		ev.getPointer().setInt(28, 0x61); // SDL_Keycode 'a'
		ev.getPointer().setShort(32, (short) SDLConstants.SDL_KMOD_LSHIFT);
		ev.getPointer().setByte(36, (byte) 1);
		ev.getPointer().setByte(37, (byte) 1);

		assertEquals(SDLConstants.SDL_EVENT_KEY_DOWN, ev.getType());
		assertEquals(SDLConstants.SDL_SCANCODE_A, ev.getScancode());
		assertEquals(0x61, ev.getKeycode());
		assertEquals(SDLConstants.SDL_KMOD_LSHIFT, ev.getKeymod());
		assertTrue(ev.isKeyDown());
		assertTrue(ev.isKeyRepeat());
	}

	@Test
	void eventKeymodIsReadAsUnsignedSixteenBit() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		ev.getPointer().setShort(32, (short) 0xFFFF);

		// A naive `int` widening of a signed short would yield -1, so the
		// `& 0xFFFF` mask in getKeymod() is load-bearing.
		assertEquals(0xFFFF, ev.getKeymod());
	}

	@Test
	void eventTextInputReadsBackZeroPointerAsEmptyString() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		// A null pointer at offset 24 is the cleared/default state.

		assertEquals("", ev.getTextInputText());
	}

	@Test
	void eventTextEditingFieldsReadFromOffsetsThirtyTwoAndThirtySix() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		ev.getPointer().setInt(32, 7);
		ev.getPointer().setInt(36, 12);

		assertEquals(7, ev.getTextEditingStart());
		assertEquals(12, ev.getTextEditingLength());
	}

	@Test
	void eventMouseWheelAccessorsReadFloatsFromOffsetsTwentyFourAndTwentyEight() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		ev.getPointer().setFloat(24, 1.25f);
		ev.getPointer().setFloat(28, -2.5f);

		assertEquals(1.25f, ev.getMouseWheelX());
		assertEquals(-2.5f, ev.getMouseWheelY());
	}

	@Test
	void eventWindowDataAccessorsReadFromOffsetsTwentyAndTwentyFour() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		ev.getPointer().setInt(20, 800);
		ev.getPointer().setInt(24, 600);

		assertEquals(800, ev.getWindowData1());
		assertEquals(600, ev.getWindowData2());
	}
}
