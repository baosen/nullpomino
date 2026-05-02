package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins the public API surface, protected calculation methods, and
 * inherited pure-logic methods on {@link AvalancheVSBombBattleMode}.
 *
 * <p>Most of the mode's logic is inherited from
 * {@link AvalancheVSDummyMode}.  This test covers:
 *
 * <ul>
 *   <li>{@code getName()} — mode title string</li>
 *   <li>{@code updateOjamaMeter} — overridden to scale the meter width
 *       by 6&#215; (the bomb-battle meter fills more slowly than the base
 *       Avalanche-VS meter)</li>
 *   <li>{@code calcPts} — inherited, returns {@code avalanche * 10}</li>
 *   <li>{@code ptsToOjama} — inherited ceiling division</li>
 *   <li>{@code calcChainClassicPower} — inherited classic chain multiplier</li>
 *   <li>{@code calcChainNewPower} — inherited new (Fever) chain-power table</li>
 *   <li>{@code calcChainMultiplier} — dispatches between classic/new</li>
 *   <li>{@code getChainColor} — inherited chain-display colour logic</li>
 * </ul>
 */
class AvalancheVSBombBattleModeTest {

	// =====================================================
	// getName
	// =====================================================

	@Test
	void getNameReturnsBombBattleRc1() {
		assertEquals("AVALANCHE VS BOMB BATTLE (RC1)",
				new TestableBombBattleMode().getName());
	}

	// =====================================================
	// updateOjamaMeter — meterColor thresholds
	//
	// With engine.field == null, width defaults to
	//   6 * 6 = 36
	// Thresholds:
	//   ojama >= 5*36 = 180  → RED
	//   ojama >= 36          → ORANGE
	//   ojama >= 1           → YELLOW
	//   ojama  < 1           → GREEN
	// =====================================================

	@Test
	void updateOjamaMeterWhenOjamaAboveFiveTimesWidthSetsColorRed() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 180;
		mode.updateOjamaMeter(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void updateOjamaMeterWhenOjamaJustBelowFiveTimesWidthSetsColorOrange() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 179;
		mode.updateOjamaMeter(engine, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void updateOjamaMeterWhenOjamaEqualsWidthSetsColorOrange() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 36;
		mode.updateOjamaMeter(engine, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void updateOjamaMeterWhenOjamaAboveWidthBelowFiveTimesWidthSetsColorOrange() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 100;
		mode.updateOjamaMeter(engine, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void updateOjamaMeterWhenOjamaIsOneSetsColorYellow() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 1;
		mode.updateOjamaMeter(engine, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void updateOjamaMeterWhenOjamaBetweenOneAndWidthSetsColorYellow() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 20;
		mode.updateOjamaMeter(engine, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void updateOjamaMeterWhenOjamaIsZeroSetsColorGreen() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 0;
		mode.updateOjamaMeter(engine, 0);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	@Test
	void updateOjamaMeterNegativeOjamaSetsColorGreen() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = -5;
		mode.updateOjamaMeter(engine, 0);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	// =====================================================
	// updateOjamaMeter — meterValue adjustments
	//
	// With field == null: width = 36, blockHeight = 16
	//   value = ojama * 16 / 36
	// =====================================================

	@Test
	void updateOjamaMeterIncrementsMeterValueWhenValueAboveCurrent() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 36;                 // value = 36*16/36 = 16
		engine.meterValue = 10;             // below target
		mode.updateOjamaMeter(engine, 0);
		assertEquals(11, engine.meterValue); // incremented by 1
	}

	@Test
	void updateOjamaMeterDecrementsMeterValueWhenValueBelowCurrent() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 36;                 // value = 36*16/36 = 16
		engine.meterValue = 20;             // above target
		mode.updateOjamaMeter(engine, 0);
		assertEquals(19, engine.meterValue); // decremented by 1
	}

	@Test
	void updateOjamaMeterDoesNotChangeMeterValueWhenAlreadyAtTarget() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 36;                 // value = 36*16/36 = 16
		engine.meterValue = 16;             // already at target
		mode.updateOjamaMeter(engine, 0);
		assertEquals(16, engine.meterValue); // unchanged
	}

	@Test
	void updateOjamaMeterWithZeroOjamaAndNegativeMeterValueIncrementsTowardZero() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.ojama[0] = 0;                  // value = 0*16/36 = 0
		engine.meterValue = -5;             // below target
		mode.updateOjamaMeter(engine, 0);
		assertEquals(-4, engine.meterValue); // incremented by 1
	}

	// =====================================================
	// calcPts  (inherited from AvalancheVSDummyMode)
	//
	//   pts = avalanche * 10
	// =====================================================

	@Test
	void calcPtsZeroAvalancheReturnsZero() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(0, mode.calcPts(0));
	}

	@Test
	void calcPtsPositiveAvalancheMultipliesByTen() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(10,   mode.calcPts(1));
		assertEquals(50,   mode.calcPts(5));
		assertEquals(100,  mode.calcPts(10));
		assertEquals(1000, mode.calcPts(100));
	}

