package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pins the private static constants on {@link NetVSBattleMode}.
 *
 * <p>These constants define attack values, combo tables, and tuning
 * parameters for NET-VS-BATTLE mode. Since they are declared private,
 * reflection is used to read them, following the same pattern as
 * {@link NetVSLineRaceAndBattleModeInitTest}.
 */
class NetVSBattleModeConstantsTest {

	// ------------------------------------------------------------------
	// LINE_ATTACK_TABLE — 10 rows (attack types) x 5 columns (player counts)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("LINE_ATTACK_TABLE has 10 rows, each with 5 columns")
	void lineAttackTableDimensions() throws Exception {
		int[][] table = read2dIntArray("LINE_ATTACK_TABLE");
		assertEquals(10, table.length, "LINE_ATTACK_TABLE should have 10 rows");
		for (int i = 0; i < table.length; i++) {
			assertEquals(5, table[i].length,
					"LINE_ATTACK_TABLE[" + i + "] should have 5 columns");
		}
	}

	@Test
	@DisplayName("LINE_ATTACK_TABLE has expected values for every row")
	void lineAttackTableValues() throws Exception {
		int[][] table = read2dIntArray("LINE_ATTACK_TABLE");
		assertArrayEquals(new int[] {0, 0, 0, 0, 0}, table[0],  "Single");
		assertArrayEquals(new int[] {1, 1, 0, 0, 0}, table[1],  "Double");
		assertArrayEquals(new int[] {2, 2, 1, 1, 1}, table[2],  "Triple");
		assertArrayEquals(new int[] {4, 3, 2, 2, 2}, table[3],  "Four");
		assertArrayEquals(new int[] {1, 1, 0, 0, 0}, table[4],  "T-Mini-S");
		assertArrayEquals(new int[] {2, 2, 1, 1, 1}, table[5],  "T-Single");
		assertArrayEquals(new int[] {4, 3, 2, 2, 2}, table[6],  "T-Double");
		assertArrayEquals(new int[] {6, 4, 3, 3, 3}, table[7],  "T-Triple");
		assertArrayEquals(new int[] {4, 3, 2, 2, 2}, table[8],  "T-Mini-D");
		assertArrayEquals(new int[] {1, 1, 0, 0, 0}, table[9],  "EZ-T");
	}

	// ------------------------------------------------------------------
	// LINE_ATTACK_TABLE_ALLSPIN — all-spin variant (10 rows x 5 columns)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("LINE_ATTACK_TABLE_ALLSPIN has 10 rows, each with 5 columns")
	void lineAttackTableAllSpinDimensions() throws Exception {
		int[][] table = read2dIntArray("LINE_ATTACK_TABLE_ALLSPIN");
		assertEquals(10, table.length,
				"LINE_ATTACK_TABLE_ALLSPIN should have 10 rows");
		for (int i = 0; i < table.length; i++) {
			assertEquals(5, table[i].length,
					"LINE_ATTACK_TABLE_ALLSPIN[" + i + "] should have 5 columns");
		}
	}

	@Test
	@DisplayName("LINE_ATTACK_TABLE_ALLSPIN has expected values for every row")
	void lineAttackTableAllSpinValues() throws Exception {
		int[][] table = read2dIntArray("LINE_ATTACK_TABLE_ALLSPIN");
		assertArrayEquals(new int[] {0, 0, 0, 0, 0}, table[0],  "Single");
		assertArrayEquals(new int[] {1, 1, 0, 0, 0}, table[1],  "Double");
		assertArrayEquals(new int[] {2, 2, 1, 1, 1}, table[2],  "Triple");
		assertArrayEquals(new int[] {4, 3, 2, 2, 2}, table[3],  "Four");
		assertArrayEquals(new int[] {0, 0, 0, 0, 0}, table[4],  "T-Mini-S (all-spin gives 0)");
		assertArrayEquals(new int[] {2, 2, 1, 1, 1}, table[5],  "T-Single");
		assertArrayEquals(new int[] {4, 3, 2, 2, 2}, table[6],  "T-Double");
		assertArrayEquals(new int[] {6, 4, 3, 3, 3}, table[7],  "T-Triple");
		assertArrayEquals(new int[] {3, 2, 1, 1, 1}, table[8],  "T-Mini-D (all-spin gives 3)");
		assertArrayEquals(new int[] {0, 0, 0, 0, 0}, table[9],  "EZ-T (all-spin gives 0)");
	}

	// ------------------------------------------------------------------
	// COMBO_ATTACK_TABLE — 5 rows (player counts) x 12 columns (combo steps)
	// ------------------------------------------------------------------

	@Test
	@DisplayName("COMBO_ATTACK_TABLE has 5 rows")
	void comboAttackTableDimensions() throws Exception {
		int[][] table = read2dIntArray("COMBO_ATTACK_TABLE");
		assertEquals(5, table.length,
				"COMBO_ATTACK_TABLE should have 5 rows (player counts)");
		// All rows are length 12
		for (int i = 0; i < table.length; i++) {
			assertEquals(12, table[i].length,
					"COMBO_ATTACK_TABLE[" + i + "] should have 12 columns");
		}
	}

	@Test
	@DisplayName("COMBO_ATTACK_TABLE has expected values for all rows")
	void comboAttackTableValues() throws Exception {
		int[][] table = read2dIntArray("COMBO_ATTACK_TABLE");
		assertArrayEquals(
				new int[] {0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 4, 5}, table[0],
				"1-2 Player(s)");
		assertArrayEquals(
				new int[] {0, 0, 1, 1, 1, 2, 2, 3, 3, 4, 4, 4}, table[1],
				"3 Player");
		assertArrayEquals(
				new int[] {0, 0, 0, 1, 1, 1, 2, 2, 3, 3, 4, 4}, table[2],
				"4 Player");
		assertArrayEquals(
				new int[] {0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3, 4}, table[3],
				"5 Player");
		assertArrayEquals(
				new int[] {0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 3, 3}, table[4],
				"6 Players");
	}

	// ------------------------------------------------------------------
	// GARBAGE_DENOMINATOR
	// ------------------------------------------------------------------

	@Test
	@DisplayName("GARBAGE_DENOMINATOR is 60")
	void garbageDenominator() throws Exception {
		assertEquals(60, readInt("GARBAGE_DENOMINATOR"),
				"GARBAGE_DENOMINATOR should be 60 (divisible by 2,3,4,5)");
	}

	// ------------------------------------------------------------------
	// Reflection helpers for private static fields
	// ------------------------------------------------------------------

	private static int[][] read2dIntArray(String name) throws Exception {
		Field f = findField(NetVSBattleMode.class, name);
		f.setAccessible(true);
		return (int[][]) f.get(null); // static field -> null instance
	}

	private static int readInt(String name) throws Exception {
		Field f = findField(NetVSBattleMode.class, name);
		f.setAccessible(true);
		return f.getInt(null); // static field -> null instance
	}

	private static Field findField(Class<?> cls, String name)
			throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(
				cls.getName() + " (and superclasses) has no field '" + name + "'");
	}
}
