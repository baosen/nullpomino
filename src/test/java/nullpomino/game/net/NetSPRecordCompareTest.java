package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Statistics;

import org.junit.jupiter.api.Test;

class NetSPRecordCompareTest {

	@Test
	void genericScoreRanksScoreLinesThenLowerTime() {
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, stats(100, 0, 0), stats(99, 999, 0));
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, stats(100, 2, 10), stats(100, 1, 1));
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, stats(100, 2, 9), stats(100, 2, 10));
	}

	@Test
	void genericTimeRanksLowerTimeLowerPiecesThenHigherPps() {
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_TIME, stats().time(99), stats().time(100));
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_TIME, stats().time(100).pieces(9), stats().time(100).pieces(10));
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_TIME, stats().time(100).pieces(10).pps(2.0f),
				stats().time(100).pieces(10).pps(1.0f));
	}

	@Test
	void scoreRaceRanksLowerTimeLowerLinesThenHigherSpl() {
		assertBeats(NetSPRecord.RANKINGTYPE_SCORERACE, stats().time(99), stats().time(100));
		assertBeats(NetSPRecord.RANKINGTYPE_SCORERACE, stats().time(100).lines(1), stats().time(100).lines(2));
		assertBeats(NetSPRecord.RANKINGTYPE_SCORERACE, stats().time(100).lines(2).spl(2.0),
				stats().time(100).lines(2).spl(1.0));
	}

	@Test
	void digRaceRanksLowerTimeLowerLinesThenLowerPieces() {
		assertBeats(NetSPRecord.RANKINGTYPE_DIGRACE, stats().time(99), stats().time(100));
		assertBeats(NetSPRecord.RANKINGTYPE_DIGRACE, stats().time(100).lines(1), stats().time(100).lines(2));
		assertBeats(NetSPRecord.RANKINGTYPE_DIGRACE, stats().time(100).lines(2).pieces(9),
				stats().time(100).lines(2).pieces(10));
	}

	@Test
	void ultraRanksScoreLinesThenLowerPieces() {
		assertBeats(NetSPRecord.RANKINGTYPE_ULTRA, stats(100, 0, 0), stats(99, 999, 0));
		assertBeats(NetSPRecord.RANKINGTYPE_ULTRA, stats(100, 2, 0), stats(100, 1, 0));
		assertBeats(NetSPRecord.RANKINGTYPE_ULTRA, stats(100, 2, 0).pieces(9), stats(100, 2, 0).pieces(10));
	}

	@Test
	void comboRaceRanksComboLowerTimeThenHigherPps() {
		assertBeats(NetSPRecord.RANKINGTYPE_COMBORACE, stats().combo(10), stats().combo(9));
		assertBeats(NetSPRecord.RANKINGTYPE_COMBORACE, stats().combo(10).time(99), stats().combo(10).time(100));
		assertBeats(NetSPRecord.RANKINGTYPE_COMBORACE, stats().combo(10).time(100).pps(2.0f),
				stats().combo(10).time(100).pps(1.0f));
	}

	@Test
	void digChallengeRanksScoreLinesThenHigherTime() {
		assertBeats(NetSPRecord.RANKINGTYPE_DIGCHALLENGE, stats(100, 0, 0), stats(99, 999, 999));
		assertBeats(NetSPRecord.RANKINGTYPE_DIGCHALLENGE, stats(100, 2, 0), stats(100, 1, 999));
		assertBeats(NetSPRecord.RANKINGTYPE_DIGCHALLENGE, stats(100, 2, 11), stats(100, 2, 10));
	}

	@Test
	void timeAttackRanksRollClearCappedLinesLowerTimeThenHigherPps() {
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK, stats().rollclear(1), stats().rollclear(0));
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK, stats().rollclear(1).lines(150),
				stats().rollclear(1).lines(149));
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK, stats().rollclear(1).lines(200).time(99),
				stats().rollclear(1).lines(250).time(100));
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK, stats().rollclear(1).lines(150).time(100).pps(2.0f),
				stats().rollclear(1).lines(150).time(100).pps(1.0f));
	}

	@Test
	void unknownAndEqualRecordsDoNotBeat() {
		assertFalse(compare(999, stats(1, 2, 3), stats(1, 2, 3)));
		assertFalse(compare(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, stats(1, 2, 3), stats(1, 2, 3)));
	}

	private static void assertBeats(int type, StatBuilder better, StatBuilder worse) {
		assertTrue(compare(type, better, worse));
		assertFalse(compare(type, worse, better));
	}

	private static boolean compare(int type, StatBuilder first, StatBuilder second) {
		NetSPRecord r1 = new NetSPRecord();
		r1.gameType = first.gameType;
		r1.stats = first.stats;
		NetSPRecord r2 = new NetSPRecord();
		r2.gameType = second.gameType;
		r2.stats = second.stats;
		return NetSPRecord.compareRecords(type, r1, r2);
	}

	private static StatBuilder stats() {
		return new StatBuilder();
	}

	private static StatBuilder stats(int score, int lines, int time) {
		return stats().score(score).lines(lines).time(time);
	}

	private static final class StatBuilder {
		final Statistics stats = new Statistics();
		int gameType;

		StatBuilder score(int value) {
			stats.score = value;
			return this;
		}

		StatBuilder lines(int value) {
			stats.lines = value;
			return this;
		}

		StatBuilder time(int value) {
			stats.time = value;
			return this;
		}

		StatBuilder pieces(int value) {
			stats.totalPieceLocked = value;
			return this;
		}

		StatBuilder pps(float value) {
			stats.pps = value;
			return this;
		}

		StatBuilder spl(double value) {
			stats.spl = value;
			return this;
		}

		StatBuilder combo(int value) {
			stats.maxCombo = value;
			return this;
		}

		StatBuilder rollclear(int value) {
			stats.rollclear = value;
			return this;
		}
	}
}
