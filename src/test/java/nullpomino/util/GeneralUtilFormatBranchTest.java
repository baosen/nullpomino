package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;

/**
 * Covers the remaining {@link GeneralUtil} branches: formatG with
 * non-positive precision, infinite values, and a negative computed
 * fraction-digit count, plus pieceIdFromDigit's non-digit rejection.
 */
public class GeneralUtilFormatBranchTest {
	@Test
	public void formatGClampsNonPositivePrecisionToOne() {
		assertEquals("2", GeneralUtil.formatG(1.5, 0));
		assertEquals("2", GeneralUtil.formatG(1.5, -3));
	}

	@Test
	public void formatGHandlesInfiniteValues() {
		assertEquals("Infinity", GeneralUtil.formatG(Double.POSITIVE_INFINITY, 3));
		assertEquals("-Infinity", GeneralUtil.formatG(Double.NEGATIVE_INFINITY, 3));
	}

	@Test
	public void formatGClampsNegativeFractionDigitsToZero() {
		assertEquals("12345", GeneralUtil.formatG(12345.0, 2));
	}

	@Test
	public void pieceIdFromDigitRejectsNonDigits() throws Exception {
		Method m = GeneralUtil.class.getDeclaredMethod("pieceIdFromDigit", char.class);
		m.setAccessible(true);
		assertEquals(nullpomino.game.component.Piece.PIECE_I, m.invoke(null, 'a'));
		assertEquals(5, m.invoke(null, '5'));
		assertEquals(9, m.invoke(null, '9'));
	}
}
