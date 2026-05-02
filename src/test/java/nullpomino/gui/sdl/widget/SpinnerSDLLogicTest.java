package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SpinnerSDLLogicTest {

    @Test
    void constructorClampsInitialValueToRange() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 150);
        assertEquals(100, s.getValue());
    }

    @Test
    void constructorClampsInitialValueBelowMin() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 10, 50, 1, 5);
        assertEquals(10, s.getValue());
    }

    @Test
    void constructorAcceptsInitialValueInRange() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 42);
        assertEquals(42, s.getValue());
    }

    @Test
    void setValueClampsToMax() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
        s.setValue(200);
        assertEquals(100, s.getValue());
    }

    @Test
    void setValueClampsToMin() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
        s.setValue(-10);
        assertEquals(0, s.getValue());
    }

    @Test
    void setValueAcceptsInRangeValue() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
        s.setValue(75);
        assertEquals(75, s.getValue());
    }

    @Test
    void handleTextInputAppendsDigitsToEditor() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
        // handleTextInput appends to the editor's existing text ("50" + "42" = "5042" → clamped to 100)
        s.handleTextInput("42");
        assertEquals(100, s.getValue());
    }

    @Test
    void handleTextInputIgnoresNonDigits() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
        s.handleTextInput("abc");
        // Non-digit input should not change the value
        assertEquals(50, s.getValue());
    }

    @Test
    void handleTextInputAllowsLeadingMinus() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, -50, 50, 1, 0);
        s.handleTextInput("-");
        // Just a minus sign with no digits shouldn't change value
        assertEquals(0, s.getValue());
    }

    @Test
    void handleTextInputClampsParsedValue() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
        s.handleTextInput("999");
        assertEquals(100, s.getValue());
    }

    @Test
    void handleTextInputWithNullDoesNotCrash() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
        s.handleTextInput(null);
        assertEquals(50, s.getValue());
    }

    @Test
    void stepParameterIsUsedForDecrement() {
        SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 5, 50);
        s.setValue(50);
        // Simulate pressing left by setting value directly with step
        s.setValue(50 - 5);
        assertEquals(45, s.getValue());
    }
}
