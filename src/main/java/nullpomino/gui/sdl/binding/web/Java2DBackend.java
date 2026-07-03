package nullpomino.gui.sdl.binding.web;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SdlBackend;

/**
 * The pure-Java backend: AWT/Swing/Java2D rendering, ImageIO images, AWT
 * fonts, and javax.sound audio. No native code, so it also runs under
 * CheerpJ in a browser. Inject with {@code SdlBackend.set(new Java2DBackend())}
 * before any binding interface is initialized.
 */
public final class Java2DBackend implements SdlBackend.Backend {

	private final WebSDL3 sdl3 = new WebSDL3();
	private final WebSDL3Image image = new WebSDL3Image(sdl3);
	private final WebSDL3TTF ttf = new WebSDL3TTF(sdl3);

	private SDL3Mixer mixer;
	private boolean mixerProbed;

	@Override public SDL3 sdl3() { return sdl3; }
	@Override public SDL3Image image() { return image; }
	@Override public SDL3TTF ttf() { return ttf; }

	@Override public synchronized SDL3Mixer mixerOrNull() {
		if(!mixerProbed) {
			mixerProbed = true;
			mixer = probeMixer();
		}
		return mixer;
	}

	/**
	 * Pick an audio backend, or null to run silent (the game's existing
	 * mixer-null guards handle that gracefully).
	 *
	 * Under CheerpJ the page provides a Web Audio bridge via native methods
	 * (javax.sound has no output devices there at all); on a desktop JVM the
	 * natives are unlinked and javax.sound is used instead.
	 */
	private static SDL3Mixer probeMixer() {
		try {
			if(BrowserAudio.jsAudioInit() != 0) {
				System.out.println("Java2DBackend: using Web Audio bridge");
				return new JsSDL3Mixer();
			}
		} catch(Exception | Error e) {
			// Natives not provided (desktop JVM) — try javax.sound.
		}
		try {
			if(javax.sound.sampled.AudioSystem.getMixerInfo().length > 0) {
				System.out.println("Java2DBackend: using javax.sound audio");
				return new WebSDL3Mixer();
			}
		} catch(Exception | Error e) {
			// Fall through to silent.
		}
		System.out.println("Java2DBackend: no audio backend available — running silent");
		return null;
	}
}
