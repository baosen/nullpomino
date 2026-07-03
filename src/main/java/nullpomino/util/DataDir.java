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
 * (working-directory-relative, as always). The browser build sets it because
 * CheerpJ does not resolve relative paths against user.dir at the I/O layer:
 * the persistent /files mount must be addressed with absolute paths.
 */
public final class DataDir {
	private static final String ROOT = System.getProperty("nullpomino.datadir", "");

	private DataDir() {}

	/** Resolve a data path. Absolute inputs pass through unchanged. */
	public static String path(String name) {
		if(ROOT.isEmpty() || new File(name).isAbsolute()) return name;
		return ROOT + "/" + name;
	}

	/** Resolve a data path as a File. */
	public static File file(String name) {
		return new File(path(name));
	}
}
