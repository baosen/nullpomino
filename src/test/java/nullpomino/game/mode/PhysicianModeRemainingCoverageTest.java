package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in PhysicianMode:
 * line 190 (cancel button in onSetting),
 * lines 316-318 (meter color branches in onLast: yellow, orange, red),
 * lines 387-388 (save ranking in saveReplay when rankingRank != -1).
 */
class PhysicianModeRemainingCoverageTest {

    // ─── Line 190: Cancel button (BUTTON_B) sets engine.quitflag ──────

    @Test
    void onSettingPressCancelSetsQuitFlag() throws Exception {
        PhysicianMode mode = new PhysicianMode();
        GameEngine e = freshEngine(mode);
        mode.playerInit(e, 0);
        e.owner.replayMode = false;

        // Press BUTTON_B to trigger cancel (isPush checks buttonTime == 1)
        e.ctrl = new Controller();
        e.ctrl.buttonTime[Controller.BUTTON_B] = 1;

        mode.onSetting(e, 0);

        assertTrue(e.quitflag, "Pressing BUTTON_B in settings should set quitflag");
    }

    // ─── Lines 316-318: meter color branches in onLast ───────────────

    @Test
    void onLastMeterColorYellow() throws Exception {
        PhysicianMode mode = new PhysicianMode();
        GameEngine e = freshEngine(mode);
        mode.playerInit(e, 0);
        e.createFieldIfNeeded();
        setInt(mode, "hoverBlocks", 100);

        // Yellow: 3 < rest < hoverBlocks/4 (= 25)
        // Put 10 gems (rest = 10)
        addGems(e, 10);

        mode.onLast(e, 0);

        assertEquals(GameEngine.METER_COLOR_YELLOW, e.meterColor,
                "rest=10 with hoverBlocks=100 should be YELLOW");
    }

    @Test
    void onLastMeterColorOrange() throws Exception {
        PhysicianMode mode = new PhysicianMode();
        GameEngine e = freshEngine(mode);
        mode.playerInit(e, 0);
        e.createFieldIfNeeded();
        setInt(mode, "hoverBlocks", 100);

        // Orange: hoverBlocks/4 <= rest < hoverBlocks/2  (25 <= rest < 50)
        // Put 30 gems (rest = 30)
        addGems(e, 30);

        mode.onLast(e, 0);

        assertEquals(GameEngine.METER_COLOR_ORANGE, e.meterColor,
                "rest=30 with hoverBlocks=100 should be ORANGE");
    }

    @Test
    void onLastMeterColorRed() throws Exception {
        PhysicianMode mode = new PhysicianMode();
        GameEngine e = freshEngine(mode);
        mode.playerInit(e, 0);
        e.createFieldIfNeeded();
        setInt(mode, "hoverBlocks", 100);

        // Red: rest >= hoverBlocks/2 (= 50)
        // Put 60 gems (rest = 60)
        addGems(e, 60);

        mode.onLast(e, 0);

        assertEquals(GameEngine.METER_COLOR_RED, e.meterColor,
                "rest=60 with hoverBlocks=100 should be RED");
    }

    // ─── Lines 387-388: saveReplay with rankingRank != -1 ────────────

    @Test
    void saveReplayWithRankingSavesRanking() throws Exception {
        PhysicianMode mode = new PhysicianMode();
        GameEngine e = freshEngine(mode);
        mode.playerInit(e, 0);
        e.ai = null;
        e.owner.replayMode = false;

        // Set a positive score so updateRanking places it
        e.statistics.score = 50000;
        e.statistics.time = 3600;

        CustomProperties prop = new CustomProperties();
        mode.saveReplay(e, 0, prop);

        // After saveReplay, ranking should have been updated
        int rankingRank = readInt(mode, "rankingRank");
        assertTrue(rankingRank >= 0,
                "With score=50000 and all-zero ranking, should be ranked at position 0");
    }

    // ─── Helper: ensure no ranking is set if score is zero ───────────
    // (negative test that lines 387-388 are NOT hit with score=0)

    @Test
    void saveReplayWithoutRankingSkipsSave() throws Exception {
        PhysicianMode mode = new PhysicianMode();
        GameEngine e = freshEngine(mode);
        mode.playerInit(e, 0);
        e.ai = null;
        e.owner.replayMode = false;

        // Score stays at 0, which doesn't beat any ranking entry (all 0, time -1)
        CustomProperties prop = new CustomProperties();
        mode.saveReplay(e, 0, prop);

        int rankingRank = readInt(mode, "rankingRank");
        assertEquals(-1, rankingRank,
                "With score=0, should not be ranked");
    }

    // ─── helpers ─────────────────────────────────────────────────────

    private static GameEngine freshEngine(PhysicianMode mode) {
        GameManager gm = new GameManager(new EventReceiver());
        gm.mode = mode;
        gm.init();
        gm.engine[0].init();
        return gm.engine[0];
    }

    /** Adds {@code count} gem blocks spread across the field bottom row. */
    private static void addGems(GameEngine e, int count) {
        int x = 0, y = 0;
        for (int i = 0; i < count; i++) {
            e.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GEM_RED));
            x++;
            if (x >= e.field.getWidth()) {
                x = 0;
                y++;
            }
        }
    }

    private static int readInt(Object instance, String name) throws Exception {
        Field f = findField(instance.getClass(), name);
        f.setAccessible(true);
        return f.getInt(instance);
    }

    private static void setInt(Object instance, String name, int value) throws Exception {
        Field f = findField(instance.getClass(), name);
        f.setAccessible(true);
        f.setInt(instance, value);
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
