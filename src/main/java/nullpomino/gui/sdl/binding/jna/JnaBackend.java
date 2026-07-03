package nullpomino.gui.sdl.binding.jna;

import com.sun.jna.Native;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SdlBackend;

/**
 * The desktop backend: loads the native SDL3 libraries through JNA.
 * This is the only package that touches {@code com.sun.jna}.
 */
public final class JnaBackend implements SdlBackend.Backend {

	private SDL3 sdl3;
	private SDL3Image image;
	private SDL3TTF ttf;
	private SDL3Mixer mixer;
	private boolean mixerProbed;

	public JnaBackend() {}

	@Override public synchronized SDL3 sdl3() {
		if(sdl3 == null) {
			sdl3 = new JnaSDL3(Native.load("SDL3", RawSDL3.class));
		}
		return sdl3;
	}

	@Override public synchronized SDL3Image image() {
		if(image == null) {
			image = new JnaSDL3Image(Native.load("SDL3_image", RawSDL3Image.class));
		}
		return image;
	}

	@Override public synchronized SDL3TTF ttf() {
		if(ttf == null) {
			ttf = new JnaSDL3TTF(Native.load("SDL3_ttf", RawSDL3TTF.class));
		}
		return ttf;
	}

	@Override public synchronized SDL3Mixer mixerOrNull() {
		if(!mixerProbed) {
			mixerProbed = true;
			try {
				mixer = new JnaSDL3Mixer(Native.load("SDL3_mixer", RawSDL3Mixer.class));
			} catch(UnsatisfiedLinkError e) {
				mixer = null;
			}
		}
		return mixer;
	}
}
