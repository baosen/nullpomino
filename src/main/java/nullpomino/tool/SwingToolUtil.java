// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.tool;

import java.util.List;
import java.util.Vector;

/**
 * Shared helpers for the legacy Swing tools.
 */
public final class SwingToolUtil {
	private SwingToolUtil() {
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
}
