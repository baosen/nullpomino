package nullpomino.gui.sdl.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
	void eventNullTextReadsBackAsEmptyString() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		ev.text = null;

		assertEquals("", ev.getTextInputText());
	}

	@Test
	void eventAccessorsMirrorTheirFields() {
		SDLStructs.SDL_Event ev = new SDLStructs.SDL_Event();
		ev.type = SDLConstants.SDL_EVENT_KEY_DOWN;
		ev.scancode = SDLConstants.SDL_SCANCODE_A;
		ev.keycode = 0x61; // SDL_Keycode 'a'
		ev.keymod = SDLConstants.SDL_KMOD_LSHIFT;
		ev.keyDown = true;
		ev.keyRepeat = true;
		ev.text = "a";
		ev.editingStart = 7;
		ev.editingLength = 12;
		ev.wheelX = 1.25f;
		ev.wheelY = -2.5f;
		ev.windowData1 = 800;
		ev.windowData2 = 600;

		assertEquals(SDLConstants.SDL_EVENT_KEY_DOWN, ev.getType());
		assertEquals(SDLConstants.SDL_SCANCODE_A, ev.getScancode());
		assertEquals(0x61, ev.getKeycode());
		assertEquals(SDLConstants.SDL_KMOD_LSHIFT, ev.getKeymod());
		assertEquals(true, ev.isKeyDown());
		assertEquals(true, ev.isKeyRepeat());
		assertEquals("a", ev.getTextInputText());
		assertEquals(7, ev.getTextEditingStart());
		assertEquals(12, ev.getTextEditingLength());
		assertEquals(1.25f, ev.getMouseWheelX());
		assertEquals(-2.5f, ev.getMouseWheelY());
		assertEquals(800, ev.getWindowData1());
		assertEquals(600, ev.getWindowData2());
	}
}
