// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.tool;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Vector;

/**
 * Shared helpers for the legacy Swing tools.
 */
public final class SwingToolUtil {
	private SwingToolUtil() {
	}

	public static void ignoreOptionalFileLoad(IOException e) {
		// Optional user config and locale files are allowed to be absent.
	}

	public static void ignoreInvalidTextField(Exception e) {
		// Legacy text-field readers fall back to zero on malformed input.
	}

	/**
	 * Strip package prefixes from fully-qualified class names for compact
	 * combobox display.
	 */
	public static Vector<String> shortClassNames(List<String> names) {
		Vector<String> shortNames = new Vector<String>();

		for(String name : names) {
			shortNames.add(shortClassName(name));
		}

		return shortNames;
	}

	/**
	 * Strip the package prefix from a fully-qualified class name.
	 */
	public static String shortClassName(String name) {
		int last = name.lastIndexOf('.');

		if(last != -1) {
			return name.substring(last + 1);
		}
		return name;
	}

	/**
	 * Convert a local path to the file URL shape used by the Swing tools.
	 */
	public static URL fileUrl(String path) throws MalformedURLException {
		return new File(path).toURI().toURL();
	}
}
