package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Additional branch-coverage tests for {@link PracticeMode} targeting the
 * still-uncovered FALSE / short-circuit sides of compound conditions in the
 * onSetting decide path, the replay-mode skip path, and a renderLast
 * goal-suffix branch.
 *
 * <ul>
 *   <li>L564 false: BUTTON_A pushed but {@code menuTime < 5} (decide skipped)</li>
 *   <li>L583 false: cursor 43 (save map) with {@code engine.field == null}</li>
 *   <li>L602 short-circuit: start game with useMap + a non-empty field (so the
 *       map is NOT reloaded and useMap stays true), and useMap == false</li>
 *   <li>L633: replay mode skip via BUTTON_F before the 120-frame timeout</li>
 *   <li>L956 false: renderLast POINTS goal suffix suppressed when ending != 0</li>
 * </ul>
 */
class PracticeModeBranchCoverageTest3 {

    /** Never writes the git-tracked config/map/** files (or any file). */
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

    private static void pressA(GameEngine engine) {
        engine.ctrl.reset();
        engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
    }

    // -----------------------------------------------------------------------
    // L564 false: BUTTON_A pushed but menuTime < 5 -> decide block skipped
    // -----------------------------------------------------------------------

    @Test
    void onSettingButtonAIgnoredWhenMenuTimeTooShort() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "menuCursor", 0); // would be "start game" if A took effect
        setInt(mode, "menuTime", 0);   // < 5 -> A short-circuits to false
        pressA(engine);

        boolean ret = mode.onSetting(engine, 0);
        // Decide path not taken: still in menu and menuOnly stays true.
        assertTrue(ret, "menuTime<5 must keep us in the setting menu");
        assertTrue(engine.owner.menuOnly, "start game must NOT have been triggered");
        // menuTime advances by 1 each onSetting call.
        assertEquals(1, readInt(mode, "menuTime"));
    }

    // -----------------------------------------------------------------------
    // L583 false: cursor 43 (save map) when engine.field == null
    // -----------------------------------------------------------------------

    @Test
    void onSettingSaveMapWithNullFieldSkipsSave() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.field = null; // force the (engine.field != null) guard to be false
        setInt(mode, "menuCursor", 43);
        setInt(mode, "menuTime", 10);
        setInt(mode, "mapNumber", 0);
        pressA(engine);

        boolean ret = mode.onSetting(engine, 0);
        // The decide path ran (cursor 43) but the save body was skipped.
        assertTrue(ret, "save-map decide path returns true (stays in menu)");
        assertEquals(null, engine.field, "field must remain null - save body skipped");
    }

    // -----------------------------------------------------------------------
    // L602 short-circuit: start game with useMap + a NON-empty field.
    // engine.field.isEmpty() is false -> the whole condition is false, so the
    // map is not reloaded and useMap stays true.
    // -----------------------------------------------------------------------

    @Test
    void onSettingStartGameUseMapWithNonEmptyFieldKeepsUseMap() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.createFieldIfNeeded();
        // Make the field non-empty so isEmpty() returns false.
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));

        setInt(mode, "menuCursor", 0); // start game branch
        setInt(mode, "menuTime", 10);
        setBoolean(mode, "useMap", true);
        pressA(engine);

        boolean ret = mode.onSetting(engine, 0);
        assertFalse(ret, "start game returns false");
        // Non-empty field means the load-map block was skipped; useMap untouched.
        assertTrue(readBoolean(mode, "useMap"), "useMap stays true (no reload attempted)");
        assertFalse(engine.owner.menuOnly, "game started");
    }

    // -----------------------------------------------------------------------
    // L602 first-operand false: start game with useMap == false
    // -----------------------------------------------------------------------

    @Test
    void onSettingStartGameUseMapFalseSkipsMapLoad() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setInt(mode, "menuCursor", 0);
        setInt(mode, "menuTime", 10);
        setBoolean(mode, "useMap", false); // first operand false -> short-circuit
        pressA(engine);

        boolean ret = mode.onSetting(engine, 0);
        assertFalse(ret, "start game returns false");
        assertFalse(readBoolean(mode, "useMap"), "useMap stays false");
        assertFalse(engine.owner.menuOnly, "game started");
    }

    // -----------------------------------------------------------------------
    // L633: replay-mode skip via BUTTON_F before the 120-frame timeout
    // -----------------------------------------------------------------------

    @Test
    void onSettingReplayModeButtonFSkips() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        engine.owner.replayMode = true;
        setInt(mode, "menuTime", 0); // far below 120, so only BUTTON_F can advance

        engine.ctrl.reset();
        engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;

        boolean ret = mode.onSetting(engine, 0);
        assertFalse(ret, "BUTTON_F in replay mode must skip the setting screen");
        assertFalse(engine.owner.menuOnly, "menuOnly cleared on skip");
    }

    // -----------------------------------------------------------------------
    // L956 false: POINTS goal "(-N)" suffix suppressed when ending != 0
    // -----------------------------------------------------------------------

    @Test
    void renderLastPointsGoalSuffixSuppressedDuringEnding() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.stat = GameEngine.Status.MOVE;
        setInt(mode, "leveltype", 2); // POINTS
        setInt(mode, "lastgoal", 3);  // != 0
        setInt(mode, "scgettime", 5); // < 120
        engine.ending = 1;            // ending != 0 -> suffix suppressed (L956 false)
        mode.renderLast(engine, 0);   // must not throw
        assertEquals(1, engine.ending);
    }

    // --- reflection helpers ---

    private static void setInt(Object obj, String name, int value) throws Exception {
        field(obj.getClass(), name).setInt(obj, value);
    }

    private static void setBoolean(Object obj, String name, boolean value) throws Exception {
        field(obj.getClass(), name).setBoolean(obj, value);
    }

    private static int readInt(Object obj, String name) throws Exception {
        return field(obj.getClass(), name).getInt(obj);
    }

    private static boolean readBoolean(Object obj, String name) throws Exception {
        return field(obj.getClass(), name).getBoolean(obj);
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
