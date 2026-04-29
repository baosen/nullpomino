package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatisticsRoundTripTest {

	@Test
	void exportImportRoundTripsAllStringFields() {
		Statistics original = populatedStatistics();

		Statistics imported = new Statistics(original.exportString());

		assertEquals(original.exportString(), imported.exportString());
		assertEquals(original.score, imported.score);
		assertEquals(original.spl, imported.spl);
		assertEquals(original.pps, imported.pps);
		assertEquals(original.rollclear, imported.rollclear);
	}

	@Test
	void exportStringArrayKeepsLegacyFieldCount() {
		assertEquals(38, new Statistics().exportStringArray().length);
	}

	@Test
	void importAcceptsLegacyThirtySevenFieldStringsWithoutRollclear() {
		Statistics original = populatedStatistics();
		String[] fields = original.exportStringArray();
		String[] legacyFields = new String[37];
		System.arraycopy(fields, 0, legacyFields, 0, legacyFields.length);

		Statistics imported = new Statistics(String.join(";", legacyFields));

		assertEquals(original.score, imported.score);
		assertEquals(original.maxChain, imported.maxChain);
		assertEquals(0, imported.rollclear);
	}

	private static Statistics populatedStatistics() {
		Statistics stats = new Statistics();
		stats.score = 100;
		stats.scoreFromLineClear = 101;
		stats.scoreFromSoftDrop = 102;
		stats.scoreFromHardDrop = 103;
		stats.scoreFromOtherBonus = 104;
		stats.lines = 5;
		stats.time = 6;
		stats.level = 7;
		stats.levelDispAdd = 8;
		stats.totalPieceLocked = 9;
		stats.totalPieceActiveTime = 10;
		stats.totalPieceMove = 11;
		stats.totalPieceRotate = 12;
		stats.totalSingle = 13;
		stats.totalDouble = 14;
		stats.totalTriple = 15;
		stats.totalFour = 16;
		stats.totalTSpinZeroMini = 17;
		stats.totalTSpinZero = 18;
		stats.totalTSpinSingleMini = 19;
		stats.totalTSpinSingle = 20;
		stats.totalTSpinDoubleMini = 21;
		stats.totalTSpinDouble = 22;
		stats.totalTSpinTriple = 23;
		stats.totalB2BFour = 24;
		stats.totalB2BTSpin = 25;
		stats.totalHoldUsed = 26;
		stats.maxCombo = 27;
		stats.spl = 1.25;
		stats.spm = 2.5;
		stats.sps = 3.75;
		stats.lpm = 4.25f;
		stats.lps = 5.5f;
		stats.ppm = 6.75f;
		stats.pps = 7.25f;
		stats.gamerate = 59.94f;
		stats.maxChain = 28;
		stats.rollclear = 1;
		return stats;
	}
}
