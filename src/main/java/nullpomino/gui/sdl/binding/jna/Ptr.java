package nullpomino.gui.sdl.binding.jna;

import com.sun.jna.Pointer;

import nullpomino.gui.sdl.binding.SdlHandles;

/**
 * Wraps a native pointer as every opaque handle type at once. The neutral
 * interfaces keep the handle types distinct, so a single wrapper class is
 * safe here and keeps the marshalling trivial.
 */
final class Ptr implements SdlHandles.SdlWindow, SdlHandles.SdlRenderer, SdlHandles.SdlTexture,
		SdlHandles.SdlSurface, SdlHandles.SdlFont, SdlHandles.SdlJoystick,
		SdlHandles.MixMixer, SdlHandles.MixAudio, SdlHandles.MixTrack {

	final Pointer p;

	private Ptr(Pointer p) {
		this.p = p;
	}

	/** Wrap a native pointer, mapping native null to Java null. */
	static Ptr wrap(Pointer p) {
		return p == null ? null : new Ptr(p);
	}

	/** Unwrap a handle back to its native pointer (null-safe). */
	static Pointer p(Object handle) {
		return handle == null ? null : ((Ptr) handle).p;
	}
}
