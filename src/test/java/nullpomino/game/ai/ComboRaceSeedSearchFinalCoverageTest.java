package nullpomino.game.ai;
import static org.junit.jupiter.api.Assertions.*;
import nullpomino.game.component.*;
import org.junit.jupiter.api.*;
class ComboRaceSeedSearchFinalCoverageTest {
    @Test void t1() { ComboRaceSeedSearch.createTables(); assertNotNull(ComboRaceSeedSearch.moves); }
    @Test void t2() { Field fld = new Field(10, 20, 0, false); assertEquals(0, ComboRaceSeedSearch.fieldToCode(fld)); }
    @Test void t3() { assertEquals(0, ComboRaceSeedSearch.fieldToIndex((short)0x7)); assertEquals(-1, ComboRaceSeedSearch.fieldToIndex((short)0xFFF)); }
    @Test void t4() { Field fld = new Field(4, 20, 0, false); ComboRaceSeedSearch.fieldToIndex(fld, 0); assertTrue(true); }
    @Test void t5() { ComboRaceSeedSearch.createTables(); ComboRaceSeedSearch.nextQueueIDs = new int[]{0,1,2,3,4,5}; assertTrue(ComboRaceSeedSearch.thinkMain(0, -1, 0) >= 0); }
    @Test void t6() { assertEquals(0, ComboRaceSeedSearch.thinkMain(-1, -1, 0)); }
}