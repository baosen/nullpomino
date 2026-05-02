package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the shared constants and concrete-subclass contract on {@link
 * AbstractGradeMode}. The two Grade Mania variants (GRADE MANIA 2 and
 * GRADE MANIA 3) inherit all three scalar constants and the same set of
 * telemetry fields from this class.
 *
 * <p>Drives a {@link GradeStub} concrete subclass so the tests stay
 * focused on the parent-class contract without requiring a full game
 * engine.
 */
class AbstractGradeModeTest {

	// -----------------------------------------------------------------------
	// Constants
	// -----------------------------------------------------------------------

	@Test
	void rankingMaxIsTen() {
		assertEquals(10, AbstractGradeMode.RANKING_MAX);
	}

	@Test
	void sectionMaxIsTen() {
		assertEquals(10, AbstractGradeMode.SECTION_MAX);
	}

	@Test
	void defaultSectionTimeIs5400() {
		assertEquals(5400, AbstractGradeMode.DEFAULT_SECTION_TIME);
	}

	// -----------------------------------------------------------------------
	// Concrete subclass instantiation and field defaults
	// -----------------------------------------------------------------------

	@Test
	void stubExtendsAbstractGradeMode() {
		assertTrue(new GradeStub() instanceof AbstractGradeMode);
	}

	@Test
	void stubFieldsHaveSensibleDefaults() {
		GradeStub mode = new GradeStub();

		// Constants-sized arrays are allocated by subclasses in playerInit,
		// so before init they remain null.  Numeric fields default to 0,
		// booleans to false.
		assertEquals(0, mode.gravityindex);
		assertEquals(0, mode.nextseclv);
		assertEquals(0, mode.grade);
		assertEquals(0, mode.lastGradeTime);
		assertEquals(0, mode.harddropBonus);
		assertEquals(0, mode.comboValue);
		assertEquals(0, mode.lastscore);
		assertEquals(0, mode.scgettime);
		assertEquals(0, mode.rolltime);
		assertEquals(0, mode.rollclear);
		assertEquals(0, mode.secretGrade);
		assertEquals(0, mode.bgmlv);
		assertEquals(0, mode.gradeflash);
		assertEquals(0, mode.sectionscomp);
		assertEquals(0, mode.sectionavgtime);
		assertEquals(0, mode.sectionlasttime);
		assertEquals(0, mode.medalAC);
		assertEquals(0, mode.medalST);
		assertEquals(0, mode.medalSK);
		assertEquals(0, mode.medalCO);
		assertEquals(0, mode.version);
		assertEquals(0, mode.rankingRank);

		assertNotNull(mode.sectiontime);
		assertNotNull(mode.sectionIsNewRecord);
	}

	// -----------------------------------------------------------------------
	// Constants used as array sizes
	// -----------------------------------------------------------------------

	@Test
	void sectionTimeArrayLengthMatchesSectionMax() {
		GradeStub mode = new GradeStub();
		assertEquals(AbstractGradeMode.SECTION_MAX, mode.sectiontime.length);
		assertEquals(AbstractGradeMode.SECTION_MAX, mode.sectionIsNewRecord.length);
	}

	/**
	 * Concrete subclass with minimal overrides.  All GameMode methods have
	 * default (or AbstractMode) implementations, so the stub only needs a
	 * constructor that initialises the inherited arrays (mimicking what
	 * GradeMania2Mode and GradeMania3Mode do in {@code playerInit}).
	 */
	private static final class GradeStub extends AbstractGradeMode {
		GradeStub() {
			sectiontime = new int[SECTION_MAX];
			sectionIsNewRecord = new boolean[SECTION_MAX];
		}
	}
}
