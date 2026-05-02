package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import nullpomino.game.play.GameEngine;

/**
 * Pins the pure-logic chain multiplier, pts, and ojama calculation
 * methods on AvalancheVSDummyMode that are pure arithmetic and don't
 * require a GameEngine for the computation.
 */
class AvalancheVSChainCalculationTest {

    // --- calcChainClassicPower ---

    @Test
    void calcChainClassicPowerReturns8ForChain2() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        assertEquals(8, mode.calcChainClassicPower(2));
    }

    @Test
    void calcChainClassicPowerReturns16ForChain3() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        assertEquals(16, mode.calcChainClassicPower(3));
    }

    @Test
    void calcChainClassicPowerReturns32TimesChainMinus3ForChain4Plus() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        assertEquals(32, mode.calcChainClassicPower(4));
        assertEquals(64, mode.calcChainClassicPower(5));
        assertEquals(96, mode.calcChainClassicPower(6));
        assertEquals(128, mode.calcChainClassicPower(7));
    }

    @Test
    void calcChainClassicPowerReturns0ForChain1() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        assertEquals(0, mode.calcChainClassicPower(1));
    }

    @Test
    void calcChainClassicPowerReturns0ForChain0() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        assertEquals(0, mode.calcChainClassicPower(0));
    }

    @Test
    void calcChainClassicPowerReturns0ForNegativeChain() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        assertEquals(0, mode.calcChainClassicPower(-1));
    }

    // --- calcChainNewPower (CHAIN_POWERS array lookup) ---

    @Test
    void calcChainNewPowerReturnsCorrectValuesFromChainPowersArray() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        // CHAIN_POWERS = {4, 12, 24, 33, 50, 101, 169, 254, 341, 428, 538, 648, 763, 876, 990, 999}
        assertEquals(4, mode.calcChainNewPower(1));
        assertEquals(12, mode.calcChainNewPower(2));
        assertEquals(24, mode.calcChainNewPower(3));
        assertEquals(33, mode.calcChainNewPower(4));
        assertEquals(50, mode.calcChainNewPower(5));
        assertEquals(999, mode.calcChainNewPower(16));
    }

    @Test
    void calcChainNewPowerReturnsLastValueForChainBeyondArrayLength() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        assertEquals(999, mode.calcChainNewPower(17));
        assertEquals(999, mode.calcChainNewPower(100));
    }

    @Test
    void calcChainNewPowerThrowsForChain0() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        // chain=0 causes CHAIN_POWERS[-1] access → IndexOutOfBoundsException
        org.junit.jupiter.api.Assertions.assertThrows(IndexOutOfBoundsException.class,
                () -> mode.calcChainNewPower(0));
    }

    // --- calcPts ---

    @Test
    void calcPtsReturnsAvalancheTimes10() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        assertEquals(10, mode.calcPts(1));
        assertEquals(20, mode.calcPts(2));
        assertEquals(50, mode.calcPts(5));
        assertEquals(100, mode.calcPts(10));
        assertEquals(0, mode.calcPts(0));
    }

    // --- ptsToOjama ---

    @Test
    void ptsToOjamaRoundsUp() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        // (pts + rate - 1) / rate
        assertEquals(1, mode.ptsToOjama(1, 120));   // (1+119)/120 = 1
        assertEquals(1, mode.ptsToOjama(120, 120));  // (120+119)/120 = 1
        assertEquals(2, mode.ptsToOjama(121, 120));   // (121+119)/120 = 2
        assertEquals(1, mode.ptsToOjama(1, 1));      // (1+0)/1 = 1
        assertEquals(100, mode.ptsToOjama(100, 1));   // (100+0)/1 = 100
    }

    @Test
    void ptsToOjamaWithRate1IsIdentity() {
        TestableVSDummyMode mode = new TestableVSDummyMode();
        assertEquals(50, mode.ptsToOjama(50, 1));
        assertEquals(1, mode.ptsToOjama(1, 1));
    }

    // --- CHAIN_POWERS constant verification ---

    @Test
    void chainPowersArrayHas16Elements() {
        assertEquals(16, AvalancheVSDummyMode.CHAIN_POWERS.length);
    }

    @Test
    void chainPowersArrayStartsWith4() {
        assertEquals(4, AvalancheVSDummyMode.CHAIN_POWERS[0]);
    }

    @Test
    void chainPowersArrayEndsWith999() {
        assertEquals(999, AvalancheVSDummyMode.CHAIN_POWERS[AvalancheVSDummyMode.CHAIN_POWERS.length - 1]);
    }

    // --- Constants verification ---

    @Test
    void ojamaCounterConstantsHaveExpectedValues() {
        assertEquals(0, AvalancheVSDummyMode.OJAMA_COUNTER_OFF);
        assertEquals(1, AvalancheVSDummyMode.OJAMA_COUNTER_ON);
        assertEquals(2, AvalancheVSDummyMode.OJAMA_COUNTER_FEVER);
    }

    @Test
    void ojamaCounterStringArrayHasExpectedValues() {
        assertEquals(3, AvalancheVSDummyMode.OJAMA_COUNTER_STRING.length);
        assertEquals("OFF", AvalancheVSDummyMode.OJAMA_COUNTER_STRING[0]);
        assertEquals("ON", AvalancheVSDummyMode.OJAMA_COUNTER_STRING[1]);
        assertEquals("FEVER", AvalancheVSDummyMode.OJAMA_COUNTER_STRING[2]);
    }

    @Test
    void zenkeshiConstantsHaveExpectedValues() {
        assertEquals(0, AvalancheVSDummyMode.ZENKESHI_MODE_OFF);
        assertEquals(1, AvalancheVSDummyMode.ZENKESHI_MODE_ON);
        assertEquals(2, AvalancheVSDummyMode.ZENKESHI_MODE_FEVER);
    }

    @Test
    void chainDisplayConstantsHaveExpectedValues() {
        assertEquals(0, AvalancheVSDummyMode.CHAIN_DISPLAY_NONE);
        assertEquals(1, AvalancheVSDummyMode.CHAIN_DISPLAY_YELLOW);
        assertEquals(2, AvalancheVSDummyMode.CHAIN_DISPLAY_PLAYER);
        assertEquals(3, AvalancheVSDummyMode.CHAIN_DISPLAY_SIZE);
    }

    @Test
    void chainDisplayNamesArrayHasExpectedValues() {
        assertEquals(4, AvalancheVSDummyMode.CHAIN_DISPLAY_NAMES.length);
        assertEquals("OFF", AvalancheVSDummyMode.CHAIN_DISPLAY_NAMES[0]);
        assertEquals("YELLOW", AvalancheVSDummyMode.CHAIN_DISPLAY_NAMES[1]);
        assertEquals("PLAYER", AvalancheVSDummyMode.CHAIN_DISPLAY_NAMES[2]);
        assertEquals("SIZE", AvalancheVSDummyMode.CHAIN_DISPLAY_NAMES[3]);
    }

    @Test
    void outlineTypeNamesArrayHasExpectedValues() {
        assertEquals(3, AvalancheVSDummyMode.OUTLINE_TYPE_NAMES.length);
        assertEquals("NORMAL", AvalancheVSDummyMode.OUTLINE_TYPE_NAMES[0]);
        assertEquals("COLOR", AvalancheVSDummyMode.OUTLINE_TYPE_NAMES[1]);
        assertEquals("NONE", AvalancheVSDummyMode.OUTLINE_TYPE_NAMES[2]);
    }

    @Test
    void maxPlayersIs2() {
        assertEquals(2, AvalancheVSDummyMode.MAX_PLAYERS);
    }

    @Test
    void pieceEnableArrayHasExpectedValues() {
        assertEquals(11, AvalancheVSDummyMode.PIECE_ENABLE.length);
        assertEquals(1, AvalancheVSDummyMode.PIECE_ENABLE[8]); // Only I1 piece enabled
        for (int i = 0; i < AvalancheVSDummyMode.PIECE_ENABLE.length; i++) {
            if (i != 8) {
                assertEquals(0, AvalancheVSDummyMode.PIECE_ENABLE[i],
                        "PIECE_ENABLE[" + i + "] should be 0");
            }
        }
    }

    @Test
    void blockColorsArrayHasExpectedValues() {
        assertEquals(5, AvalancheVSDummyMode.BLOCK_COLORS.length);
    }

    @Test
    void feverMapsArrayHasExpectedValues() {
        assertEquals(5, AvalancheVSDummyMode.FEVER_MAPS.length);
        assertEquals("Fever", AvalancheVSDummyMode.FEVER_MAPS[0]);
    }

    /** Concrete subclass for testing protected methods. */
    private static class TestableVSDummyMode extends AvalancheVSDummyMode {
        @Override
        public String getName() { return "TEST"; }

        @Override
        public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }

        // Convenience overloads — these delegate to the parent's
        // 3/4-param protected methods, passing null for the unused
        // GameEngine parameter (the calculation is pure arithmetic).

        int calcChainClassicPower(int chain) {
            return calcChainClassicPower(null, 0, chain);
        }

        int calcChainNewPower(int chain) {
            return calcChainNewPower(null, 0, chain);
        }

        int calcPts(int avalanche) {
            return calcPts(null, 0, avalanche);
        }

        int ptsToOjama(int pts, int rate) {
            return ptsToOjama(null, 0, pts, rate);
        }
    }
}
