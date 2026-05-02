package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pins the pure-logic calculation methods on Avalanche1PDummyMode:
 * calcChainMultiplier, calcPts, and calcOjama.
 */
class Avalanche1PDummyModeCalculationTest {

    // --- calcChainMultiplier ---

    @Test
    void calcChainMultiplierReturns0ForChain0() {
        Testable1PDummyMode mode = new Testable1PDummyMode();
        assertEquals(0, mode.calcChainMultiplier(0));
    }

    @Test
    void calcChainMultiplierReturns0ForChain1() {
        Testable1PDummyMode mode = new Testable1PDummyMode();
        assertEquals(0, mode.calcChainMultiplier(1));
    }

    @Test
    void calcChainMultiplierReturns8ForChain2() {
        Testable1PDummyMode mode = new Testable1PDummyMode();
        assertEquals(8, mode.calcChainMultiplier(2));
    }

    @Test
    void calcChainMultiplierReturns16ForChain3() {
        Testable1PDummyMode mode = new Testable1PDummyMode();
        assertEquals(16, mode.calcChainMultiplier(3));
    }

    @Test
    void calcChainMultiplierReturns32TimesChainMinus3ForChain4Plus() {
        Testable1PDummyMode mode = new Testable1PDummyMode();
        assertEquals(32, mode.calcChainMultiplier(4));
        assertEquals(64, mode.calcChainMultiplier(5));
        assertEquals(96, mode.calcChainMultiplier(6));
        assertEquals(128, mode.calcChainMultiplier(7));
        assertEquals(160, mode.calcChainMultiplier(8));
    }

    @Test
    void calcChainMultiplierReturns0ForNegativeChain() {
        Testable1PDummyMode mode = new Testable1PDummyMode();
        assertEquals(0, mode.calcChainMultiplier(-1));
    }

    // --- calcPts ---

    @Test
    void calcPtsReturnsAvalancheTimes10() {
        Testable1PDummyMode mode = new Testable1PDummyMode();
        assertEquals(10, mode.calcPts(1));
        assertEquals(20, mode.calcPts(2));
        assertEquals(50, mode.calcPts(5));
        assertEquals(100, mode.calcPts(10));
        assertEquals(0, mode.calcPts(0));
    }

    // --- calcOjama ---

    @Test
    void calcOjamaRoundsUp() {
        Testable1PDummyMode mode = new Testable1PDummyMode();
        mode.ojamaRate = 120;
        // calcOjama(score, avalanche, pts, multiplier) uses (score+ojamaRate-1)/ojamaRate
        assertEquals(1, mode.calcOjama(10, 1, 1, 1));    // (10+119)/120 = 1
        assertEquals(1, mode.calcOjama(120, 1, 1, 1));   // (120+119)/120 = 1
        assertEquals(2, mode.calcOjama(121, 1, 1, 1));    // (121+119)/120 = 2
    }

    @Test
    void calcOjamaWithRate1IsIdentity() {
        Testable1PDummyMode mode = new Testable1PDummyMode();
        mode.ojamaRate = 1;
        // With rate=1: (score+0)/1 = score
        assertEquals(50, mode.calcOjama(50, 1, 1, 1));
    }

    // --- PIECE_ENABLE constant ---

    @Test
    void pieceEnableArrayHasExpectedValues() {
        assertEquals(11, Avalanche1PDummyMode.PIECE_ENABLE.length);
        assertEquals(1, Avalanche1PDummyMode.PIECE_ENABLE[8]); // Only I1 piece enabled
        for (int i = 0; i < Avalanche1PDummyMode.PIECE_ENABLE.length; i++) {
            if (i != 8) {
                assertEquals(0, Avalanche1PDummyMode.PIECE_ENABLE[i],
                        "PIECE_ENABLE[" + i + "] should be 0");
            }
        }
    }

    // --- CHAIN_POWERS_FEVERTYPE constant ---

    @Test
    void chainPowersFeverTypeArrayHas16Elements() {
        assertEquals(16, Avalanche1PDummyMode.CHAIN_POWERS_FEVERTYPE.length);
    }

    // --- BLOCK_COLORS constant ---

    @Test
    void blockColorsArrayHas5Elements() {
        assertEquals(5, Avalanche1PDummyMode.BLOCK_COLORS.length);
    }

    // --- FEVER_MAPS constant ---

    @Test
    void feverMapsArrayHas5Elements() {
        assertEquals(5, Avalanche1PDummyMode.FEVER_MAPS.length);
        assertEquals("Fever", Avalanche1PDummyMode.FEVER_MAPS[0]);
    }

    // --- DAS constant ---

    @Test
    void dasConstantIs10() {
        assertEquals(10, Avalanche1PDummyMode.DAS);
    }

    /** Concrete test subclass that exposes protected methods. */
    private static class Testable1PDummyMode extends Avalanche1PDummyMode {
        @Override
        public String getName() { return "TEST_1P"; }
    }
}
