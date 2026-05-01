package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the shared scaffolding on {@link AbstractMarathonMode}. The two
 * Marathon variants delegate every byte of their on-disk format here,
 * so a regression in the property-key shape, ranking insert, or score
 * bookkeeping would silently change save files.
 *
 * <p>Drives a {@link MarathonStub} concrete subclass that picks a
 * single-bucket ranking layout and a stable property prefix so the
 * tests stay focused on the parent-class contract.
 */
class AbstractMarathonModeTest {

	@Test
	void afterSoftDropFallAddsFallToBothScoreAndSoftDropTotal() {
		MarathonStub mode = new MarathonStub();
		GameEngine engine = freshEngine();
		engine.statistics.score = 100;
		engine.statistics.scoreFromSoftDrop = 5;

		mode.afterSoftDropFall(engine, 0, 7);

		assertEquals(107, engine.statistics.score);
		assertEquals(12, engine.statistics.scoreFromSoftDrop);
	}

	@Test
	void afterHardDropFallAddsTwiceFallToBothScoreAndHardDropTotal() {
		MarathonStub mode = new MarathonStub();
		GameEngine engine = freshEngine();
		engine.statistics.score = 50;
		engine.statistics.scoreFromHardDrop = 4;

		mode.afterHardDropFall(engine, 0, 3);

		assertEquals(56, engine.statistics.score);
		assertEquals(10, engine.statistics.scoreFromHardDrop);
	}

	@Test
	void updateRankingInsertsAtPositionZeroForBetterScoreAndShiftsLowerEntriesDown() {
		MarathonStub mode = new MarathonStub();
		mode.allocateRankingArrays();
		// Pre-seed bucket 0 with a single existing entry at slot 0.
		mode.rankingScore[0][0] = 50;
		mode.rankingLines[0][0] = 5;
		mode.rankingTime[0][0] = 100;

		mode.updateRanking(80, 7, 90, 0);

		assertEquals(0, mode.rankingRank);
		assertEquals(80, mode.rankingScore[0][0]);
		assertEquals(7, mode.rankingLines[0][0]);
		assertEquals(90, mode.rankingTime[0][0]);
		// The previous entry shifts down to slot 1.
		assertEquals(50, mode.rankingScore[0][1]);
		assertEquals(5, mode.rankingLines[0][1]);
		assertEquals(100, mode.rankingTime[0][1]);
	}

	@Test
	void checkRankingReturnsMinusOneWhenScoreIsBelowEverySlot() {
		MarathonStub mode = new MarathonStub();
		mode.allocateRankingArrays();
		for(int i = 0; i < 10; i++) {
			mode.rankingScore[0][i] = 1000 - i;
		}

		assertEquals(-1, mode.checkRanking(0, 0, 999999, 0));
	}

	@Test
	void checkRankingTieBreaksByLinesThenByLowerTime() {
		MarathonStub mode = new MarathonStub();
		mode.allocateRankingArrays();
		mode.rankingScore[0][0] = 100; mode.rankingLines[0][0] = 5; mode.rankingTime[0][0] = 100;
		mode.rankingScore[0][1] = 100; mode.rankingLines[0][1] = 4; mode.rankingTime[0][1] = 200;

		// Same score and lines as slot 0 but a faster time → places above.
		assertEquals(0, mode.checkRanking(100, 5, 50, 0));
		// Same score and slower time but more lines than slot 0 → above.
		assertEquals(0, mode.checkRanking(100, 6, 1000, 0));
		// Lower lines and slower time but matching score → falls to slot 1.
		assertEquals(1, mode.checkRanking(100, 4, 100, 0));
	}

	@Test
	void loadCoreSettingsAppliesDocumentedDefaultsForMissingKeys() {
		MarathonStub mode = new MarathonStub();

		mode.loadCoreSettings(new CustomProperties());

		assertEquals(0, mode.startlevel);
		assertEquals(1, mode.tspinEnableType,
				"normal-T-spin (1) is the default for new configs");
		assertTrue(mode.enableTSpin);
		assertTrue(mode.enableTSpinKick);
		assertEquals(0, mode.spinCheckType);
		assertFalse(mode.tspinEnableEZ);
		assertTrue(mode.enableB2B);
		assertTrue(mode.enableCombo);
		assertFalse(mode.big);
		assertEquals(0, mode.version);
	}

