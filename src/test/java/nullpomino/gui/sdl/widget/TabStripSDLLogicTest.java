package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TabStripSDLLogicTest {

    @Test
    void constructorSetsActiveTabToZero() {
        TabStripSDL ts = new TabStripSDL(0, 0, 300, 24, new String[]{"A", "B", "C"});
        assertEquals(0, ts.getActiveTab());
    }

    @Test
    void setActiveTabClampsToValidRange() {
        TabStripSDL ts = new TabStripSDL(0, 0, 300, 24, new String[]{"A", "B", "C"});
        ts.setActiveTab(5);
        assertEquals(2, ts.getActiveTab());
        ts.setActiveTab(-1);
        assertEquals(0, ts.getActiveTab());
    }

    @Test
    void setActiveTabAcceptsValidIndex() {
        TabStripSDL ts = new TabStripSDL(0, 0, 300, 24, new String[]{"A", "B", "C"});
        ts.setActiveTab(1);
        assertEquals(1, ts.getActiveTab());
    }

    @Test
    void setActiveTabOnEmptyArrayStaysAtZero() {
        TabStripSDL ts = new TabStripSDL(0, 0, 300, 24, new String[]{});
        ts.setActiveTab(0);
        assertEquals(0, ts.getActiveTab());
    }

    @Test
    void constructorWithNullLabelsCreatesEmptyTabStrip() {
        TabStripSDL ts = new TabStripSDL(0, 0, 300, 24, null);
        assertEquals(0, ts.getActiveTab());
    }

    @Test
    void setActiveTabClampsNegativeToZero() {
        TabStripSDL ts = new TabStripSDL(0, 0, 300, 24, new String[]{"A", "B"});
        ts.setActiveTab(-5);
        assertEquals(0, ts.getActiveTab());
    }
}
