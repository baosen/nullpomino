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
			try {
				// Probe the audio system once; if unavailable (headless JVM,
				// browser without audio), the game runs silent via its
				// existing mixer-null guards.
				javax.sound.sampled.AudioSystem.getMixerInfo();
				mixer = new WebSDL3Mixer();
			} catch(Exception | Error e) {
				mixer = null;
			}
		}
		return mixer;
	}
}