	@Test
	void loadCoreSettingsAndSaveCoreSettingsRoundTripUnderSubclassPrefix() {
		MarathonStub source = new MarathonStub();
		source.startlevel = 5;
		source.tspinEnableType = 2;
		source.enableTSpin = false;
		source.enableTSpinKick = false;
		source.spinCheckType = 1;
		source.tspinEnableEZ = true;
		source.enableB2B = false;
		source.enableCombo = false;
		source.big = true;
		source.version = 42;

		CustomProperties prop = new CustomProperties();
		source.saveCoreSettings(prop);

		// Confirm a few keys land under the subclass prefix on the wire.
		assertEquals(5, prop.getProperty("teststub.startlevel", -1));
		assertEquals(true, prop.getProperty("teststub.big", false));
		assertEquals(42, prop.getProperty("teststub.version", -1));

		MarathonStub destination = new MarathonStub();
		destination.loadCoreSettings(prop);

		assertEquals(5, destination.startlevel);
		assertEquals(2, destination.tspinEnableType);
		assertFalse(destination.enableTSpin);
		assertFalse(destination.enableTSpinKick);
		assertEquals(1, destination.spinCheckType);
		assertTrue(destination.tspinEnableEZ);
		assertFalse(destination.enableB2B);
		assertFalse(destination.enableCombo);
		assertTrue(destination.big);
		assertEquals(42, destination.version);
	}

	@Test
	void loadRankingAndSaveRankingRoundTripPerGameTypeBucket() {
		MarathonStub source = new MarathonStub();
		source.allocateRankingArrays();
		// Stamp every (type, slot) cell with a unique value so a swapped
		// dimension shows up immediately.
		for(int type = 0; type < source.getGameTypeCount(); type++) {
			for(int slot = 0; slot < AbstractMarathonMode.RANKING_MAX; slot++) {
				source.rankingScore[type][slot] = 1000 + type * 100 + slot;
				source.rankingLines[type][slot] = 10 * type + slot;
				source.rankingTime[type][slot] = 999 - slot;
			}
		}

		CustomProperties prop = new CustomProperties();
		source.saveRanking(prop, "Standard");

		MarathonStub destination = new MarathonStub();
		destination.allocateRankingArrays();
		destination.loadRanking(prop, "Standard");

		for(int type = 0; type < source.getGameTypeCount(); type++) {
			for(int slot = 0; slot < AbstractMarathonMode.RANKING_MAX; slot++) {
				assertEquals(source.rankingScore[type][slot], destination.rankingScore[type][slot]);
				assertEquals(source.rankingLines[type][slot], destination.rankingLines[type][slot]);
				assertEquals(source.rankingTime[type][slot], destination.rankingTime[type][slot]);
			}
		}

		// Spot-check the property key shape so the on-disk format does not
		// drift behind the loader.
		assertEquals(1000, prop.getProperty(
				"teststub.ranking.Standard.0.score.0", -1));
		assertEquals(1, prop.getProperty(
				"teststub.ranking.Standard.0.lines.1", -1));
	}

	@Test
	void loadRankingAppliesZeroDefaultsWhenPropertyFileIsMissingKeys() {
		MarathonStub mode = new MarathonStub();
		mode.allocateRankingArrays();
		// Spoil the arrays so we can prove load actually writes zeros.
		for(int slot = 0; slot < AbstractMarathonMode.RANKING_MAX; slot++) {
			mode.rankingScore[0][slot] = 999;
			mode.rankingLines[0][slot] = 999;
			mode.rankingTime[0][slot] = 999;
		}

		mode.loadRanking(new CustomProperties(), "Anything");

		for(int slot = 0; slot < AbstractMarathonMode.RANKING_MAX; slot++) {
			assertEquals(0, mode.rankingScore[0][slot]);
			assertEquals(0, mode.rankingLines[0][slot]);
			assertEquals(0, mode.rankingTime[0][slot]);
		}
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	/**
	 * Concrete subclass with a single ranking bucket and a stable property
	 * prefix. Single bucket keeps the round-trip assertions readable
	 * without sacrificing coverage of the dimension-aware loader.
	 */
	private static final class MarathonStub extends AbstractMarathonMode {
		@Override public String getName() { return "MarathonStub"; }
		@Override protected String getPropertyPrefix() { return "teststub"; }
		@Override protected int getGameTypeCount() { return 1; }
	}
}
