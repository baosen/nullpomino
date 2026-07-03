package nullpomino.gui.sdl.binding;

/**
 * Simple mutable out-parameter holders, replacing JNA's ByReference types
 * in the backend-neutral binding interfaces. The {@code getValue()} accessor
 * matches JNA's naming so call sites only change the declared type.
 */
public final class Ref {
	private Ref() {}

	public static final class FloatRef {
		public float value;
		public float getValue() { return value; }
	}

	public static final class IntRef {
		public int value;
		public int getValue() { return value; }
	}
}
