package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Pins the null-mixer fallback paths in SoundManagerSDL. Tests run
 * without an initialised SDL3Mixer, so every method must short-
 * circuit gracefully — these guards are what lets the headless test
 * suite construct a manager just to satisfy ResourceHolderSDL.
 */
class SoundManagerSDLTest {

	@Test
	void constructorBuildsManagerWithoutMixerLibraryInitialised() {
		// No assertions — the test passes if construction does not throw
		// when NullpoMinoSDL.mixerLib / mixer are null.
		new SoundManagerSDL();
	}

	@Test
	void playReturnsSilentlyWhenMixerLibIsNull() {
		new SoundManagerSDL().play("missing");
	}

	@Test
	void changeVolumeReturnsSilentlyWhenMixerLibIsNull() {
		new SoundManagerSDL().changeVolume(64);
	}

	@Test
	void loadReturnsFalseWhenMixerLibIsNull() {
		assertFalse(new SoundManagerSDL().load("decide", "/tmp/missing.wav"));
	}

	@Test
	void destroyClearsClipMapEvenWhenMixerLibIsNull() {
		new SoundManagerSDL().destroy();
	}

	@Test
	void warmUpReturnsSilentlyWhenClipMapIsEmpty() {
		new SoundManagerSDL().warmUp();
	}
}
