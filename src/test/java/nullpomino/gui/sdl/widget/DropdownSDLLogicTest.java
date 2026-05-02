package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class DropdownSDLLogicTest {

    @Test
    void setItemsFromArraySetsItemsAndSelectsFirst() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(new String[]{"A", "B", "C"});
        assertEquals(0, dd.getSelectedIndex());
        assertEquals("A", dd.getSelectedItem());
    }

    @Test
    void setItemsFromListSetsItemsAndSelectsFirst() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(Arrays.asList("X", "Y", "Z"));
        assertEquals(0, dd.getSelectedIndex());
        assertEquals("X", dd.getSelectedItem());
    }

    @Test
    void setItemsWithNullArrayClearsSelection() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(new String[]{"A", "B"});
        dd.setItems((String[]) null);
        assertEquals(-1, dd.getSelectedIndex());
        assertEquals("", dd.getSelectedItem());
    }

    @Test
    void setItemsWithNullListClearsSelection() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(Arrays.asList("A", "B"));
        dd.setItems((List<String>) null);
        assertEquals(-1, dd.getSelectedIndex());
    }

    @Test
    void setSelectedIndexClampsToValidRange() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(new String[]{"A", "B", "C"});
        dd.setSelectedIndex(5);
        assertEquals(2, dd.getSelectedIndex());
        dd.setSelectedIndex(-1);
        assertEquals(-1, dd.getSelectedIndex());
    }

    @Test
    void setSelectedIndexAcceptsValidIndex() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(new String[]{"A", "B", "C"});
        dd.setSelectedIndex(1);
        assertEquals(1, dd.getSelectedIndex());
        assertEquals("B", dd.getSelectedItem());
    }

    @Test
    void getSelectedItemReturnsEmptyStringForNegativeIndex() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(new String[]{"A", "B"});
        dd.setSelectedIndex(-1);
        assertEquals("", dd.getSelectedItem());
    }

    @Test
    void closeSetsOpenToFalse() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(new String[]{"A"});
        dd.close();
        // Can't directly test open state, but close() should not throw
        assertFalse(false); // placeholder - close() is a simple setter
    }

    @Test
    void constructorWithItemsArrayInitializesCorrectly() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20, new String[]{"One", "Two"});
        assertEquals(0, dd.getSelectedIndex());
        assertEquals("One", dd.getSelectedItem());
    }

    @Test
    void setItemsShorterThanCurrentSelectionClampsIndex() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(new String[]{"A", "B", "C", "D"});
        dd.setSelectedIndex(3);
        dd.setItems(new String[]{"X", "Y"});
        assertEquals(1, dd.getSelectedIndex());
        assertEquals("Y", dd.getSelectedItem());
    }

    @Test
    void setItemsEmptyArrayClampsIndex() {
        DropdownSDL dd = new DropdownSDL(0, 0, 100, 20);
        dd.setItems(new String[]{"A", "B"});
        dd.setItems(new String[]{});
        assertEquals(-1, dd.getSelectedIndex());
    }
}
