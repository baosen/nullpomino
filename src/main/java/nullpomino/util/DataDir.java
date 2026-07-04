// SPDX-FileCopyrightText: 2026 baosen
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.io.File;

/**
 * Resolves game data paths (config/, replay/, ss/) against an optional
 * absolute data directory given by the system property
 * {@code nullpomino.datadir}.
 *
 * On desktop the property is unset and paths pass through untouched
 * (working-directory-relative, as always). The browser build sets a root
 * because relative paths there address a synthetic root rather than the
 * process working directory; anchoring data I/O at an explicit absolute
 * root keeps config/replays in one predictable place.
 */
public final class DataDir {
	private static String root = System.getProperty("nullpomino.datadir", "");

	private DataDir() {}

	/**
	 * Override the data root. The browser entry point calls this directly
	 * (the TeaVM target has no {@code -D} system properties at launch).
	 */
	public static void setRoot(String newRoot) {
		root = newRoot == null ? "" : newRoot;
	}

	/** Resolve a data path. Absolute inputs pass through unchanged. */
	public static String path(String name) {
		if(root.isEmpty() || new File(name).isAbsolute()) return name;
		return root + "/" + name;
	}

	/** Resolve a data path as a File. */
	public static File file(String name) {
		return new File(path(name));
	}
}
