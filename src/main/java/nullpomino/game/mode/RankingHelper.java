// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

/**
 * Shared mechanics for mode-local ranking tables.
 */
final class RankingHelper {
	private RankingHelper() {
	}

	interface RankPredicate {
		boolean beats(int index);
	}

	interface EntryCopier {
		void copy(int toIndex, int fromIndex);
	}

	interface EntryWriter {
		void write(int index);
	}

	static int findRank(int maxRank, RankPredicate predicate) {
		for(int i = 0; i < maxRank; i++) {
			if(predicate.beats(i)) return i;
		}
		return -1;
	}

	static void insertAt(int rank, int maxRank, EntryCopier copier, EntryWriter writer) {
		if(rank == -1) return;

		for(int i = maxRank - 1; i > rank; i--) {
			copier.copy(i, i - 1);
		}
		writer.write(rank);
	}
}