	@Test
	void calcPtsNegativeAvalancheReturnsNegative() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(-10, mode.calcPts(-1));
		assertEquals(-50, mode.calcPts(-5));
	}

	// =====================================================
	// ptsToOjama  (inherited)
	//
	//   ojama = ceil(pts / rate)  =  (pts + rate - 1) / rate
	// =====================================================

	@Test
	void ptsToOjamaZeroPtsReturnsZero() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(0, mode.ptsToOjama(0, 60));
		assertEquals(0, mode.ptsToOjama(0, 1));
		assertEquals(0, mode.ptsToOjama(0, 1000));
	}

	@Test
	void ptsToOjamaExactlyAtRateReturnsOne() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(1, mode.ptsToOjama(60, 60));
		assertEquals(1, mode.ptsToOjama(1, 1));
	}

	@Test
	void ptsToOjamaRoundsUp() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(1,  mode.ptsToOjama(1, 60));
		assertEquals(2,  mode.ptsToOjama(61, 60));
		assertEquals(17, mode.ptsToOjama(1000, 60));
	}

	@Test
	void ptsToOjamaRateOfOneIsIdentity() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(1,  mode.ptsToOjama(1, 1));
		assertEquals(10, mode.ptsToOjama(10, 1));
		assertEquals(100, mode.ptsToOjama(100, 1));
	}

	// =====================================================
	// calcChainClassicPower  (inherited)
	//
	//   chain 1 → 0
	//   chain 2 → 8
	//   chain 3 → 16
	//   chain ≥4 → 32 * (chain - 3)
	// =====================================================

	@Test
	void calcChainClassicPowerChainZeroOrOneReturnsZero() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(0, mode.calcChainClassicPower(0));
		assertEquals(0, mode.calcChainClassicPower(1));
	}

	@Test
	void calcChainClassicPowerChainTwoReturnsEight() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(8, mode.calcChainClassicPower(2));
	}

	@Test
	void calcChainClassicPowerChainThreeReturnsSixteen() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(16, mode.calcChainClassicPower(3));
	}

	@Test
	void calcChainClassicPowerChainFourReturnsThirtyTwo() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(32, mode.calcChainClassicPower(4));
	}

	@Test
	void calcChainClassicPowerChainFiveReturnsSixtyFour() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(64, mode.calcChainClassicPower(5));
	}

	@Test
	void calcChainClassicPowerChainSixReturnsNinetySix() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(96, mode.calcChainClassicPower(6));
	}

	@Test
	void calcChainClassicPowerFollowsFormulaForLargeChains() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		// 32 * (100 - 3) = 3104
		assertEquals(3104, mode.calcChainClassicPower(100));
		// 32 * (20 - 3) = 544
		assertEquals(544,  mode.calcChainClassicPower(20));
	}

	@Test
	void calcChainClassicPowerNegativeChainReturnsZero() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(0, mode.calcChainClassicPower(-1));
		assertEquals(0, mode.calcChainClassicPower(-100));
	}

	// =====================================================
	// calcChainNewPower  (inherited, uses CHAIN_POWERS)
	//
	//   CHAIN_POWERS has 16 elements:
	//     {4, 12, 24, 33, 50, 101, 169, 254,
	//      341, 428, 538, 648, 763, 876, 990, 999}
	//
	//   chain 1  → CHAIN_POWERS[0]  = 4
	//   chain 16 → CHAIN_POWERS[15] = 999
	//   chain >16 → clamped to 999
	//   chain 0 or negative → throws ArrayIndexOutOfBoundsException
	// =====================================================

	@Test
	void calcChainNewPowerChainOneReturnsFour() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(4, mode.calcChainNewPower(1));
	}

	@Test
	void calcChainNewPowerChainTwoReturnsTwelve() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(12, mode.calcChainNewPower(2));
	}

	@Test
	void calcChainNewPowerChainSixteenReturnsNineHundredNinetyNine() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(999, mode.calcChainNewPower(16));
	}

	@Test
	void calcChainNewPowerChainAboveLengthClampsToLastElement() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertEquals(999, mode.calcChainNewPower(17));
		assertEquals(999, mode.calcChainNewPower(100));
		assertEquals(999, mode.calcChainNewPower(1000));
	}

	@Test
	void calcChainNewPowerChainZeroThrowsArrayIndexOutOfBounds() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> mode.calcChainNewPower(0));
	}

	@Test
	void calcChainNewPowerNegativeChainThrowsArrayIndexOutOfBounds() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> mode.calcChainNewPower(-1));
	}

	// =====================================================
	// calcChainMultiplier  (inherited, dispatches based on
	// newChainPower[playerID])
	//
	// After modeInit, newChainPower[] is false (classic path).
	// =====================================================

	@Test
	void calcChainMultiplierWithClassicPowerUsesClassicFormula() {
		TestableBombBattleMode mode = initMode();
		// newChainPower[0] defaults to false → classic path
		assertEquals(0,  mode.calcChainMultiplier(null, 0, 1));
		assertEquals(8,  mode.calcChainMultiplier(null, 0, 2));
		assertEquals(16, mode.calcChainMultiplier(null, 0, 3));
		assertEquals(32, mode.calcChainMultiplier(null, 0, 4));
		assertEquals(64, mode.calcChainMultiplier(null, 0, 5));
	}

	// =====================================================
	// getChainColor  (inherited from AvalancheVSDummyMode)
	// =====================================================

	@Test
	void getChainColorNoneDisplayDefaultsToYellow() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSDummyMode.CHAIN_DISPLAY_NONE;
		assertEquals(EventReceiver.COLOR_YELLOW, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorYellowDisplayReturnsYellow() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSDummyMode.CHAIN_DISPLAY_YELLOW;
		assertEquals(EventReceiver.COLOR_YELLOW, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorPlayerDisplayPlayerZeroReturnsRed() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSDummyMode.CHAIN_DISPLAY_PLAYER;
		assertEquals(EventReceiver.COLOR_RED, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorPlayerDisplayPlayerOneReturnsBlue() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[1] = AvalancheVSDummyMode.CHAIN_DISPLAY_PLAYER;
		assertEquals(EventReceiver.COLOR_BLUE, mode.getChainColor(engine, 1));
	}

	@Test
	void getChainColorSizeDisplayChainBelowThresholdReturnsRed() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSDummyMode.CHAIN_DISPLAY_SIZE;
		mode.rensaShibari[0] = 5;
		engine.chain = 3;
		assertEquals(EventReceiver.COLOR_RED, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorSizeDisplayChainAtThresholdReturnsGreen() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSDummyMode.CHAIN_DISPLAY_SIZE;
		mode.rensaShibari[0] = 5;
		engine.chain = 5;
		assertEquals(EventReceiver.COLOR_GREEN, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorSizeDisplayChainAboveThresholdReturnsGreen() {
		TestableBombBattleMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSDummyMode.CHAIN_DISPLAY_SIZE;
		mode.rensaShibari[0] = 5;
		engine.chain = 10;
		assertEquals(EventReceiver.COLOR_GREEN, mode.getChainColor(engine, 0));
	}

	// =====================================================
	// Helpers
	// =====================================================

	private static TestableBombBattleMode initMode() {
		TestableBombBattleMode mode = new TestableBombBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		return mode;
	}

	private static GameEngine createEngine() {
		return new GameEngine(new GameManager(new EventReceiver()), 0);
	}

	// =====================================================
	// Concrete test subclass
	//
	// Widens access on protected methods and provides thin
	// convenience wrappers for inherited pure-logic methods
	// whose engine/playerID parameters are unused.
	// =====================================================

	private static class TestableBombBattleMode extends AvalancheVSBombBattleMode {

		@Override
		public String getName() {
			return super.getName();
		}

		/** Expose protected method for testing. */
		@Override
		public void updateOjamaMeter(GameEngine engine, int playerID) {
			super.updateOjamaMeter(engine, playerID);
		}

		/** Expose protected method for testing. */
		@Override
		public int getChainColor(GameEngine engine, int playerID) {
			return super.getChainColor(engine, playerID);
		}

		// ----- convenience wrappers (engine/playerID are unused) -----

		/** Delegates to {@code calcPts(null, 0, avalanche)}. */
		public int calcPts(int avalanche) {
			return calcPts(null, 0, avalanche);
		}

		/** Delegates to {@code ptsToOjama(null, 0, pts, rate)}. */
		public int ptsToOjama(int pts, int rate) {
			return ptsToOjama(null, 0, pts, rate);
		}

		/** Delegates to {@code calcChainClassicPower(null, 0, chain)}. */
		public int calcChainClassicPower(int chain) {
			return calcChainClassicPower(null, 0, chain);
		}

		/** Delegates to {@code calcChainNewPower(null, 0, chain)}. */
		public int calcChainNewPower(int chain) {
			return calcChainNewPower(null, 0, chain);
		}
	}
}
