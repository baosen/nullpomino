package nuklear.swig;

/** Compatibility shim — SWIG 4 generates nk_text_align; older code uses nk_text_alignment */
public final class nk_text_alignment {
	public final static int NK_TEXT_LEFT = nk_text_align.NK_TEXT_ALIGN_LEFT | nk_text_align.NK_TEXT_ALIGN_MIDDLE;
	public final static int NK_TEXT_CENTERED = nk_text_align.NK_TEXT_ALIGN_CENTERED | nk_text_align.NK_TEXT_ALIGN_MIDDLE;
	public final static int NK_TEXT_RIGHT = nk_text_align.NK_TEXT_ALIGN_RIGHT | nk_text_align.NK_TEXT_ALIGN_MIDDLE;
}
