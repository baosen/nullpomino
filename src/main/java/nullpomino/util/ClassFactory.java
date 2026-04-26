// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

/**
 * Typed construction for class names loaded from config, rules, and replays.
 */
public final class ClassFactory {
	private ClassFactory() {
	}

	public static <T> T create(String className, Class<T> expectedType) throws ReflectiveOperationException {
		return Class.forName(LegacyClassNames.translate(className))
				.asSubclass(expectedType)
				.getDeclaredConstructor()
				.newInstance();
	}
}
