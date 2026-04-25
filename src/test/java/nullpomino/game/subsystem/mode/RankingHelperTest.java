package nullpomino.game.subsystem.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RankingHelperTest {

	@Test
	void findRankUsesCallerTieBreakPredicate() {
		final int[] scores = {1000, 900, 900};
		final int[] lines = {40, 50, 40};
		final int[] times = {3600, 4000, 3000};
		final int score = 900;
		final int line = 50;
		final int time = 3500;

		int rank = RankingHelper.findRank(scores.length, new RankingHelper.RankPredicate() {
			public boolean beats(int i) {
				return score > scores[i]
					|| (score == scores[i] && line > lines[i])
					|| (score == scores[i] && line == lines[i] && time < times[i]);
			}
		});

		assertEquals(1, rank);
	}

	@Test
	void insertAtShiftsLowerEntriesAndWritesRecord() {
		final int[] scores = {1000, 900, 800};
		final int[] lines = {40, 35, 30};
		final int[] times = {3600, 3900, 4200};

		RankingHelper.insertAt(1, scores.length, new RankingHelper.EntryCopier() {
			public void copy(int toIndex, int fromIndex) {
				scores[toIndex] = scores[fromIndex];
				lines[toIndex] = lines[fromIndex];
				times[toIndex] = times[fromIndex];
			}
		}, new RankingHelper.EntryWriter() {
			public void write(int index) {
				scores[index] = 950;
				lines[index] = 37;
				times[index] = 3700;
			}
		});

		assertArrayEquals(new int[] {1000, 950, 900}, scores);
		assertArrayEquals(new int[] {40, 37, 35}, lines);
		assertArrayEquals(new int[] {3600, 3700, 3900}, times);
	}

	@Test
	void insertAtIgnoresUnrankedSentinel() {
		final int[] scores = {1000, 900, 800};

		RankingHelper.insertAt(-1, scores.length, new RankingHelper.EntryCopier() {
			public void copy(int toIndex, int fromIndex) {
				scores[toIndex] = -1;
			}
		}, new RankingHelper.EntryWriter() {
			public void write(int index) {
				scores[index] = -1;
			}
		});

		assertArrayEquals(new int[] {1000, 900, 800}, scores);
	}
}
