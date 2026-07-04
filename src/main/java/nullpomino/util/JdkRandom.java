// SPDX-FileCopyrightText: 2026 baosen
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.util;

import java.util.Random;

/**
 * Drop-in replacement for {@link Random} that fixes {@link #nextInt(int)}
 * under TeaVM.
 *
 * TeaVM's classlib (0.14.1+) correctly seeds and advances the LCG core
 * ({@code setSeed}/{@code next(int)}/{@code nextLong}/{@code nextFloat} all
 * match the JDK), but its {@code Random} shim never overrides
 * {@code nextInt(int bound)} — it falls through to the {@code
 * RandomGenerator} interface's modern default (mask against the next power
 * of two, reject out-of-range draws) instead of {@code java.util.Random}'s
 * own historically-preserved legacy override ({@code next(31)} plus
 * modulo-with-rejection). That makes bounded-random sequences diverge from
 * the JDK for the same seed on TeaVM, even though the seed itself now
 * works. This reimplements that legacy algorithm using the (correctly
 * seed-driven) inherited {@link #next(int)}.
 *
 * On a real JVM (desktop, CheerpJ) this is byte-for-byte what {@code
 * Random.nextInt(int)} already does internally, so it's safe for
 * unconditional use on every platform, not just the TeaVM web build.
 */
public final class JdkRandom extends Random {
	private static final long serialVersionUID = 1L;

	public JdkRandom(long seed) {
		super(seed);
	}

	@Override
	public int nextInt(int bound) {
		if (bound <= 0) {
			throw new IllegalArgumentException("bound must be positive");
		}
		int r = next(31);
		int m = bound - 1;
		if ((bound & m) == 0) { // power of 2
			r = (int) ((bound * (long) r) >> 31);
		} else {
			for (int u = r; u - (r = u % bound) + m < 0; u = next(31)) {
				// retry: rejection sampling to avoid modulo bias
			}
		}
		return r;
	}
}
