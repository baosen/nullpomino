package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

class StatisticsTest {

	@Test
	void defaultConstructorAppliesZeroValues() {
		Statistics s = new Statistics();

		assertEquals(0, s.score);
		assertEquals(0, s.lines);
		assertEquals(0, s.time);
		assertEquals(0, s.level);
		assertEquals(0.0, s.spl);
		assertEquals(0.0, s.spm);
		assertEquals(0f, s.lpm);
		assertEquals(0, s.maxChain);
		assertEquals(0, s.rollclear);
	}

	@Test
	void resetClearsEveryFieldAfterMutation() {
		Statistics s = populated();

		s.reset();

		assertEquals(0, s.score);
		assertEquals(0, s.lines);
		assertEquals(0, s.time);
		assertEquals(0.0, s.spl);
		assertEquals(0f, s.gamerate);
		assertEquals(0, s.rollclear);
	}

	@Test
	void copyConstructorReplicatesAllFieldsIndependently() {
		Statistics src = populated();

		Statistics dst = new Statistics(src);

		assertNotSame(src, dst);
		assertEquals(src.exportString(), dst.exportString());

		src.score = 0;
		assertEquals(100, dst.score);
	}

	@Test
	void copyOverwritesEveryField() {
		Statistics src = populated();
		Statistics dst = new Statistics();

		dst.copy(src);

		assertEquals(src.exportString(), dst.exportString());
	}

	@Test
	void stringArrayConstructorImportsAllFields() {
		Statistics src = populated();
		String[] fields = src.exportStringArray();

		Statistics imported = new Statistics(fields);

		assertEquals(src.exportString(), imported.exportString());
	}

	@Test
	void updateLeavesDerivedFieldsZeroWhenLinesAndTimeAreZero() {
		Statistics s = new Statistics();
		s.score = 9999;
		s.totalPieceLocked = 50;

		s.update();

		assertEquals(0.0, s.spl);
		assertEquals(0.0, s.spm);
		assertEquals(0.0, s.sps);
		assertEquals(0f, s.lpm);
		assertEquals(0f, s.lps);
		assertEquals(0f, s.ppm);
		assertEquals(0f, s.pps);
	}

	@Test
	void updateComputesPerLineRateOnlyWhenLinesNonZeroAndTimeZero() {
		Statistics s = new Statistics();
		s.score = 1200;
		s.lines = 4;
		s.time = 0;

		s.update();

		assertEquals(300.0, s.spl);
		assertEquals(0.0, s.spm);
		assertEquals(0f, s.lpm);
	}

	@Test
	void updateComputesAllRatesWhenLinesAndTimeAreNonZero() {
		Statistics s = new Statistics();
		s.score = 1200;
		s.lines = 4;
		s.time = 60;
		s.totalPieceLocked = 10;

		s.update();

		assertEquals(300.0, s.spl);
		assertEquals(1200.0 * 3600.0 / 60.0, s.spm);
		assertEquals(1200.0 * 60.0 / 60.0, s.sps);
		assertEquals(4 * 3600f / 60f, s.lpm);
		assertEquals(4 * 60f / 60f, s.lps);
		assertEquals(10 * 3600f / 60f, s.ppm);
		assertEquals(10 * 60f / 60f, s.pps);
	}

	@Test
	void writePropertyAndReadPropertyRoundTrip() {
		Statistics src = populated();
		CustomProperties p = new CustomProperties();

		src.writeProperty(p, 1);
		Statistics imported = new Statistics();
		imported.readProperty(p, 1);

		assertEquals(src.exportString(), imported.exportString());
	}

	@Test
	void writePropertyForPlayerZeroAlsoEmitsLegacyResultKeys() {
		Statistics src = populated();
		CustomProperties p = new CustomProperties();

		src.writeProperty(p, 0);

		assertEquals(src.score, p.getProperty("result.score", -1));
		assertEquals(src.lines, p.getProperty("result.totallines", -1));
		assertEquals(src.level, p.getProperty("result.level", -1));
		assertEquals(src.time, p.getProperty("result.time", -1));
	}

	@Test
	void writePropertyForPlayerOneOmitsLegacyResultKeys() {
		Statistics src = populated();
		CustomProperties p = new CustomProperties();

		src.writeProperty(p, 1);

		assertEquals(-1, p.getProperty("result.score", -1));
		assertEquals(-1, p.getProperty("result.totallines", -1));
	}

	private static Statistics populated() {
		Statistics s = new Statistics();
		s.score = 100;
		s.scoreFromLineClear = 101;
		s.scoreFromSoftDrop = 102;
		s.scoreFromHardDrop = 103;
		s.scoreFromOtherBonus = 104;
		s.lines = 5;
		s.time = 6;
		s.level = 7;
		s.levelDispAdd = 8;
		s.totalPieceLocked = 9;
		s.totalPieceActiveTime = 10;
		s.totalPieceMove = 11;
		s.totalPieceRotate = 12;
		s.totalSingle = 13;
		s.totalDouble = 14;
		s.totalTriple = 15;
		s.totalFour = 16;
		s.totalTSpinZeroMini = 17;
		s.totalTSpinZero = 18;
		s.totalTSpinSingleMini = 19;
		s.totalTSpinSingle = 20;
		s.totalTSpinDoubleMini = 21;
		s.totalTSpinDouble = 22;
		s.totalTSpinTriple = 23;
		s.totalB2BFour = 24;
		s.totalB2BTSpin = 25;
		s.totalHoldUsed = 26;
		s.maxCombo = 27;
		s.spl = 1.25;
		s.spm = 2.5;
		s.sps = 3.75;
		s.lpm = 4.25f;
		s.lps = 5.5f;
		s.ppm = 6.75f;
		s.pps = 7.25f;
		s.gamerate = 59.94f;
		s.maxChain = 28;
		s.rollclear = 1;
		return s;
	}
}
