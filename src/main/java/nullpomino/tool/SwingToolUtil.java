// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;

/**
 * Shared helpers for the legacy Swing tools.
 */
public final class SwingToolUtil {
	private SwingToolUtil() {
	}

	/**
	 * Read a text list into a Vector, preserving the legacy behavior of
	 * stopping at the first missing or empty line.
	 */
	public static Vector<String> readNonEmptyLines(String filename) {
		Vector<String> lines = new Vector<String>();

		try (BufferedReader in = new BufferedReader(new FileReader(filename))) {
			String str;
			while((str = in.readLine()) != null) {
				if(str.length() <= 0) break;
				lines.add(str);
			}
		} catch(IOException e) {
			ignoreListReadFailure(e);
		}

		return lines;
	}

	private static void ignoreListReadFailure(IOException e) {
		// Legacy Swing tools treat missing optional list files as empty lists.
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
	public static Vector<String> shortClassNames(Vector<String> names) {
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
