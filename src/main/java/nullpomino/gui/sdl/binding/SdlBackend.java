package nullpomino.gui.sdl.binding;

/**
 * Selects the SDL binding backend for the process.
 *
 * The default is the JNA-backed desktop backend, resolved by reflection so
 * this package (and everything compiled against it) has no compile-time
 * dependency on JNA. Alternative backends (the pure-Java2D web backend)
 * inject themselves via {@link #set} before any binding interface is touched,
 * which guarantees {@code com.sun.jna} classes are never initialized in
 * environments that cannot load native code.
 */
public final class SdlBackend {
	private SdlBackend() {}

	/** Factory for the per-library binding implementations of one backend. */
	public interface Backend {
		SDL3 sdl3();
		SDL3Image image();
		SDL3TTF ttf();
		/** @return the mixer binding, or null if audio is unavailable */
		SDL3Mixer mixerOrNull();
	}

	private static final String DEFAULT_BACKEND = "nullpomino.gui.sdl.binding.jna.JnaBackend";

	private static volatile Backend backend;

	/** Inject a backend. Must be called before any binding interface is initialized. */
	public static void set(Backend b) {
		backend = b;
	}

	public static Backend get() {
		Backend b = backend;
		if(b == null) {
			String cls = System.getProperty("nullpomino.sdl.backend", DEFAULT_BACKEND);
			try {
				b = (Backend) Class.forName(cls).getDeclaredConstructor().newInstance();
			} catch(ReflectiveOperationException e) {
				// IllegalStateException (not ExceptionInInitializerError) so this
				// reachable method stays free of a class TeaVM's classlib lacks;
				// the browser build injects its backend, so this path is dead there.
				throw new IllegalStateException("Cannot load SDL backend: " + cls, e);
			}
			backend = b;
		}
		return b;
	}
}
