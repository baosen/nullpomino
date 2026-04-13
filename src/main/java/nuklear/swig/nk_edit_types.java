package nuklear.swig;

/** Compatibility shim — SWIG 4 generates nk_edit_flags; older code uses nk_edit_types */
public final class nk_edit_types {
	public final static int NK_EDIT_SIMPLE = nk_edit_flags.NK_EDIT_ALWAYS_INSERT_MODE;
	public final static int NK_EDIT_FIELD = nk_edit_flags.NK_EDIT_ALWAYS_INSERT_MODE | nk_edit_flags.NK_EDIT_SELECTABLE | nk_edit_flags.NK_EDIT_CLIPBOARD;
	public final static int NK_EDIT_BOX = nk_edit_flags.NK_EDIT_ALWAYS_INSERT_MODE | nk_edit_flags.NK_EDIT_SELECTABLE | nk_edit_flags.NK_EDIT_MULTILINE | nk_edit_flags.NK_EDIT_ALLOW_TAB | nk_edit_flags.NK_EDIT_CLIPBOARD;
	public final static int NK_EDIT_EDITOR = nk_edit_flags.NK_EDIT_SELECTABLE | nk_edit_flags.NK_EDIT_MULTILINE | nk_edit_flags.NK_EDIT_ALLOW_TAB | nk_edit_flags.NK_EDIT_CLIPBOARD;
}
