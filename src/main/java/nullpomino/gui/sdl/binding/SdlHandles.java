package nullpomino.gui.sdl.binding;

/**
 * Opaque handle types for SDL objects, shared by all binding backends.
 *
 * These are deliberately empty marker interfaces: the desktop backend wraps
 * native pointers in them, the web backend wraps plain Java objects. Using
 * distinct reference types (rather than raw pointers or longs) keeps the two
 * backends impossible to cross-wire and preserves the identity comparisons
 * the frontend relies on (e.g. {@code trackAudio[i] == audio}).
 */
public final class SdlHandles {
	private SdlHandles() {}

	public interface SdlWindow {}
	public interface SdlRenderer {}
	public interface SdlTexture {}
	public interface SdlSurface {}
	public interface SdlFont {}
	public interface SdlGamepad {}
	public interface MixMixer {}
	public interface MixAudio {}
	public interface MixTrack {}
}
