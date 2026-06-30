package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link PracticeMode#onSetting} across every numeric configuration
 * cursor, pushing each value past both its lower and upper bound so the
 * wraparound branches ({@code value < min} and {@code value > max}) all fire.
 * Also exercises the BUTTON_E (x100) and BUTTON_F (x1000) speed multipliers.
 *
 * <p>Only LEFT/RIGHT is injected (never BUTTON_A), so the decide block that
 * persists presets / maps is never reached. The receiver is still a
 * no-op-persisting subclass as a belt-and-braces guard against writing the
 * git-tracked {@code config/map/**} files.
 */
class PracticeModeSettingWraparoundTest {

    /** Never writes anything to disk. */
    private static final class NonPersistingReceiver extends EventReceiver {
        @Override public boolean saveProperties(String f, CustomProperties p) { return true; }
        @Override public void saveModeConfig(CustomProperties c) { }
    }

    private static GameEngine freshEngine(PracticeMode mode) {
        GameManager manager = new GameManager(new NonPersistingReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[0].owner.replayMode = false;
        return manager.engine[0];
    }

    /** Sets menuCursor, injects a single LEFT/RIGHT (and optional E/F) press, then runs onSetting. */
    private static void change(PracticeMode mode, GameEngine engine, int cursor, int dirButton, int multButton)
            throws Exception {
        setInt(mode, "menuCursor", cursor);
        setInt(mode, "menuTime", 0);
        // reset() clears both buttonPress and buttonTime; clearButtonState() would
        // leave stale buttonTime entries so a prior LEFT keeps overriding RIGHT.
        engine.ctrl.reset();
        engine.ctrl.buttonPress[dirButton] = true;
        engine.ctrl.buttonTime[dirButton] = 1;
        if(multButton >= 0) {
            engine.ctrl.buttonPress[multButton] = true;
            engine.ctrl.buttonTime[multButton] = 1;
        }
        mode.onSetting(engine, 0);
    }

    // -----------------------------------------------------------------------
    // case 0: gravity (with x100 / x1000 multipliers)
    // -----------------------------------------------------------------------

    @Test
    void gravityWrapsAtBothBoundsWithMultipliers() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        // Underflow with the x100 multiplier (BUTTON_E): -1 - 100 < -1 -> 99999
        engine.speed.gravity = -1;
        change(mode, engine, 0, Controller.BUTTON_LEFT, Controller.BUTTON_E);
        assertEquals(99999, engine.speed.gravity);

        // Overflow with the x1000 multiplier (BUTTON_F): 99999 + 1000 > 99999 -> -1
        engine.speed.gravity = 99999;
        change(mode, engine, 0, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
        assertEquals(-1, engine.speed.gravity);
    }

    // -----------------------------------------------------------------------
    // case 1: denominator (with x100 / x1000 multipliers)
    // -----------------------------------------------------------------------

    @Test
    void denominatorWrapsAtBothBoundsWithMultipliers() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.speed.denominator = -1;
        change(mode, engine, 1, Controller.BUTTON_LEFT, Controller.BUTTON_E);
        assertEquals(99999, engine.speed.denominator);

        engine.speed.denominator = 99999;
        change(mode, engine, 1, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
        assertEquals(-1, engine.speed.denominator);
    }

    // -----------------------------------------------------------------------
    // cases 2-6: are / areLine / lineDelay / lockDelay / das (0..99 wrap)
    // -----------------------------------------------------------------------

    @Test
    void areAreLineLineDelayLockDelayDasWrapAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.speed.are = 0;
        change(mode, engine, 2, Controller.BUTTON_LEFT, -1);
        assertEquals(99, engine.speed.are);
        engine.speed.are = 99;
        change(mode, engine, 2, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, engine.speed.are);

        engine.speed.areLine = 0;
        change(mode, engine, 3, Controller.BUTTON_LEFT, -1);
        assertEquals(99, engine.speed.areLine);
        engine.speed.areLine = 99;
        change(mode, engine, 3, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, engine.speed.areLine);

        engine.speed.lineDelay = 0;
        change(mode, engine, 4, Controller.BUTTON_LEFT, -1);
        assertEquals(99, engine.speed.lineDelay);
        engine.speed.lineDelay = 99;
        change(mode, engine, 4, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, engine.speed.lineDelay);

        engine.speed.lockDelay = 0;
        change(mode, engine, 5, Controller.BUTTON_LEFT, -1);
        assertEquals(99, engine.speed.lockDelay);
        engine.speed.lockDelay = 99;
        change(mode, engine, 5, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, engine.speed.lockDelay);

        engine.speed.das = 0;
        change(mode, engine, 6, Controller.BUTTON_LEFT, -1);
        assertEquals(99, engine.speed.das);
        engine.speed.das = 99;
        change(mode, engine, 6, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, engine.speed.das);
    }

    // -----------------------------------------------------------------------
    // case 7: bgmno (0 .. BGM_COUNT-1 wrap)
    // -----------------------------------------------------------------------

    @Test
    void bgmnoWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "bgmno", 0);
        change(mode, engine, 7, Controller.BUTTON_LEFT, -1);
        assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));

        setInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
        change(mode, engine, 7, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "bgmno"));
    }

    // -----------------------------------------------------------------------
    // case 9: leveltype (0 .. LEVELTYPE_MAX-1 == 4 wrap)
    // -----------------------------------------------------------------------

    @Test
    void leveltypeWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "leveltype", 0);
        change(mode, engine, 9, Controller.BUTTON_LEFT, -1);
        assertEquals(4, readInt(mode, "leveltype")); // LEVELTYPE_MAX - 1

        setInt(mode, "leveltype", 4);
        change(mode, engine, 9, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "leveltype"));
    }

    // -----------------------------------------------------------------------
    // case 10: tspinEnableType (0..2 wrap)
    // -----------------------------------------------------------------------

    @Test
    void tspinEnableTypeWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "tspinEnableType", 0);
        change(mode, engine, 10, Controller.BUTTON_LEFT, -1);
        assertEquals(2, readInt(mode, "tspinEnableType"));

        setInt(mode, "tspinEnableType", 2);
        change(mode, engine, 10, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "tspinEnableType"));
    }

    // -----------------------------------------------------------------------
    // case 12: spinCheckType (0..1 wrap)
    // -----------------------------------------------------------------------

    @Test
    void spinCheckTypeWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "spinCheckType", 0);
        change(mode, engine, 12, Controller.BUTTON_LEFT, -1);
        assertEquals(1, readInt(mode, "spinCheckType"));

        setInt(mode, "spinCheckType", 1);
        change(mode, engine, 12, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "spinCheckType"));
    }

    // -----------------------------------------------------------------------
    // case 15: comboType (0..2 wrap)
    // -----------------------------------------------------------------------

    @Test
    void comboTypeWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "comboType", 0);
        change(mode, engine, 15, Controller.BUTTON_LEFT, -1);
        assertEquals(2, readInt(mode, "comboType"));

        setInt(mode, "comboType", 2);
        change(mode, engine, 15, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "comboType"));
    }

    // -----------------------------------------------------------------------
    // case 19: goallv (-1 .. 9999 wrap, with x1000 multiplier)
    // -----------------------------------------------------------------------

    @Test
    void goallvWrapsAtBothBoundsWithMultiplier() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        // Underflow: -1 - 1000 < -1 -> 9999
        setInt(mode, "goallv", -1);
        change(mode, engine, 19, Controller.BUTTON_LEFT, Controller.BUTTON_F);
        assertEquals(9999, readInt(mode, "goallv"));

        // Overflow: 9999 + 1000 > 9999 -> -1
        setInt(mode, "goallv", 9999);
        change(mode, engine, 19, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
        assertEquals(-1, readInt(mode, "goallv"));
    }

    // -----------------------------------------------------------------------
    // case 20: timelimit (0 .. 3600*20 wrap, change scaled by 60*m)
    // -----------------------------------------------------------------------

    @Test
    void timelimitWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        // Underflow: 0 - 60 < 0 -> 3600*20
        setInt(mode, "timelimit", 0);
        change(mode, engine, 20, Controller.BUTTON_LEFT, -1);
        assertEquals(3600 * 20, readInt(mode, "timelimit"));

        // Overflow: 72000 + 60 > 72000 -> 0
        setInt(mode, "timelimit", 3600 * 20);
        change(mode, engine, 20, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "timelimit"));
    }

    // -----------------------------------------------------------------------
    // case 21: rolltimelimit (0 .. 3600*20 wrap)
    // -----------------------------------------------------------------------

    @Test
    void rolltimelimitWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "rolltimelimit", 0);
        change(mode, engine, 21, Controller.BUTTON_LEFT, -1);
        assertEquals(3600 * 20, readInt(mode, "rolltimelimit"));

        setInt(mode, "rolltimelimit", 3600 * 20);
        change(mode, engine, 21, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "rolltimelimit"));
    }

    // -----------------------------------------------------------------------
    // case 24: blockHidden (-2 .. 9999 wrap, with x100 multiplier)
    // -----------------------------------------------------------------------

    @Test
    void blockHiddenWrapsAtBothBoundsWithMultiplier() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        // Underflow: -2 - 100 < -2 -> 9999
        setInt(mode, "blockHidden", -2);
        change(mode, engine, 24, Controller.BUTTON_LEFT, Controller.BUTTON_E);
        assertEquals(9999, readInt(mode, "blockHidden"));

        // Overflow: 9999 + 100 > 9999 -> -2
        setInt(mode, "blockHidden", 9999);
        change(mode, engine, 24, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
        assertEquals(-2, readInt(mode, "blockHidden"));
    }

    // -----------------------------------------------------------------------
    // case 26: blockOutlineType (0..3 wrap)
    // -----------------------------------------------------------------------

    @Test
    void blockOutlineTypeWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "blockOutlineType", 0);
        change(mode, engine, 26, Controller.BUTTON_LEFT, -1);
        assertEquals(3, readInt(mode, "blockOutlineType"));

        setInt(mode, "blockOutlineType", 3);
        change(mode, engine, 26, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "blockOutlineType"));
    }

    // -----------------------------------------------------------------------
    // case 28: heboHiddenLevel (0..7 wrap)
    // -----------------------------------------------------------------------

    @Test
    void heboHiddenLevelWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "heboHiddenLevel", 0);
        change(mode, engine, 28, Controller.BUTTON_LEFT, -1);
        assertEquals(7, readInt(mode, "heboHiddenLevel"));

        setInt(mode, "heboHiddenLevel", 7);
        change(mode, engine, 28, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "heboHiddenLevel"));
    }

    // -----------------------------------------------------------------------
    // cases 41/42/43: mapNumber (0..99 wrap) -- LEFT/RIGHT only, no decide
    // -----------------------------------------------------------------------

    @Test
    void mapNumberWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "mapNumber", 0);
        change(mode, engine, 41, Controller.BUTTON_LEFT, -1);
        assertEquals(99, readInt(mode, "mapNumber"));

        setInt(mode, "mapNumber", 99);
        change(mode, engine, 43, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "mapNumber"));
    }

    // -----------------------------------------------------------------------
    // cases 44/45: presetNumber (0..99 wrap) -- LEFT/RIGHT only, no decide
    // -----------------------------------------------------------------------

    @Test
    void presetNumberWrapsAtBothBounds() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "presetNumber", 0);
        change(mode, engine, 44, Controller.BUTTON_LEFT, -1);
        assertEquals(99, readInt(mode, "presetNumber"));

        setInt(mode, "presetNumber", 99);
        change(mode, engine, 45, Controller.BUTTON_RIGHT, -1);
        assertEquals(0, readInt(mode, "presetNumber"));
    }

    // --- reflection helpers ---

    private static void setInt(Object obj, String name, int value) throws Exception {
        field(obj.getClass(), name).setInt(obj, value);
    }

    private static int readInt(Object obj, String name) throws Exception {
        return field(obj.getClass(), name).getInt(obj);
    }

    private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
