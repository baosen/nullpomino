// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Typed construction for class names loaded from config, rules, and replays.
 *
 * <p>Two resolution strategies, tried in order:
 * <ol>
 *   <li>an explicit registry of constructor suppliers, keyed by canonical
 *       (post-{@link LegacyClassNames#translate translate}) class name; and</li>
 *   <li>reflective {@code Class.forName}.</li>
 * </ol>
 * The registry exists for the browser (TeaVM) target, where ahead-of-time
 * compilation dead-code-eliminates any class only reached through
 * {@code Class.forName}. The desktop build leaves the registry empty and
 * relies entirely on reflection, so its behavior is unchanged.
 */
public final class ClassFactory {
	private static final Map<String, Supplier<?>> REGISTRY = new ConcurrentHashMap<>();

	private ClassFactory() {
	}

	/**
	 * Register a reflection-free constructor for a canonical class name (the
	 * name after {@link LegacyClassNames#translate}). Later registrations for
	 * the same name win.
	 */
	public static void register(String className, Supplier<?> constructor) {
		REGISTRY.put(className, constructor);
	}

	public static <T> T create(String className, Class<T> expectedType) throws ReflectiveOperationException {
		String translated = LegacyClassNames.translate(className);
		Supplier<?> supplier = REGISTRY.get(translated);
		if (supplier != null) {
			return expectedType.cast(supplier.get());
		}
		return Class.forName(translated)
				.asSubclass(expectedType)
				.getDeclaredConstructor()
				.newInstance();
	}
}
