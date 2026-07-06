package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gaps in {@link AbstractManiaMode#checkRanking}
 * (L196-200): the tie-breaking chain where roll-clear and grade match and the
 * comparison falls through to level and then time.
 *
 * <p>The ranking arrays are set directly on a local subclass instance, so no
 * ranking state is loaded from (or written to) the shared config directory.
 */
class AbstractManiaModeBranchGapTest {

	private static final class ManiaGapMode extends AbstractManiaMode {
	}

	/** Entry 0: rollclear=1, grade=5, level=5, time=1000; entries 1-9 all zero. */
	private static ManiaGapMode modeWithPresetRanking() {
		ManiaGapMode mode = new ManiaGapMode();
		mode.rankingGrade = new int[10];
		mode.rankingLevel = new int[10];
		mode.rankingTime = new int[10];
		mode.rankingRollclear = new int[10];
		mode.rankingGrade[0] = 5;
		mode.rankingLevel[0] = 5;
		mode.rankingTime[0] = 1000;
		mode.rankingRollclear[0] = 1;
		return mode;
	}

	@Test
	void sameClearAndGradeButHigherLevelOutranksEntry() {
		ManiaGapMode mode = modeWithPresetRanking();
		// clear ==, grade ==, level > -> third clause true.
		assertEquals(0, mode.checkRanking(5, 6, 9999, 1));
	}

	@Test
	void fullTieWithBetterTimeOutranksEntry() {
		ManiaGapMode mode = modeWithPresetRanking();
		// clear ==, grade ==, level ==, time < -> fourth clause true.
		assertEquals(0, mode.checkRanking(5, 5, 500, 1));
	}

	@Test
	void fullTieWithWorseTimeFallsPastEntry() {
		ManiaGapMode mode = modeWithPresetRanking();
		// Entry 0 wins the time tie-break; entry 1 (all zero) is beaten on clear.
		assertEquals(1, mode.checkRanking(5, 5, 2000, 1));
	}

	@Test
	void higherGradeAtSameClearOutranksEntry() {
		ManiaGapMode mode = modeWithPresetRanking();
		assertEquals(0, mode.checkRanking(6, 0, 0, 1));
	}

	@Test
	void lowerGradeAtSameClearFallsPastEntry() {
		ManiaGapMode mode = modeWithPresetRanking();
		// grade 4 < 5: every clause false at entry 0, wins at entry 1 on clear.
		assertEquals(1, mode.checkRanking(4, 9, 0, 1));
	}

	@Test
	void higherClearOutranksEntry() {
		ManiaGapMode mode = modeWithPresetRanking();
		assertEquals(0, mode.checkRanking(0, 0, 0, 2));
	}

	@Test
	void allZeroCandidateIsUnranked() {
		ManiaGapMode mode = modeWithPresetRanking();
		// Ties every zero entry on clear/grade/level but never beats the time.
		assertEquals(-1, mode.checkRanking(0, 0, 0, 0));
	}
}
