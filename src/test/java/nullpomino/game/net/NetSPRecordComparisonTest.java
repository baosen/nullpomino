package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Statistics;

class NetSPRecordComparisonTest {

	// ========================================================================
	// Ranking type constants
	// ========================================================================

	@Test
	void rankingTypeConstantsHaveExpectedValues() {
		assertEquals(0, NetSPRecord.RANKINGTYPE_GENERIC_SCORE);
		assertEquals(1, NetSPRecord.RANKINGTYPE_GENERIC_TIME);
		assertEquals(2, NetSPRecord.RANKINGTYPE_SCORERACE);
		assertEquals(3, NetSPRecord.RANKINGTYPE_DIGRACE);
		assertEquals(4, NetSPRecord.RANKINGTYPE_ULTRA);
		assertEquals(5, NetSPRecord.RANKINGTYPE_COMBORACE);
		assertEquals(6, NetSPRecord.RANKINGTYPE_DIGCHALLENGE);
		assertEquals(7, NetSPRecord.RANKINGTYPE_TIMEATTACK);
	}

	// ========================================================================
	// compareRecords — integration tests for all ranking types
	// ========================================================================

	@Test
	void genericScoreRanksScoreThenLinesThenLowerTime() {
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_SCORE,
				statsWith().score(100).lines(0).time(0),
				statsWith().score(50).lines(0).time(0));
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_SCORE,
				statsWith().score(100).lines(5).time(0),
				statsWith().score(100).lines(3).time(0));
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_SCORE,
				statsWith().score(100).lines(5).time(30),
				statsWith().score(100).lines(5).time(60));
	}

	@Test
	void genericTimeRanksLowerTimeThenLowerPiecesThenHigherPps() {
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_TIME,
				statsWith().time(50),
				statsWith().time(100));
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_TIME,
				statsWith().time(50).pieces(10),
				statsWith().time(50).pieces(20));
		assertBeats(NetSPRecord.RANKINGTYPE_GENERIC_TIME,
				statsWith().time(50).pieces(10).pps(2.0f),
				statsWith().time(50).pieces(10).pps(1.0f));
	}

	@Test
	void scoreRaceRanksLowerTimeThenLowerLinesThenHigherSpl() {
		assertBeats(NetSPRecord.RANKINGTYPE_SCORERACE,
				statsWith().time(50),
				statsWith().time(100));
		assertBeats(NetSPRecord.RANKINGTYPE_SCORERACE,
				statsWith().time(50).lines(5),
				statsWith().time(50).lines(10));
		assertBeats(NetSPRecord.RANKINGTYPE_SCORERACE,
				statsWith().time(50).lines(5).spl(3.0),
				statsWith().time(50).lines(5).spl(1.5));
	}

	@Test
	void digRaceRanksLowerTimeThenLowerLinesThenLowerPieces() {
		assertBeats(NetSPRecord.RANKINGTYPE_DIGRACE,
				statsWith().time(50),
				statsWith().time(100));
		assertBeats(NetSPRecord.RANKINGTYPE_DIGRACE,
				statsWith().time(50).lines(5),
				statsWith().time(50).lines(10));
		assertBeats(NetSPRecord.RANKINGTYPE_DIGRACE,
				statsWith().time(50).lines(5).pieces(30),
				statsWith().time(50).lines(5).pieces(40));
	}

	@Test
	void ultraRanksScoreThenLinesThenLowerPieces() {
		assertBeats(NetSPRecord.RANKINGTYPE_ULTRA,
				statsWith().score(100).lines(0),
				statsWith().score(50).lines(0));
		assertBeats(NetSPRecord.RANKINGTYPE_ULTRA,
				statsWith().score(100).lines(5),
				statsWith().score(100).lines(3));
		assertBeats(NetSPRecord.RANKINGTYPE_ULTRA,
				statsWith().score(100).lines(5).pieces(30),
				statsWith().score(100).lines(5).pieces(40));
	}

	@Test
	void comboRaceRanksComboThenLowerTimeThenHigherPps() {
		assertBeats(NetSPRecord.RANKINGTYPE_COMBORACE,
				statsWith().combo(10),
				statsWith().combo(5));
		assertBeats(NetSPRecord.RANKINGTYPE_COMBORACE,
				statsWith().combo(10).time(30),
				statsWith().combo(10).time(60));
		assertBeats(NetSPRecord.RANKINGTYPE_COMBORACE,
				statsWith().combo(10).time(30).pps(2.0f),
				statsWith().combo(10).time(30).pps(1.0f));
	}

	@Test
	void digChallengeRanksScoreThenLinesThenHigherTime() {
		assertBeats(NetSPRecord.RANKINGTYPE_DIGCHALLENGE,
				statsWith().score(100).lines(0),
				statsWith().score(50).lines(0));
		assertBeats(NetSPRecord.RANKINGTYPE_DIGCHALLENGE,
				statsWith().score(100).lines(5),
				statsWith().score(100).lines(3));
		assertBeats(NetSPRecord.RANKINGTYPE_DIGCHALLENGE,
				statsWith().score(100).lines(5).time(60),
				statsWith().score(100).lines(5).time(30));
	}

	@Test
	void timeAttackRanksRollClearThenCappedLinesThenLowerTimeThenHigherPps() {
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK,
				statsWith().rollclear(1),
				statsWith().rollclear(0));
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK,
				statsWith().rollclear(1).lines(150),
				statsWith().rollclear(1).lines(100));
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK,
				statsWith().rollclear(1).lines(150).time(30),
				statsWith().rollclear(1).lines(150).time(60));
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK,
				statsWith().rollclear(1).lines(150).time(30).pps(2.0f),
				statsWith().rollclear(1).lines(150).time(30).pps(1.0f));
	}

	@Test
	void timeAttackCapsLinesAt200ForGameType5Plus() {
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK,
				statsWith().gameType(5).rollclear(1).lines(200),
				statsWith().gameType(5).rollclear(1).lines(150));
		assertBeats(NetSPRecord.RANKINGTYPE_TIMEATTACK,
				statsWith().gameType(5).rollclear(1).lines(200).time(30),
				statsWith().gameType(5).rollclear(1).lines(250).time(60));
	}

	@Test
	void equalRecordsReturnFalseForAllTypes() {
		for (int type = 0; type <= 7; type++) {
			Statistics s = statsWith().score(100).lines(10).time(50).pieces(20).combo(5).rollclear(1).stats;
			assertFalse(NetSPRecord.compareRecords(type, recordWith(s), recordWith(s)),
					"Equal records should not beat each other for type " + type);
		}
	}

	@Test
	void unknownRankingTypeReturnsFalse() {
		Statistics s = statsWith().score(100).stats;
		assertFalse(NetSPRecord.compareRecords(999, recordWith(s), recordWith(s)));
		assertFalse(NetSPRecord.compareRecords(-1, recordWith(s), recordWith(s)));
		assertFalse(NetSPRecord.compareRecords(8, recordWith(s), recordWith(s)));
	}

	// ========================================================================
	// isBetter (private, via reflection)
	// ========================================================================

	@Test
	void isBetterReturnsTrueWhenFirstNonZeroIsPositive() throws Exception {
		assertTrue(invokeIsBetter(1, 0, -1));
	}

	@Test
	void isBetterReturnsFalseWhenFirstNonZeroIsNegative() throws Exception {
		assertFalse(invokeIsBetter(-1, 0, 1));
	}

	@Test
	void isBetterReturnsFalseWhenAllZeros() throws Exception {
		assertFalse(invokeIsBetter(0, 0, 0));
	}

	@Test
	void isBetterSkipsZerosAndReturnsTrueForLaterPositive() throws Exception {
		assertTrue(invokeIsBetter(0, 0, 5));
	}

	@Test
	void isBetterSkipsZerosAndReturnsFalseForLaterNegative() throws Exception {
		assertFalse(invokeIsBetter(0, 0, -3));
	}

	@Test
	void isBetterReturnsFalseForEmptyInput() throws Exception {
		assertFalse(invokeIsBetter());
	}

	@Test
	void isBetterSinglePositive() throws Exception {
		assertTrue(invokeIsBetter(2));
	}

	@Test
	void isBetterSingleNegative() throws Exception {
		assertFalse(invokeIsBetter(-2));
	}

	@Test
	void isBetterSingleZero() throws Exception {
		assertFalse(invokeIsBetter(0));
	}

	// ========================================================================
	// higher(int, int) / lower(int, int) (private, via reflection)
	// ========================================================================

	@Test
	void higherIntReturnsPositiveWhenFirstGreater() throws Exception {
		assertTrue(invokeHigherInt(5, 3) > 0);
	}

	@Test
	void higherIntReturnsNegativeWhenFirstSmaller() throws Exception {
		assertTrue(invokeHigherInt(3, 5) < 0);
	}

	@Test
	void higherIntReturnsZeroWhenEqual() throws Exception {
		assertEquals(0, invokeHigherInt(7, 7));
	}

	@Test
	void higherIntHandlesNegativeValues() throws Exception {
		assertTrue(invokeHigherInt(-1, -5) > 0);
		assertTrue(invokeHigherInt(-5, -1) < 0);
		assertEquals(0, invokeHigherInt(-3, -3));
	}

	@Test
	void lowerIntReturnsPositiveWhenFirstSmaller() throws Exception {
		assertTrue(invokeLowerInt(3, 5) > 0);
	}

	@Test
	void lowerIntReturnsNegativeWhenFirstGreater() throws Exception {
		assertTrue(invokeLowerInt(5, 3) < 0);
	}

	@Test
	void lowerIntReturnsZeroWhenEqual() throws Exception {
		assertEquals(0, invokeLowerInt(7, 7));
	}

	@Test
	void lowerIntHandlesNegativeValues() throws Exception {
		assertTrue(invokeLowerInt(-5, -1) > 0);
		assertTrue(invokeLowerInt(-1, -5) < 0);
		assertEquals(0, invokeLowerInt(-3, -3));
	}

	// ========================================================================
	// higher(float, float) (private, via reflection)
	// ========================================================================

	@Test
	void higherFloatReturnsPositiveWhenFirstGreater() throws Exception {
		assertTrue(invokeHigherFloat(5.5f, 3.3f) > 0);
	}

	@Test
	void higherFloatReturnsNegativeWhenFirstSmaller() throws Exception {
		assertTrue(invokeHigherFloat(3.3f, 5.5f) < 0);
	}

	@Test
	void higherFloatReturnsZeroWhenEqual() throws Exception {
		assertEquals(0, invokeHigherFloat(4.2f, 4.2f));
	}

	@Test
	void higherFloatHandlesNegativeValues() throws Exception {
		assertTrue(invokeHigherFloat(-1.5f, -5.5f) > 0);
		assertTrue(invokeHigherFloat(-5.5f, -1.5f) < 0);
	}

	@Test
	void higherFloatHandlesNaN() throws Exception {
		assertTrue(invokeHigherFloat(Float.NaN, 1.0f) > 0);
	}

	// ========================================================================
	// higher(double, double) (private, via reflection)
	// ========================================================================

	@Test
	void higherDoubleReturnsPositiveWhenFirstGreater() throws Exception {
		assertTrue(invokeHigherDouble(5.5, 3.3) > 0);
	}

	@Test
	void higherDoubleReturnsNegativeWhenFirstSmaller() throws Exception {
		assertTrue(invokeHigherDouble(3.3, 5.5) < 0);
	}

	@Test
	void higherDoubleReturnsZeroWhenEqual() throws Exception {
		assertEquals(0, invokeHigherDouble(4.2, 4.2));
	}

	@Test
	void higherDoubleHandlesNegativeValues() throws Exception {
		assertTrue(invokeHigherDouble(-1.5, -5.5) > 0);
		assertTrue(invokeHigherDouble(-5.5, -1.5) < 0);
	}

	@Test
	void higherDoubleHandlesNaN() throws Exception {
		assertTrue(invokeHigherDouble(Double.NaN, 1.0) > 0);
	}

	// ========================================================================
	// statRow (private, via reflection)
	// ========================================================================

	@Test
	void statRowJoinsWithCommas() throws Exception {
		assertEquals("1,2,hello", invokeStatRow(1, 2, "hello"));
	}

	@Test
	void statRowSingleElement() throws Exception {
		assertEquals("42", invokeStatRow(42));
	}

	@Test
	void statRowEmptyReturnsEmptyString() throws Exception {
		assertEquals("", invokeStatRow());
	}

	@Test
	void statRowHandlesNullElements() throws Exception {
		assertEquals("null", invokeStatRow((Object) null));
	}

	@Test
	void statRowPreservesWhitespace() throws Exception {
		assertEquals("a b, c ", invokeStatRow("a b", " c "));
	}

	@Test
	void statRowMixedTypes() throws Exception {
		assertEquals("10,20.5,true,test", invokeStatRow(10, 20.5, true, "test"));
	}

	@Test
	void statRowSpecialCharacters() throws Exception {
		assertEquals("a,b,c", invokeStatRow("a", "b", "c"));
	}

	// ========================================================================
	// Private helpers
	// ========================================================================

	private static void assertBeats(int type, StatBuilder better, StatBuilder worse) {
		assertTrue(compare(type, better, worse),
				"Expected record to be better for type " + type);
		assertFalse(compare(type, worse, better),
				"Expected record to NOT be better for type " + type);
	}

	private static boolean compare(int type, StatBuilder first, StatBuilder second) {
		NetSPRecord r1 = recordWith(first.stats);
		r1.gameType = first.gameType;
		NetSPRecord r2 = recordWith(second.stats);
		r2.gameType = second.gameType;
		return NetSPRecord.compareRecords(type, r1, r2);
	}

	private static NetSPRecord recordWith(Statistics stats) {
		NetSPRecord r = new NetSPRecord();
		r.stats = stats;
		return r;
	}

	private static StatBuilder statsWith() {
		return new StatBuilder();
	}

	private static final class StatBuilder {
		private final Statistics stats = new Statistics();
		private int gameType;

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

		StatBuilder gameType(int value) {
			gameType = value;
			return this;
		}
	}

	// ---- Reflection helpers for private static methods ----

	private static boolean invokeIsBetter(int... comparisons) throws Exception {
		Method method = NetSPRecord.class.getDeclaredMethod("isBetter", int[].class);
		method.setAccessible(true);
		return (boolean) method.invoke(null, comparisons);
	}

	private static int invokeHigherInt(int a, int b) throws Exception {
		Method method = NetSPRecord.class.getDeclaredMethod("higher", int.class, int.class);
		method.setAccessible(true);
		return (int) method.invoke(null, a, b);
	}

	private static int invokeLowerInt(int a, int b) throws Exception {
		Method method = NetSPRecord.class.getDeclaredMethod("lower", int.class, int.class);
		method.setAccessible(true);
		return (int) method.invoke(null, a, b);
	}

	private static int invokeHigherFloat(float a, float b) throws Exception {
		Method method = NetSPRecord.class.getDeclaredMethod("higher", float.class, float.class);
		method.setAccessible(true);
		return (int) method.invoke(null, a, b);
	}

	private static int invokeHigherDouble(double a, double b) throws Exception {
		Method method = NetSPRecord.class.getDeclaredMethod("higher", double.class, double.class);
		method.setAccessible(true);
		return (int) method.invoke(null, a, b);
	}

	private static String invokeStatRow(Object... values) throws Exception {
		Method method = NetSPRecord.class.getDeclaredMethod("statRow", Object[].class);
		method.setAccessible(true);
		return (String) method.invoke(null, new Object[] { values });
	}
}
