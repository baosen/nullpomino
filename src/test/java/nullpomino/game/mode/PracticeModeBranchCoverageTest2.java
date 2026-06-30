package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Covers uncovered lines in {@link PracticeMode}:
 * <ul>
 *   <li>Lines 578-579: cursor-42 map load when prop != null (map file found)</li>
 *   <li>Lines 605-607: start-game path when useMap + empty field + prop != null</li>
 *   <li>Line 1094: onLast countdown playSE when timelimitTimer == 600</li>
 * </ul>
 */
class PracticeModeBranchCoverageTest2 {

    /**
     * A custom EventReceiver that returns a non-null CustomProperties for any
     * loadProperties call, allowing the map-load branches at lines 578-579 and
     * 605-607 to execute.
     */
    private static class MapAvailableReceiver extends EventReceiver {
        @Override
        public CustomProperties loadProperties(String filename) {
            // Return an empty-but-non-null properties so the if(prop != null) branch
            // at lines 577 and 604 evaluates true.
            return new CustomProperties();
        }

        @Override
        public boolean saveProperties(String filename, CustomProperties prop) {
            // Swallow saves to avoid file-system pollution
            return true;
        }

        @Override
        public void saveModeConfig(CustomProperties prop) {
            // Swallow
        }
    }

    // -----------------------------------------------------------------------
    // Lines 578-579: cursor-42 "Map読み込み" when map file exists (prop != null)
    // -----------------------------------------------------------------------

    @Test
    void onSettingCursor42MapLoadWithNonNullProp() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameManager manager = new GameManager(new MapAvailableReceiver());
        manager.mode = mode;
        manager.modeConfig = new CustomProperties();
        manager.init();
        GameEngine engine = manager.engine[0];
        engine.init();
        engine.owner.modeConfig = new CustomProperties();
        mode.playerInit(engine, 0);

        // Set up cursor == 42, menuTime >= 5
        setInt(mode, "menuCursor", 42);
        setInt(mode, "menuTime", 10);
        setInt(mode, "mapNumber", 0);
        engine.owner.replayMode = false;

        // Press A to trigger the map-load branch
        engine.ctrl.clearButtonState();
        engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

        boolean ret = mode.onSetting(engine, 0);
        assertTrue(ret, "onSetting should return true while still in menu");
        // The field was created and a (possibly empty) map loaded — field must exist
        assertNotNull(engine.field, "field should exist after map load");
    }

    // -----------------------------------------------------------------------
    // Lines 605-607: start-game with useMap + empty field + prop != null
    // -----------------------------------------------------------------------

    @Test
    void onSettingStartGameWithUseMapAndFoundMapFile() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameManager manager = new GameManager(new MapAvailableReceiver());
        manager.mode = mode;
        manager.modeConfig = new CustomProperties();
        manager.init();
        GameEngine engine = manager.engine[0];
        engine.init();
        engine.owner.modeConfig = new CustomProperties();
        mode.playerInit(engine, 0);

        // Any non-42/43/44/45 menuCursor triggers the start-game else-branch
        setInt(mode, "menuCursor", 0);
        setInt(mode, "menuTime", 10);
        setInt(mode, "mapNumber", 0);
        // useMap == true, field is null (not yet created) so isEmpty path is taken
        setBoolean(mode, "useMap", true);
        engine.owner.replayMode = false;

        engine.ctrl.clearButtonState();
        engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

        boolean ret = mode.onSetting(engine, 0);
        // Start-game returns false to leave the setting screen
        assertEquals(false, ret, "start game path should return false");
        // The field was created and loaded (lines 605-607 executed)
        assertNotNull(engine.field, "field should be created by lines 605-607");
    }

    // -----------------------------------------------------------------------
    // Line 1094: onLast countdown SE when timelimitTimer == 600
    // -----------------------------------------------------------------------

    @Test
    void onLastCountdownSEWhenTimerAt600() throws Exception {
        PracticeMode mode = new PracticeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));

        // Set up: normal play mode (not ending==2), timelimit > 0, timerActive
        setInt(mode, "timelimit", 3600);
        // onLast decrements before the countdown check, so start at 601 to
        // make the branch see 600 (== 10 * 60, and divisible by 60).
        setInt(mode, "timelimitTimer", 601);
        setInt(mode, "goallv", 0); // not -1, so ending goes to GAMEOVER when time runs out
        engine.ending = 0;
        engine.gameActive = true;
        engine.timerActive = true;

        // onLast should execute the countdown branch (line 1093-1094)
        // and NOT trigger game-over (timelimitTimer > 0)
        mode.onLast(engine, 0);

        // timelimitTimer should have decremented by 1 (from the timelimitTimer-- at 1081)
        assertEquals(600, readInt(mode, "timelimitTimer"),
                "timelimitTimer should decrement each onLast call");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(PracticeMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        return manager.engine[0];
    }

    private static int readInt(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        return f.getInt(obj);
    }

    private static void setInt(Object obj, String name, int value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setInt(obj, value);
    }

    private static void setBoolean(Object obj, String name, boolean value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setBoolean(obj, value);
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
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
